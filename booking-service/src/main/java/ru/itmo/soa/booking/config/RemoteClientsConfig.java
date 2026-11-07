package ru.itmo.soa.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import ru.itmo.soa.booking.client.person.PersonsApi;
import ru.itmo.soa.booking.client.ticket.TicketsApi;
import ru.itmo.soa.booking.client.ticket.invoker.ApiClient;
import ru.itmo.soa.booking.utils.Constants;

@Configuration
public class RemoteClientsConfig {

    @Bean
    public ClientHttpRequestFactory servicesRequestFactory(SslBundles ssl) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withSslBundle(ssl.getBundle(Constants.SERVICES_SSL_BUNDLE))
                .withConnectTimeout(Constants.CONNECT_TIMEOUT)
                .withReadTimeout(Constants.READ_TIMEOUT)
                .withRedirects(Redirects.DONT_FOLLOW);
        return ClientHttpRequestFactoryBuilder.jdk().build(settings);
    }

    @Bean
    public TicketsApi ticketsApi(
            @Value(Constants.TICKET_SERVICE_URL) String url,
            ClientHttpRequestFactory servicesRequestFactory
    ) {
        RestClient restClient = ApiClient.buildRestClientBuilder()
                .requestFactory(servicesRequestFactory)
                .build();
        ApiClient client = new ApiClient(restClient);
        client.setBasePath(url);
        return new TicketsApi(client);
    }

    @Bean
    public PersonsApi personsApi(
            @Value(Constants.PERSON_SERVICE_URL) String url,
            ClientHttpRequestFactory servicesRequestFactory
    ) {
        RestClient restClient = ru.itmo.soa.booking.client.person.invoker.ApiClient.buildRestClientBuilder()
                .requestFactory(servicesRequestFactory)
                .build();
        var client = new ru.itmo.soa.booking.client.person.invoker.ApiClient(restClient);
        client.setBasePath(url);
        return new PersonsApi(client);
    }
}
