package ru.itmo.soa.common;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final String INVALID_BODY = "JSON запроса нарушает ограничения модели";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApi(ApiException ex) {
        return respond(ex.getStatus(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> parameterName(v.getPropertyPath().toString()) + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, message, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                "Параметр " + ex.getName() + ": недопустимое значение \"" + ex.getValue() + "\"", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiError.Detail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiError.Detail(e.getField(), e.getDefaultMessage()))
                .distinct()
                .toList();
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, INVALID_BODY, details);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = mismatch.getPath().stream()
                    .map(r -> r.getFieldName() != null ? r.getFieldName() : String.valueOf(r.getIndex()))
                    .collect(Collectors.joining("."));
            return respond(HttpStatus.UNPROCESSABLE_ENTITY, INVALID_BODY,
                    List.of(new ApiError.Detail(field, mismatchMessage(mismatch))));
        }
        if (ex.getCause() instanceof JsonMappingException mapping && mapping.getCause() != null) {
            return respond(HttpStatus.UNPROCESSABLE_ENTITY, INVALID_BODY,
                    List.of(new ApiError.Detail("", mapping.getCause().getMessage())));
        }
        return respond(HttpStatus.BAD_REQUEST, "Тело запроса отсутствует или не является корректным JSON", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> r.getMethodParameter().getParameterName() + ": " + e.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, message, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        return respond(statusCode, ex.getMessage(), List.of());
    }

    private static String mismatchMessage(MismatchedInputException ex) {
        if (ex instanceof UnrecognizedPropertyException) {
            return "неизвестное поле";
        }
        if (ex instanceof InvalidNullException) {
            return "не может быть null";
        }
        if (ex instanceof InvalidFormatException format) {
            return "недопустимое значение \"" + format.getValue() + "\"";
        }
        return "неверный тип значения";
    }

    private static String parameterName(String propertyPath) {
        int dot = propertyPath.indexOf('.');
        return dot < 0 ? propertyPath : propertyPath.substring(dot + 1);
    }

    private static ResponseEntity<Object> respond(HttpStatusCode status, String message, List<ApiError.Detail> details) {
        return ResponseEntity.status(status).body(ApiError.of(message, details));
    }
}
