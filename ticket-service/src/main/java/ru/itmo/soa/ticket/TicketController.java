package ru.itmo.soa.ticket;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.soa.common.ApiException;
import ru.itmo.soa.common.PageQuery;
import ru.itmo.soa.ticket.api.TicketOperationsApi;
import ru.itmo.soa.ticket.api.TicketsApi;
import ru.itmo.soa.ticket.api.model.Coordinates;
import ru.itmo.soa.ticket.api.model.Ticket;
import ru.itmo.soa.ticket.api.model.TicketCreate;
import ru.itmo.soa.ticket.api.model.TicketPage;
import ru.itmo.soa.ticket.api.model.TicketPatch;
import ru.itmo.soa.ticket.api.model.TicketType;
import ru.itmo.soa.ticket.api.model.Venue;
import ru.itmo.soa.ticket.api.model.VenueInput;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@RestController
@Transactional
public class TicketController implements TicketsApi, TicketOperationsApi {

    private static final Set<String> FIELDS = Set.of("id", "name", "coordinates.x", "coordinates.y", "creationDate",
            "price", "comment", "type", "venue.id", "venue.name", "venue.capacity", "venue.type");

    @PersistenceContext
    private EntityManager em;

    @Override
    public ResponseEntity<Ticket> createTicket(TicketCreate body) {
        TicketEntity e = new TicketEntity();
        e.name = body.getName();
        e.coordinates = coordinates(body.getCoordinates());
        e.creationDate = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
        e.price = body.getPrice().orElse(null);
        e.comment = body.getComment();
        e.type = body.getType().orElse(null);
        e.venue = venue(body.getVenue().orElse(null));
        em.persist(e);
        em.flush();
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(e.id).toUri()).body(toDto(e));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<TicketPage> listTickets(Integer page, Integer size, List<String> sort, List<String> filter) {
        PageQuery.Result<TicketEntity> result = PageQuery.find(em, TicketEntity.class, FIELDS, Set.of("venue"),
                page, size, sort, filter);
        return ResponseEntity.ok(new TicketPage(result.items().stream().map(TicketController::toDto).toList(),
                page, size, result.totalElements(), result.totalPages()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Ticket> getTicket(Long ticketId) {
        return ResponseEntity.ok(toDto(find(ticketId)));
    }

    @Override
    public ResponseEntity<Ticket> updateTicket(Long ticketId, TicketPatch patch) {
        if (patch.getName() == null && patch.getCoordinates() == null && patch.getComment() == null
                && !patch.getPrice().isPresent() && !patch.getType().isPresent() && !patch.getVenue().isPresent()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "JSON запроса должен содержать хотя бы одно поле");
        }
        TicketEntity e = find(ticketId);
        if (patch.getName() != null) e.name = patch.getName();
        if (patch.getCoordinates() != null) e.coordinates = coordinates(patch.getCoordinates());
        if (patch.getComment() != null) e.comment = patch.getComment();
        if (patch.getPrice().isPresent()) e.price = patch.getPrice().get();
        if (patch.getType().isPresent()) e.type = patch.getType().get();
        if (patch.getVenue().isPresent()) e.venue = venue(patch.getVenue().get());
        em.flush();
        return ResponseEntity.ok(toDto(e));
    }

    @Override
    public ResponseEntity<Void> deleteTicket(Long ticketId) {
        em.remove(find(ticketId));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteAnyTicketByType(TicketType type) {
        TicketEntity e = em.createQuery("select t from TicketEntity t where t.type = :type", TicketEntity.class)
                .setParameter("type", type)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Билет с type=" + type + " не найден"));
        em.remove(e);
        return ResponseEntity.noContent().build();
    }

    private TicketEntity find(Long id) {
        TicketEntity e = em.find(TicketEntity.class, id);
        if (e == null) {
            throw ApiException.notFound("Билет с id=" + id + " не найден");
        }
        return e;
    }

    private static TicketEntity.Coordinates coordinates(Coordinates dto) {
        TicketEntity.Coordinates c = new TicketEntity.Coordinates();
        c.x = dto.getX();
        c.y = dto.getY();
        return c;
    }

    private static TicketEntity.Venue venue(VenueInput dto) {
        if (dto == null) {
            return null;
        }
        TicketEntity.Venue v = new TicketEntity.Venue();
        v.name = dto.getName();
        v.capacity = dto.getCapacity();
        v.type = dto.getType();
        return v;
    }

    private static Ticket toDto(TicketEntity e) {
        Ticket t = new Ticket(e.id, e.name, new Coordinates(e.coordinates.x, e.coordinates.y),
                e.creationDate.withOffsetSameInstant(ZoneOffset.UTC), e.comment);
        t.price(e.price).type(e.type);
        t.venue(e.venue == null ? null : new Venue(e.venue.id, e.venue.name, e.venue.capacity, e.venue.type));
        return t;
    }
}
