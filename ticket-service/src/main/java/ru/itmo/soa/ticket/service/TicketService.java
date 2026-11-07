package ru.itmo.soa.ticket.service;

import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.soa.common.error.ApiException;
import ru.itmo.soa.common.query.QueryParameters;
import ru.itmo.soa.ticket.api.model.Coordinates;
import ru.itmo.soa.ticket.api.model.Ticket;
import ru.itmo.soa.ticket.api.model.TicketCreate;
import ru.itmo.soa.ticket.api.model.TicketPage;
import ru.itmo.soa.ticket.api.model.TicketPatch;
import ru.itmo.soa.ticket.api.model.TicketType;
import ru.itmo.soa.ticket.api.model.Venue;
import ru.itmo.soa.ticket.api.model.VenueInput;
import ru.itmo.soa.ticket.entity.TicketEntity;
import ru.itmo.soa.ticket.entity.VenueEntity;
import ru.itmo.soa.ticket.repository.TicketRepository;
import ru.itmo.soa.ticket.utils.Constants;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static ru.itmo.soa.common.utils.Constants.ERROR_BODY_EMPTY;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository repository;
    private final EntityManager entityManager;

    @Transactional
    public Ticket create(TicketCreate body) {
        TicketEntity e = new TicketEntity();
        e.name = body.getName();
        e.coordinates = coordinates(body.getCoordinates());
        e.creationDate = OffsetDateTime.now(ZoneOffset.UTC);
        e.price = body.getPrice().orElse(null);
        e.comment = body.getComment();
        e.type = body.getType().orElse(null);
        e.venue = venue(body.getVenue().orElse(null));
        return toDto(repository.save(e));
    }

    @Transactional(readOnly = true)
    public TicketPage list(Integer page, Integer size, List<String> sort, List<String> filter) {
        Map<String, Object> parameters = new HashMap<>();
        String where = QueryParameters.whereClause(
                TicketEntity.class,
                filter,
                TicketService::queryField,
                parameters
        );
        String orderBy = QueryParameters.orderBy(TicketEntity.class, sort, TicketService::queryField);

        TypedQuery<Long> countQuery = entityManager.createQuery(
                """
                select count(e)
                from TicketEntity e
                left join e.venue venue
                """ + where,
                Long.class
        );
        parameters.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();
        long offset = (long) (page - 1) * size;
        List<Ticket> items = List.of();
        if (offset < total) {
            TypedQuery<TicketEntity> query = entityManager.createQuery(
                    """
                    select e
                    from TicketEntity e
                    left join fetch e.venue venue
                    """ + where + " order by " + orderBy,
                    TicketEntity.class
            );
            parameters.forEach(query::setParameter);
            items = query.setFirstResult((int) offset)
                    .setMaxResults(size)
                    .getResultList()
                    .stream()
                    .map(TicketService::toDto)
                    .toList();
        }
        return new TicketPage(items, page, size, total, (int) ((total + size - 1) / size));
    }

    @Transactional(readOnly = true)
    public Ticket get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public Ticket update(Long id, TicketPatch patch) {
        if (patch.getName() == null && patch.getCoordinates() == null && patch.getComment() == null
                && !patch.getPrice().isPresent() && !patch.getType().isPresent() && !patch.getVenue().isPresent()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ERROR_BODY_EMPTY);
        }
        TicketEntity e = find(id);
        if (patch.getName() != null) e.name = patch.getName();
        if (patch.getCoordinates() != null) e.coordinates = coordinates(patch.getCoordinates());
        if (patch.getComment() != null) e.comment = patch.getComment();
        if (patch.getPrice().isPresent()) e.price = patch.getPrice().get();
        if (patch.getType().isPresent()) e.type = patch.getType().get();
        if (patch.getVenue().isPresent()) e.venue = venue(patch.getVenue().get());
        return toDto(e);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    @Transactional
    public void deleteAnyByType(TicketType type) {
        repository.delete(repository.findFirstByType(type)
                .orElseThrow(() -> ApiException.notFound(Constants.TICKET_NOT_FOUND_BY_TYPE, type)));
    }

    private static String queryField(String field) {
        return field.startsWith("venue.") ? field : "e." + field;
    }

    private TicketEntity find(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound(Constants.TICKET_NOT_FOUND, id));
    }

    private static TicketEntity.Coordinates coordinates(Coordinates dto) {
        TicketEntity.Coordinates c = new TicketEntity.Coordinates();
        c.x = dto.getX();
        c.y = dto.getY();
        return c;
    }

    private static VenueEntity venue(VenueInput dto) {
        if (dto == null) {
            return null;
        }
        VenueEntity v = new VenueEntity();
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
