package ru.itmo.soa.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Forwards /api/** to the services unchanged, so the browser talks to one origin
 * and does not have to trust three self-signed certificates.
 */
@RestController
public class ProxyController {

    private final RestClient http = RestClient.create();
    private final Map<String, String> upstreams;

    public ProxyController(@Value("${ticket-service.url}") String ticketUrl,
                           @Value("${person-service.url}") String personUrl,
                           @Value("${booking-service.url}") String bookingUrl) {
        upstreams = Map.of("tickets", ticketUrl, "persons", personUrl, "booking", bookingUrl);
    }

    @RequestMapping("/api/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request, HttpMethod method,
                                        @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI().substring(request.getContextPath().length() + "/api".length());
        String base = upstreams.get(path.split("/")[1]);
        if (base == null) {
            return error(HttpStatus.NOT_FOUND, "Неизвестный сервис: " + path);
        }
        String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        RestClient.RequestBodySpec spec = http.method(method).uri(URI.create(base + path + query));
        if (request.getContentType() != null) {
            spec.header(HttpHeaders.CONTENT_TYPE, request.getContentType());
        }
        if (body != null) {
            spec.body(body);
        }
        try {
            return spec.exchange((req, res) -> {
                HttpHeaders headers = new HttpHeaders();
                if (res.getHeaders().getContentType() != null) {
                    headers.setContentType(res.getHeaders().getContentType());
                }
                if (res.getHeaders().getLocation() != null) {
                    headers.setLocation(res.getHeaders().getLocation());
                }
                return new ResponseEntity<>(res.getBody().readAllBytes(), headers, res.getStatusCode());
            });
        } catch (ResourceAccessException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "Сервис недоступен: " + base);
        }
    }

    private static ResponseEntity<byte[]> error(HttpStatus status, String message) {
        String json = "{\"message\":\"" + message.replace("\"", "'") + "\",\"time\":\""
                + OffsetDateTime.now(ZoneOffset.UTC) + "\"}";
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(json.getBytes());
    }
}
