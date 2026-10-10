package ru.itmo.soa.booking;

import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.List;

@RestController
public class BookingController implements BookingsApi, BookingApi {

    @Autowired
    private BookingService service;

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
        return ResponseEntity.ok(service.cancel(personId));
    }

    private static ResponseEntity<Booking> created(Booking booking) {
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/booking/bookings/{id}").buildAndExpand(booking.getId()).toUri()).body(booking);
    }
}
