package ru.itmo.soa.common.error;

import lombok.RequiredArgsConstructor;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.itmo.soa.common.utils.Constants;
import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final MessageSource messages;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        String message = text(ex.getError());
        List<ApiError.Detail> details = List.of();
        if (ex.getField() != null) {
            details = List.of(new ApiError.Detail(ex.getField(), message));
            message = text(Constants.ERROR_BODY_INVALID);
        }
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(message, details));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex) {
        var error = ex.getFieldErrors().get(0);
        return ResponseEntity.unprocessableEntity().body(ApiError.of(
                text(Constants.ERROR_BODY_INVALID),
                List.of(new ApiError.Detail(error.getField(), error.getDefaultMessage()))
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return handleApi(ApiException.badRequest(Constants.ERROR_FIELD_UNKNOWN));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleInvalidParameter(ConstraintViolationException ex) {
        var error = ex.getConstraintViolations().iterator().next();
        String parameter = error.getPropertyPath().toString().replaceFirst("^[^.]*\\.", "");
        return handleApi(ApiException.badRequest(Constants.ERROR_PARAM_CONSTRAINTS, parameter, error.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleWrongParameter(MethodArgumentTypeMismatchException ex) {
        return handleApi(ApiException.badRequest(Constants.ERROR_PARAM_VALUE, ex.getName(), ex.getValue()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockException) {
                return handleApi(new ApiException(HttpStatus.CONFLICT, ex, Constants.ERROR_CONFLICT));
            }
        }
        if (ex instanceof ErrorResponse response) {
            HttpStatusCode status = response.getStatusCode();
            String message = messages.getMessage(
                    Constants.ERROR_HTTP_PREFIX + status.value(),
                    null,
                    text(Constants.ERROR_HTTP),
                    LocaleContextHolder.getLocale()
            );
            return ResponseEntity.status(status).headers(response.getHeaders()).body(ApiError.of(message, List.of()));
        }
        return handleApi(new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ex, Constants.ERROR_INTERNAL));
    }

    private String text(MessageSourceResolvable message) {
        return messages.getMessage(message, LocaleContextHolder.getLocale());
    }

    private String text(String code) {
        return messages.getMessage(code, null, LocaleContextHolder.getLocale());
    }

}
