package ru.itmo.soa.common.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.itmo.soa.common.error.ApiException.Violation;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final MessageSource messages;

    public ApiExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        List<ApiError.Detail> details = ex.getDetails().stream()
                .map(v -> new ApiError.Detail(v.field(), text(v.message())))
                .distinct()
                .toList();
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(text(ex.getError()), details));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex) {
        return handleApi(ApiException.invalid(ex.getFieldErrors().stream()
                .map(e -> Violation.text(e.getField(), e.getDefaultMessage()))
                .toList()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidNullException e) {
            return handleApi(ApiException.invalid(List.of(Violation.of(path(e), "error.field.null"))));
        }
        return handleApi(ApiException.badRequest("error.body.malformed"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleInvalidParameter(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath().toString().replaceFirst("^[^.]*\\.", "") + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return handleApi(ApiException.badRequest("error.param.constraints", errors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleInvalidParameter(HandlerMethodValidationException ex) {
        String errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return handleApi(ApiException.badRequest("error.param.constraints", errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleWrongParameter(MethodArgumentTypeMismatchException ex) {
        return handleApi(ApiException.badRequest("error.param.value", ex.getName(), ex.getValue()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        if (ex instanceof ErrorResponse response) {
            HttpStatusCode status = response.getStatusCode();
            MessageSourceResolvable message = new DefaultMessageSourceResolvable(
                    new String[]{"error.http." + status.value()}, null, ex.getMessage());
            return ResponseEntity.status(status).body(ApiError.of(text(message), List.of()));
        }
        log.error("Unexpected error", ex);
        return handleApi(new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal"));
    }

    private String text(MessageSourceResolvable message) {
        return messages.getMessage(message, LocaleContextHolder.getLocale());
    }

    private static String path(JsonMappingException ex) {
        return ex.getPath().stream().map(JsonMappingException.Reference::getFieldName).collect(Collectors.joining("."));
    }
}
