package ru.itmo.soa.ticket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.soa.common.error.ApiException;
import ru.itmo.soa.common.repository.PageQuery;
import ru.itmo.soa.ticket.api.model.Coordinates;
import ru.itmo.soa.ticket.api.model.Ticket;
import ru.itmo.soa.ticket.api.model.TicketCreate;
import ru.itmo.soa.ticket.api.model.TicketPage;
import ru.itmo.soa.ticket.api.model.TicketPatch;
import ru.itmo.soa.ticket.api.model.TicketType;
import ru.itmo.soa.ticket.api.model.Venue;
import ru.itmo.soa.ticket.api.model.VenueInput;
import ru.itmo.soa.ticket.entity.TicketEntity;
import ru.itmo.soa.ticket.repository.TicketRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TicketService {

    private static final Set<String> FIELDS = Set.of("id", "name", "coordinates.x", "coordinates.y", "creationDate",
            "price", "comment", "type", "venue.id", "venue.name", "venue.capacity", "venue.type");

    @Autowired
    private TicketRepository repository;

    @Autowired
    private PageQuery pageQuery;

    public Ticket create(TicketCreate body) {
        TicketEntity e = new TicketEntity();
        e.name = body.getName();
        e.coordinates = coordinates(body.getCoordinates());
        e.creationDate = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
        e.price = body.getPrice().orElse(null);
        e.comment = body.getComment();
        e.type = body.getType().orElse(null);
        e.venue = venue(body.getVenue().orElse(null));
        return toDto(repository.saveAndFlush(e));
    }

    @Transactional(readOnly = true)
    public TicketPage list(Integer page, Integer size, List<String> sort, List<String> filter) {
        PageQuery.Result<TicketEntity> result = pageQuery.find(TicketEntity.class, FIELDS, Set.of("venue"),
                page, size, sort, filter);
        return new TicketPage(result.items().stream().map(TicketService::toDto).toList(),
                page, size, result.totalElements(), result.totalPages());
    }

    @Transactional(readOnly = true)
    public Ticket get(Long id) {
        return toDto(find(id));
    }

    public Ticket update(Long id, TicketPatch patch) {
        if (patch.getName() == null && patch.getCoordinates() == null && patch.getComment() == null
                && !patch.getPrice().isPresent() && !patch.getType().isPresent() && !patch.getVenue().isPresent()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "error.body.empty");
        }
        TicketEntity e = find(id);
        if (patch.getName() != null) e.name = patch.getName();
        if (patch.getCoordinates() != null) e.coordinates = coordinates(patch.getCoordinates());
        if (patch.getComment() != null) e.comment = patch.getComment();
        if (patch.getPrice().isPresent()) e.price = patch.getPrice().get();
        if (patch.getType().isPresent()) e.type = patch.getType().get();
        if (patch.getVenue().isPresent()) e.venue = venue(patch.getVenue().get());
        return toDto(repository.saveAndFlush(e));
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    public void deleteAnyByType(TicketType type) {
        repository.delete(repository.findFirstByType(type)
                .orElseThrow(() -> ApiException.notFound("ticket.notFoundByType", type)));
    }

    private TicketEntity find(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("ticket.notFound", id));
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
