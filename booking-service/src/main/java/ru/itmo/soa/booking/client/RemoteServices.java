package ru.itmo.soa.booking.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.itmo.soa.booking.client.person.PersonsApi;
import ru.itmo.soa.booking.client.ticket.TicketsApi;
import ru.itmo.soa.booking.client.ticket.invoker.ApiClient;
import ru.itmo.soa.booking.client.ticket.model.Ticket;
import ru.itmo.soa.booking.client.ticket.model.TicketCreate;
import ru.itmo.soa.booking.utils.Constants;
import ru.itmo.soa.common.error.ApiException;
import java.util.function.Supplier;

@Component
public class RemoteServices {

    private final TicketsApi tickets;
    private final PersonsApi persons;

    public RemoteServices(
            @Value(Constants.TICKET_SERVICE_URL) String ticketUrl,
            @Value(Constants.PERSON_SERVICE_URL) String personUrl,
            SslBundles ssl
    ) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withSslBundle(ssl.getBundle(Constants.SERVICES_SSL_BUNDLE))
                .withConnectTimeout(Constants.CONNECT_TIMEOUT)
                .withReadTimeout(Constants.READ_TIMEOUT)
                .withRedirects(Redirects.DONT_FOLLOW);
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.jdk().build(settings);

        RestClient ticketRestClient = ApiClient.buildRestClientBuilder().requestFactory(requestFactory).build();
        ApiClient ticketClient = new ApiClient(ticketRestClient);
        ticketClient.setBasePath(ticketUrl);
        tickets = new TicketsApi(ticketClient);

        RestClient personRestClient = ru.itmo.soa.booking.client.person.invoker.ApiClient.buildRestClientBuilder()
                .requestFactory(requestFactory).build();
        var personClient = new ru.itmo.soa.booking.client.person.invoker.ApiClient(personRestClient);
        personClient.setBasePath(personUrl);
        persons = new PersonsApi(personClient);
    }

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

    private static <T> T call(RemoteService service, Long notFoundId, Supplier<T> request) {
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
