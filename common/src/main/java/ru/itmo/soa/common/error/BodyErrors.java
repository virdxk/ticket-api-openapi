package ru.itmo.soa.common.error;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import ru.itmo.soa.common.error.ApiException.Violation;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class BodyErrors extends RequestBodyAdviceAdapter {

    private static final ThreadLocal<List<Violation>> ERRORS = ThreadLocal.withInitial(ArrayList::new);

    private static final Map<Class<?>, Object> PLACEHOLDERS = Map.of(
            String.class, "", Integer.class, 0, Long.class, 0L, Float.class, 0f, Double.class, 0d,
            LocalDate.class, LocalDate.EPOCH, OffsetDateTime.class, OffsetDateTime.MIN);

    public static final DeserializationProblemHandler HANDLER = new DeserializationProblemHandler() {
        @Override
        public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> type, String value, String msg) {
            return record(ctxt.getParser(), type, "error.field.value", value);
        }

        @Override
        public Object handleWeirdNumberValue(DeserializationContext ctxt, Class<?> type, Number value, String msg) {
            return record(ctxt.getParser(), type, "error.field.value", value);
        }

        @Override
        public Object handleInstantiationProblem(DeserializationContext ctxt, Class<?> type, Object value, Throwable t) {
            return record(ctxt.getParser(), type, "error.field.value", value);
        }

        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt, JavaType type, JsonToken token, JsonParser p,
                String msg) throws IOException {
            p.skipChildren();
            return record(p, type.getRawClass(), "error.field.type");
        }

        @Override
        public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, JsonDeserializer<?> deserializer,
                Object bean, String property) throws IOException {
            p.skipChildren();
            record(p, Object.class, "error.field.unknown");
            return true;
        }
    };

    @Autowired
    private Validator validator;

    private static Object record(JsonParser p, Class<?> type, String code, Object... args) {
        String field = p.getParsingContext().pathAsPointer().toString().replaceFirst("^/", "").replace('/', '.');
        ERRORS.get().add(Violation.of(field, code, args));
        return type.isEnum() ? type.getEnumConstants()[0] : PLACEHOLDERS.get(type);
    }

    @Override
    public boolean supports(MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage message, MethodParameter parameter, Type type,
            Class<? extends HttpMessageConverter<?>> converter) {
        ERRORS.get().clear();
        return message;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage message, MethodParameter parameter, Type type,
            Class<? extends HttpMessageConverter<?>> converter) {
        List<Violation> errors = new ArrayList<>(ERRORS.get());
        if (errors.isEmpty()) {
            return body;
        }
        if (body == null) {
            throw ApiException.invalid(errors);
        }
        List<String> broken = errors.stream().map(Violation::field).toList();
        validator.validate(body).stream()
                .filter(v -> !broken.contains(v.getPropertyPath().toString()))
                .forEach(v -> errors.add(Violation.text(v.getPropertyPath().toString(), v.getMessage())));
        throw ApiException.invalid(errors);
    }
}
