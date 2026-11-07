package ru.itmo.soa.booking.service;

import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itmo.soa.booking.api.model.Booking;
import ru.itmo.soa.booking.api.model.BookingCreate;
import ru.itmo.soa.booking.api.model.BookingPage;
import ru.itmo.soa.booking.api.model.BookingPatch;
import ru.itmo.soa.booking.client.RemoteServices;
import ru.itmo.soa.booking.client.ticket.model.Coordinates;
import ru.itmo.soa.booking.client.ticket.model.Ticket;
import ru.itmo.soa.booking.client.ticket.model.TicketCreate;
import ru.itmo.soa.booking.client.ticket.model.TicketType;
import ru.itmo.soa.booking.client.ticket.model.VenueInput;
import ru.itmo.soa.booking.entity.BookingEntity;
import ru.itmo.soa.booking.repository.BookingRepository;
import ru.itmo.soa.booking.utils.Constants;
import ru.itmo.soa.common.error.ApiException;
import ru.itmo.soa.common.query.QueryParameters;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static ru.itmo.soa.common.utils.Constants.ERROR_BODY_EMPTY;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository repository;
    private final EntityManager entityManager;
    private final QueryParameters queryParameters = new QueryParameters();
    private final RemoteServices remote;
    private final TransactionTemplate tx;

    public Booking create(BookingCreate body) {
        remote.getTicket(body.getTicketId());
        remote.checkPerson(body.getPersonId());
        return tx.execute(status -> save(body.getTicketId(), body.getPersonId()));
    }

    @Transactional(readOnly = true)
    public BookingPage list(Integer page, Integer size, List<String> sort, List<String> filter) {
        Map<String, Object> parameters = new HashMap<>();
        String where = queryParameters.whereClause(
                BookingEntity.class,
                filter,
                field -> "e." + field,
                parameters
        );
        String orderBy = queryParameters.orderBy(BookingEntity.class, sort, field -> "e." + field);

        TypedQuery<Long> countQuery = entityManager.createQuery(
                """
                select count(e)
                from BookingEntity e
                """ + where,
                Long.class
        );
        parameters.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();
        long offset = (long) (page - 1) * size;
        List<Booking> items = List.of();
        if (offset < total) {
            TypedQuery<BookingEntity> query = entityManager.createQuery(
                    """
                    select e
                    from BookingEntity e
                    """ + where + " order by " + orderBy,
                    BookingEntity.class
            );
            parameters.forEach(query::setParameter);
            items = query.setFirstResult((int) offset)
                    .setMaxResults(size)
                    .getResultList()
                    .stream()
                    .map(this::toDto)
                    .toList();
        }
        return new BookingPage(items, page, size, total, (int) ((total + size - 1) / size));
    }

    @Transactional(readOnly = true)
    public Booking get(Long id) {
        return toDto(find(id));
    }

    public Booking update(Long id, BookingPatch patch) {
        if (patch.getTicketId() == null && patch.getPersonId() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ERROR_BODY_EMPTY);
        }
        if (!repository.existsById(id)) {
            throw ApiException.notFound(Constants.BOOKING_NOT_FOUND, id);
        }
        if (patch.getTicketId() != null) {
            remote.getTicket(patch.getTicketId());
        }
        if (patch.getPersonId() != null) {
            remote.checkPerson(patch.getPersonId());
        }
        return tx.execute(status -> {
            BookingEntity e = find(id);
            if (patch.getTicketId() != null) {
                e.ticketId = patch.getTicketId();
            }
            if (patch.getPersonId() != null) {
                e.personId = patch.getPersonId();
            }
            return toDto(e);
        });
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    public Booking sellVip(Long ticketId, Long personId) {
        remote.checkPerson(personId);
        Ticket source = remote.getTicket(ticketId);
        if (source.getPrice() == null) {
            throw new ApiException(HttpStatus.CONFLICT, Constants.TICKET_NO_PRICE, ticketId);
        }
        if (!Float.isFinite(source.getPrice() * 2)) {
            throw new ApiException(HttpStatus.CONFLICT, Constants.TICKET_PRICE_OVERFLOW, ticketId);
        }
        TicketCreate copy = new TicketCreate()
                .name(source.getName())
                .coordinates(new Coordinates().x(source.getCoordinates().getX()).y(source.getCoordinates().getY()))
                .comment(source.getComment())
                .price(source.getPrice() * 2)
                .type(TicketType.VIP)
                .venue(source.getVenue() == null ? null : new VenueInput()
                        .name(source.getVenue().getName())
                        .capacity(source.getVenue().getCapacity())
                        .type(source.getVenue().getType()));
        Long copyId = remote.createTicket(copy).getId();
        try {
            return tx.execute(status -> save(copyId, personId));
        } catch (RuntimeException e) {
            try {
                remote.deleteTicket(copyId);
            } catch (RuntimeException compensation) {
                e.addSuppressed(compensation);
            }
            throw e;
        }
    }

    @Transactional
    public int cancel(Long personId) {
        return repository.deleteByPersonId(personId);
    }

    private Booking save(Long ticketId, Long personId) {
        BookingEntity e = new BookingEntity();
        e.ticketId = ticketId;
        e.personId = personId;
        e.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        return toDto(repository.save(e));
    }

    private BookingEntity find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound(Constants.BOOKING_NOT_FOUND, id));
    }

    private Booking toDto(BookingEntity e) {
        return new Booking(e.id, e.ticketId, e.personId, e.createdAt.withOffsetSameInstant(ZoneOffset.UTC));
    }
}
