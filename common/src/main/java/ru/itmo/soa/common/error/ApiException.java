package ru.itmo.soa.common.error;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;

import java.util.List;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final MessageSourceResolvable error;
    private final List<Violation> details;

    public ApiException(HttpStatus status, String code, Object... args) {
        this(status, List.of(), code, args);
    }

    public ApiException(HttpStatus status, List<Violation> details, String code, Object... args) {
        super(code);
        this.status = status;
        this.error = Violation.message(code, args);
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public MessageSourceResolvable getError() {
        return error;
    }

    public List<Violation> getDetails() {
        return details;
    }

    public static ApiException badRequest(String code, Object... args) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, args);
    }

    public static ApiException notFound(String code, Object... args) {
        return new ApiException(HttpStatus.NOT_FOUND, code, args);
    }

    public static ApiException invalid(List<Violation> details) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, details, "error.body.invalid");
    }
}
