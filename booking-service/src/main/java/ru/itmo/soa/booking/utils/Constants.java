package ru.itmo.soa.booking.utils;

import java.time.Duration;

public final class Constants {

    public static final String BOOKING_LOCATION_PATH = "/booking/bookings/{id}";
    public static final String BOOKING_NOT_FOUND = "booking.notFound";
    public static final String TICKET_NOT_FOUND = "ticket.notFound";
    public static final String PERSON_NOT_FOUND = "person.notFound";
    public static final String TICKET_NO_PRICE = "ticket.noPrice";
    public static final String TICKET_PRICE_OVERFLOW = "ticket.priceOverflow";
    public static final String SERVICE_FAILED = "service.failed";
    public static final String SERVICE_UNAVAILABLE = "service.unavailable";
    public static final String TICKET_SERVICE = "Ticket Service";
    public static final String PERSON_SERVICE = "Person Service";
    public static final String TICKET_SERVICE_URL = "${ticket-service.url}";
    public static final String PERSON_SERVICE_URL = "${person-service.url}";
    public static final String SERVICES_SSL_BUNDLE = "services";
    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
}
