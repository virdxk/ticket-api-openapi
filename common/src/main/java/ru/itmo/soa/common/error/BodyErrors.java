package ru.itmo.soa.common.error;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import jakarta.validation.Validator;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestControllerAdvice
public class BodyErrors extends RequestBodyAdviceAdapter {

    private static final ThreadLocal<List<Violation>> ERRORS = ThreadLocal.withInitial(ArrayList::new);

    private final Validator validator;

    public BodyErrors(Validator validator) {
        this.validator = validator;
    }

    public static final DeserializationProblemHandler HANDLER = new DeserializationProblemHandler() {
        @Override
        public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String value,
                String failureMsg) {
            return record(ctxt.getParser(), targetType, Violation.of(path(ctxt.getParser()), "error.field.value", value));
        }

        @Override
        public Object handleWeirdNumberValue(DeserializationContext ctxt, Class<?> targetType, Number value,
                String failureMsg) {
            return record(ctxt.getParser(), targetType, Violation.of(path(ctxt.getParser()), "error.field.value", value));
        }

        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt, JavaType targetType,
                com.fasterxml.jackson.core.JsonToken token, JsonParser p, String failureMsg) throws IOException {
            p.skipChildren();
            return record(p, targetType.getRawClass(), Violation.of(path(p), "error.field.type"));
        }

        @Override
        public Object handleInstantiationProblem(DeserializationContext ctxt, Class<?> instClass, Object argument,
                Throwable t) {
            return record(ctxt.getParser(), instClass, Violation.of(path(ctxt.getParser()), "error.field.value", argument));
        }

        @Override
        public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException {
            ERRORS.get().add(Violation.of(path(p), "error.field.unknown"));
            p.skipChildren();
            return true;
        }
    };

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object record(JsonParser p, Class<?> type, Violation violation) {
        ERRORS.get().add(violation);
        if (type == Float.class) return 0f;
        if (type == Double.class) return 0d;
        if (type == Long.class) return 0L;
        if (type == Integer.class) return 0;
        if (type == String.class) return "";
        if (type == LocalDate.class) return LocalDate.EPOCH;
        if (type == OffsetDateTime.class) return OffsetDateTime.now();
        if (type.isEnum()) return type.getEnumConstants()[0];
        return null;
    }

    private static String path(JsonParser p) {
        return p.getParsingContext().pathAsPointer().toString().substring(1).replace('/', '.');
    }

    @Override
    public boolean supports(MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage message, MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        ERRORS.get().clear();
        return message;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage message, MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        List<Violation> details = new ArrayList<>(ERRORS.get());
        ERRORS.get().clear();
        if (details.isEmpty()) {
            return body;
        }
        Set<String> broken = details.stream().map(Violation::field).collect(Collectors.toSet());
        validator.validate(body).stream()
                .map(v -> Violation.text(v.getPropertyPath().toString(), v.getMessage()))
                .filter(d -> !broken.contains(d.field()))
                .distinct()
                .forEach(details::add);
        throw ApiException.invalid(details);
    }
}
