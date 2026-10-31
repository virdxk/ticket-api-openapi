package ru.itmo.soa.common.error;

import jakarta.persistence.OptimisticLockException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

// common has no spring-orm on the classpath; handler resolution walks the cause chain of the translated DataAccessException
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConcurrencyExceptionHandler {

    private final MessageSource messages;

    public ConcurrencyExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockException ex) {
        String message = messages.getMessage("error.conflict", null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(message, List.of()));
    }
}
