package ru.itmo.soa.common;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<ApiError.Detail> details;

    public ApiException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public ApiException(HttpStatus status, String message, List<ApiError.Detail> details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public List<ApiError.Detail> getDetails() {
        return details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }
}
