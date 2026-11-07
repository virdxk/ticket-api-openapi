package ru.itmo.soa.common.error;

import lombok.Getter;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import java.io.Serial;
import java.util.Arrays;

@Getter
public class ApiException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final MessageSourceResolvable error;
    private final String field;

    public ApiException(HttpStatus status, String code, Object... args) {
        this(status, null, null, code, args);
    }

    public ApiException(HttpStatus status, Throwable cause, String code, Object... args) {
        this(status, cause, null, code, args);
    }

    private ApiException(
            HttpStatus status,
            Throwable cause,
            String field,
            String code,
            Object... args
    ) {
        super(code, cause);
        this.status = status;
        this.error = new DefaultMessageSourceResolvable(
                new String[]{code},
                Arrays.stream(args).map(String::valueOf).toArray()
        );
        this.field = field;
    }

    public static ApiException badRequest(String code, Object... args) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, args);
    }

    public static ApiException notFound(String code, Object... args) {
        return new ApiException(HttpStatus.NOT_FOUND, code, args);
    }

    public static ApiException invalid(String field, String code, Object... args) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, null, field, code, args);
    }
}
