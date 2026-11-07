package ru.itmo.soa.booking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.soa.booking.api.BookingApi;
import ru.itmo.soa.booking.api.BookingsApi;
import ru.itmo.soa.booking.api.model.Booking;
import ru.itmo.soa.booking.api.model.BookingCreate;
import ru.itmo.soa.booking.api.model.BookingPage;
import ru.itmo.soa.booking.api.model.BookingPatch;
import ru.itmo.soa.booking.api.model.CancellationResult;
import ru.itmo.soa.booking.service.BookingService;
import ru.itmo.soa.booking.utils.Constants;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookingController implements BookingsApi, BookingApi {

    private final BookingService service;

    @Override
    public ResponseEntity<Booking> createBooking(BookingCreate body) {
        return created(service.create(body));
    }

    @Override
    public ResponseEntity<BookingPage> listBookings(Integer page, Integer size, List<String> sort, List<String> filter) {
        return ResponseEntity.ok(service.list(page, size, sort, filter));
    }

    @Override
    public ResponseEntity<Booking> getBooking(Long bookingId) {
        return ResponseEntity.ok(service.get(bookingId));
    }

    @Override
    public ResponseEntity<Booking> updateBooking(Long bookingId, BookingPatch patch) {
        return ResponseEntity.ok(service.update(bookingId, patch));
    }

    @Override
    public ResponseEntity<Void> deleteBooking(Long bookingId) {
        service.delete(bookingId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Booking> sellVipTicket(Long ticketId, Long personId) {
        return created(service.sellVip(ticketId, personId));
    }

    @Override
    public ResponseEntity<CancellationResult> cancelPersonBookings(Long personId) {
        int cancelledBookings = service.cancel(personId);
        return ResponseEntity.ok(new CancellationResult(personId, cancelledBookings));
    }

    private static ResponseEntity<Booking> created(Booking booking) {
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(Constants.BOOKING_LOCATION_PATH).buildAndExpand(booking.getId()).toUri()).body(booking);
    }
}
