package ru.itmo.soa.booking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Set;

@Service
@Transactional
public class BookingService {

    private static final Set<String> FIELDS = Set.of("id", "ticketId", "personId", "createdAt");

    @Autowired
    private BookingRepository repository;

    @Autowired
    private PageQuery pageQuery;

    @Autowired
    private RemoteServices remote;

    public Booking create(BookingCreate body) {
        remote.getTicket(body.getTicketId());
        remote.checkPerson(body.getPersonId());
        return save(body.getTicketId(), body.getPersonId());
    }

    @Transactional(readOnly = true)
    public BookingPage list(Integer page, Integer size, List<String> sort, List<String> filter) {
        PageQuery.Result<BookingEntity> result = pageQuery.find(BookingEntity.class, FIELDS, Set.of(),
                page, size, sort, filter);
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
        BookingEntity e = find(id);
        if (patch.getTicketId() != null) {
            remote.getTicket(patch.getTicketId());
            e.ticketId = patch.getTicketId();
        }
        if (patch.getPersonId() != null) {
            remote.checkPerson(patch.getPersonId());
            e.personId = patch.getPersonId();
        }
        return toDto(repository.saveAndFlush(e));
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    public Booking sellVip(Long ticketId, Long personId) {
        remote.checkPerson(personId);
        Ticket source = remote.getTicket(ticketId);
        if (source.getPrice() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "ticket.noPrice", ticketId);
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
        return save(remote.createTicket(copy).getId(), personId);
    }

    public CancellationResult cancel(Long personId) {
        return new CancellationResult(personId, repository.deleteByPersonId(personId));
    }

    private Booking save(Long ticketId, Long personId) {
        BookingEntity e = new BookingEntity();
        e.ticketId = ticketId;
        e.personId = personId;
        e.createdAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
        return toDto(repository.saveAndFlush(e));
    }

    private BookingEntity find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("booking.notFound", id));
    }

    private static Booking toDto(BookingEntity e) {
        return new Booking(e.id, e.ticketId, e.personId, e.createdAt.withOffsetSameInstant(ZoneOffset.UTC));
    }
}
