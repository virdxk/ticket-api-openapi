package ru.itmo.soa.booking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itmo.soa.booking.api.model.Booking;
import ru.itmo.soa.booking.api.model.BookingCreate;
import ru.itmo.soa.booking.api.model.BookingPage;
import ru.itmo.soa.booking.api.model.BookingPatch;
import ru.itmo.soa.booking.api.model.CancellationResult;
import ru.itmo.soa.booking.client.RemoteServices;
import ru.itmo.soa.booking.client.ticket.model.Coordinates;
import ru.itmo.soa.booking.client.ticket.model.Ticket;
import ru.itmo.soa.booking.client.ticket.model.TicketCreate;
import ru.itmo.soa.booking.client.ticket.model.TicketType;
import ru.itmo.soa.booking.client.ticket.model.VenueInput;
import ru.itmo.soa.booking.entity.BookingEntity;
import ru.itmo.soa.booking.repository.BookingRepository;
import ru.itmo.soa.common.error.ApiException;
import ru.itmo.soa.common.repository.PageQuery;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository repository;
    private final PageQuery pageQuery;
    private final RemoteServices remote;
    private final TransactionTemplate tx;

    public BookingService(BookingRepository repository, PageQuery pageQuery, RemoteServices remote,
                          PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.pageQuery = pageQuery;
        this.remote = remote;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public Booking create(BookingCreate body) {
        remote.getTicket(body.getTicketId());
        remote.checkPerson(body.getPersonId());
        return tx.execute(status -> save(body.getTicketId(), body.getPersonId()));
    }

    @Transactional(readOnly = true)
    public BookingPage list(Integer page, Integer size, List<String> sort, List<String> filter) {
        PageQuery.Result<BookingEntity> result = pageQuery.find(BookingEntity.class, page, size, sort, filter);
        return new BookingPage(result.items().stream().map(BookingService::toDto).toList(),
                page, size, result.totalElements(), result.totalPages());
    }

    @Transactional(readOnly = true)
    public Booking get(Long id) {
        return toDto(find(id));
    }

    public Booking update(Long id, BookingPatch patch) {
        if (patch.getTicketId() == null && patch.getPersonId() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "error.body.empty");
        }
        if (!repository.existsById(id)) {
            throw ApiException.notFound("booking.notFound", id);
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
            throw new ApiException(HttpStatus.CONFLICT, "ticket.noPrice", ticketId);
        }
        if (!Float.isFinite(source.getPrice() * 2)) {
            throw new ApiException(HttpStatus.CONFLICT, "ticket.priceOverflow", ticketId);
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
                log.error("VIP sale compensation failed: copied ticket {} (source ticket {}, person {}) was not deleted",
                        copyId, ticketId, personId, compensation);
            }
            throw e;
        }
    }

    @Transactional
    public CancellationResult cancel(Long personId) {
        return new CancellationResult(personId, repository.deleteByPersonId(personId));
    }

    private Booking save(Long ticketId, Long personId) {
        BookingEntity e = new BookingEntity();
        e.ticketId = ticketId;
        e.personId = personId;
        e.createdAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
        return toDto(repository.save(e));
    }

    private BookingEntity find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("booking.notFound", id));
    }

    private static Booking toDto(BookingEntity e) {
        return new Booking(e.id, e.ticketId, e.personId, e.createdAt.withOffsetSameInstant(ZoneOffset.UTC));
    }
}
