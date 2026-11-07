package ru.itmo.soa.booking.client;

import lombok.RequiredArgsConstructor;
import ru.itmo.soa.booking.utils.Constants;

@RequiredArgsConstructor
enum RemoteService {

    TICKET(Constants.TICKET_SERVICE, Constants.TICKET_NOT_FOUND),
    PERSON(Constants.PERSON_SERVICE, Constants.PERSON_NOT_FOUND);

    private final String displayName;
    private final String notFoundCode;

    String getDisplayName() {
        return displayName;
    }

    String getNotFoundCode() {
        return notFoundCode;
    }
}
