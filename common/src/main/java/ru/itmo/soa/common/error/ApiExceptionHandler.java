package ru.itmo.soa.common.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Autowired
    private MessageSource messages;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApi(ApiException ex) {
        return respond(ex.getStatus(), ex.getError(), ex.getDetails());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations().stream()
                .map(v -> parameterName(v.getPropertyPath().toString()) + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, Violation.message("error.param.constraints", errors), List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                Violation.message("error.param.value", ex.getName(), ex.getValue()), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, Violation.message("error.internal"), List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Violation> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Violation.text(e.getField(), e.getDefaultMessage()))
                .distinct()
                .toList();
        return handleApi(ApiException.invalid(details));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = mismatch.getPath().stream()
                    .map(r -> r.getFieldName() != null ? r.getFieldName() : String.valueOf(r.getIndex()))
                    .collect(Collectors.joining("."));
            return handleApi(ApiException.invalid(List.of(mismatch(field, mismatch))));
        }
        return respond(HttpStatus.BAD_REQUEST, Violation.message("error.body.malformed"), List.of());
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, Violation.message("error.param.constraints", errors), List.of());
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        return respond(statusCode, Violation.message("error.http." + statusCode.value()), List.of());
    }

    private static Violation mismatch(String field, MismatchedInputException ex) {
        if (ex instanceof UnrecognizedPropertyException) {
            return Violation.of(field, "error.field.unknown");
        }
        if (ex instanceof InvalidNullException) {
            return Violation.of(field, "error.field.null");
        }
        if (ex instanceof InvalidFormatException format) {
            return Violation.of(field, "error.field.value", format.getValue());
        }
        return Violation.of(field, "error.field.type");
    }

    private static String parameterName(String propertyPath) {
        int dot = propertyPath.indexOf('.');
        return dot < 0 ? propertyPath : propertyPath.substring(dot + 1);
    }

    private String text(MessageSourceResolvable message) {
        return messages.getMessage(message, LocaleContextHolder.getLocale());
    }

    private ResponseEntity<Object> respond(HttpStatusCode status, MessageSourceResolvable message,
            List<Violation> details) {
        List<ApiError.Detail> resolved = details.stream()
                .map(v -> new ApiError.Detail(v.field(), text(v.message())))
                .distinct()
                .toList();
        return ResponseEntity.status(status).body(ApiError.of(text(message), resolved));
    }
}
