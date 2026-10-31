package ru.itmo.soa.booking.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.itmo.soa.booking.client.person.PersonsApi;
import ru.itmo.soa.booking.client.ticket.TicketsApi;
import ru.itmo.soa.booking.client.ticket.model.Ticket;
import ru.itmo.soa.booking.client.ticket.model.TicketCreate;
import ru.itmo.soa.common.error.ApiException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

@Component
public class RemoteServices {

    private static final Logger log = LoggerFactory.getLogger(RemoteServices.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final TicketsApi tickets;
    private final PersonsApi persons;

    public RemoteServices(@Value("${ticket-service.url}") String ticketUrl,
                          @Value("${person-service.url}") String personUrl, SslBundles ssl) {
        JdkClientHttpRequestFactory https = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .sslContext(ssl.getBundle("services").createSslContext())
                .connectTimeout(CONNECT_TIMEOUT)
                .build());
        https.setReadTimeout(READ_TIMEOUT);
        tickets = new TicketsApi(new ru.itmo.soa.booking.client.ticket.invoker.ApiClient(
                ru.itmo.soa.booking.client.ticket.invoker.ApiClient.buildRestClientBuilder()
                        .requestFactory(https).build()).setBasePath(ticketUrl));
        persons = new PersonsApi(new ru.itmo.soa.booking.client.person.invoker.ApiClient(
                ru.itmo.soa.booking.client.person.invoker.ApiClient.buildRestClientBuilder()
                        .requestFactory(https).build()).setBasePath(personUrl));
    }

    public Ticket getTicket(long id) {
        return call("Ticket Service", "ticket.notFound", id, () -> tickets.getTicket(id));
    }

    public Ticket createTicket(TicketCreate ticket) {
        return call("Ticket Service", null, null, () -> tickets.createTicket(ticket));
    }

    public void checkPerson(long id) {
        call("Person Service", "person.notFound", id, () -> persons.getPerson(id));
    }

    private static <T> T call(String service, String notFound, Object id, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            if (notFound != null && e.getStatusCode().value() == 404) {
                throw ApiException.notFound(notFound, id);
            }
            log.warn("{} returned HTTP {}: {}", service, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, e, "service.failed", service, e.getStatusCode().value());
        } catch (RestClientException e) {
            log.warn("{} is unavailable", service, e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, e, "service.unavailable", service);
        }
    }
}
