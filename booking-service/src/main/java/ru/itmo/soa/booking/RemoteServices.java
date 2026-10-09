package ru.itmo.soa.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.itmo.soa.booking.client.person.PersonsApi;
import ru.itmo.soa.booking.client.ticket.TicketsApi;
import ru.itmo.soa.booking.client.ticket.model.Ticket;
import ru.itmo.soa.booking.client.ticket.model.TicketCreate;
import ru.itmo.soa.common.ApiException;

import java.util.function.Supplier;

/**
 * Ticket Service and Person Service through the clients generated from their specifications.
 * TLS trust comes from the JVM truststore configured for the Tomcat instance.
 */
@Component
public class RemoteServices {

    private final TicketsApi tickets;
    private final PersonsApi persons;

    public RemoteServices(@Value("${ticket-service.url}") String ticketUrl,
                          @Value("${person-service.url}") String personUrl) {
        tickets = new TicketsApi(new ru.itmo.soa.booking.client.ticket.invoker.ApiClient().setBasePath(ticketUrl));
        persons = new PersonsApi(new ru.itmo.soa.booking.client.person.invoker.ApiClient().setBasePath(personUrl));
    }

    public Ticket getTicket(long id) {
        return call("Ticket Service", "Билет с id=" + id + " не найден", () -> tickets.getTicket(id));
    }

    public Ticket createTicket(TicketCreate ticket) {
        return call("Ticket Service", null, () -> tickets.createTicket(ticket));
    }

    public void checkPerson(long id) {
        call("Person Service", "Человек с id=" + id + " не найден", () -> persons.getPerson(id));
    }

    private static <T> T call(String service, String notFound, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            if (notFound != null && e.getStatusCode().value() == 404) {
                throw ApiException.notFound(notFound);
            }
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    service + " вернул ошибку " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, service + " недоступен");
        }
    }
}
