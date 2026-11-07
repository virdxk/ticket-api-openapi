package ru.itmo.soa.booking.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.itmo.soa.booking.client.person.PersonsApi;
import ru.itmo.soa.booking.client.ticket.TicketsApi;
import ru.itmo.soa.booking.client.ticket.model.Ticket;
import ru.itmo.soa.booking.client.ticket.model.TicketCreate;
import ru.itmo.soa.booking.utils.Constants;
import ru.itmo.soa.common.error.ApiException;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RemoteServices {

    private final TicketsApi tickets;
    private final PersonsApi persons;

    public Ticket getTicket(long id) {
        return call(RemoteService.TICKET, id, () -> tickets.getTicket(id));
    }

    public Ticket createTicket(TicketCreate ticket) {
        return call(RemoteService.TICKET, null, () -> tickets.createTicket(ticket));
    }

    public void deleteTicket(long id) {
        call(RemoteService.TICKET, null, () -> {
            tickets.deleteTicket(id);
            return null;
        });
    }

    public void checkPerson(long id) {
        call(RemoteService.PERSON, id, () -> persons.getPerson(id));
    }

    private <T> T call(RemoteService service, Long notFoundId, Supplier<T> request) {
        String name = service.getDisplayName();
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            if (notFoundId != null && e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw ApiException.notFound(service.getNotFoundCode(), notFoundId);
            }
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    e,
                    Constants.SERVICE_FAILED,
                    name,
                    e.getStatusCode().value()
            );
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, e, Constants.SERVICE_UNAVAILABLE, name);
        }
    }
}
