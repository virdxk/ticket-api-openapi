package ru.itmo.soa.booking;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.soa.booking.api.BookingApi;
import ru.itmo.soa.booking.api.BookingsApi;
import ru.itmo.soa.booking.api.model.Booking;
import ru.itmo.soa.booking.api.model.BookingCreate;
import ru.itmo.soa.booking.api.model.BookingPage;
import ru.itmo.soa.booking.api.model.BookingPatch;
import ru.itmo.soa.booking.api.model.CancellationResult;
import ru.itmo.soa.booking.client.ticket.model.Coordinates;
import ru.itmo.soa.booking.client.ticket.model.Ticket;
import ru.itmo.soa.booking.client.ticket.model.TicketCreate;
import ru.itmo.soa.booking.client.ticket.model.TicketType;
import ru.itmo.soa.booking.client.ticket.model.VenueInput;
import ru.itmo.soa.common.ApiException;
import ru.itmo.soa.common.PageQuery;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@RestController
@Transactional
public class BookingController implements BookingsApi, BookingApi {

    private static final Set<String> FIELDS = Set.of("id", "ticketId", "personId", "createdAt");

    @PersistenceContext
    private EntityManager em;

    private final RemoteServices remote;

    public BookingController(RemoteServices remote) {
        this.remote = remote;
    }

    @Override
    public ResponseEntity<Booking> createBooking(BookingCreate body) {
        remote.getTicket(body.getTicketId());
        remote.checkPerson(body.getPersonId());
        return created(save(body.getTicketId(), body.getPersonId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<BookingPage> listBookings(Integer page, Integer size, List<String> sort, List<String> filter) {
        PageQuery.Result<BookingEntity> result = PageQuery.find(em, BookingEntity.class, FIELDS, Set.of(),
                page, size, sort, filter);
        return ResponseEntity.ok(new BookingPage(result.items().stream().map(BookingController::toDto).toList(),
                page, size, result.totalElements(), result.totalPages()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Booking> getBooking(Long bookingId) {
        return ResponseEntity.ok(toDto(find(bookingId)));
    }

    @Override
    public ResponseEntity<Booking> updateBooking(Long bookingId, BookingPatch patch) {
        if (patch.getTicketId() == null && patch.getPersonId() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "JSON запроса должен содержать хотя бы одно поле");
        }
        BookingEntity e = find(bookingId);
        if (patch.getTicketId() != null) {
            remote.getTicket(patch.getTicketId());
            e.ticketId = patch.getTicketId();
        }
        if (patch.getPersonId() != null) {
            remote.checkPerson(patch.getPersonId());
            e.personId = patch.getPersonId();
        }
        return ResponseEntity.ok(toDto(e));
    }

    @Override
    public ResponseEntity<Void> deleteBooking(Long bookingId) {
        em.remove(find(bookingId));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Booking> sellVipTicket(Long ticketId, Long personId) {
        remote.checkPerson(personId);
        Ticket source = remote.getTicket(ticketId);
        if (source.getPrice() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "У билета с id=" + ticketId + " не задана цена");
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
        Ticket vip = remote.createTicket(copy);
        return created(save(vip.getId(), personId));
    }

    @Override
    public ResponseEntity<CancellationResult> cancelPersonBookings(Long personId) {
        int cancelled = em.createQuery("delete from BookingEntity b where b.personId = :personId")
                .setParameter("personId", personId)
                .executeUpdate();
        return ResponseEntity.ok(new CancellationResult(personId, cancelled));
    }

    private BookingEntity save(Long ticketId, Long personId) {
        BookingEntity e = new BookingEntity();
        e.ticketId = ticketId;
        e.personId = personId;
        e.createdAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
        em.persist(e);
        em.flush();
        return e;
    }

    private ResponseEntity<Booking> created(BookingEntity e) {
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/booking/bookings/{id}").buildAndExpand(e.id).toUri()).body(toDto(e));
    }

    private BookingEntity find(Long id) {
        BookingEntity e = em.find(BookingEntity.class, id);
        if (e == null) {
            throw ApiException.notFound("Бронирование с id=" + id + " не найдено");
        }
        return e;
    }

    private static Booking toDto(BookingEntity e) {
        return new Booking(e.id, e.ticketId, e.personId, e.createdAt.withOffsetSameInstant(ZoneOffset.UTC));
    }
}
