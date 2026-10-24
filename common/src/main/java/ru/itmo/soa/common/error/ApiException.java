package ru.itmo.soa.common.error;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

public class ApiException extends RuntimeException {

    public record Violation(String field, MessageSourceResolvable message) {

        public static Violation of(String field, String code, Object... args) {
            return new Violation(field, ApiException.message(code, args));
        }

        public static Violation text(String field, String text) {
            return new Violation(field, new DefaultMessageSourceResolvable(null, null, text));
        }
    }

    private final HttpStatus status;
    private final MessageSourceResolvable error;
    private final List<Violation> details;

    public ApiException(HttpStatus status, String code, Object... args) {
        this(status, List.of(), code, args);
    }

    private ApiException(HttpStatus status, List<Violation> details, String code, Object... args) {
        super(code);
        this.status = status;
        this.error = message(code, args);
        this.details = details;
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

    static MessageSourceResolvable message(String code, Object... args) {
        return new DefaultMessageSourceResolvable(new String[]{code}, Arrays.stream(args).map(String::valueOf).toArray());
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
}
