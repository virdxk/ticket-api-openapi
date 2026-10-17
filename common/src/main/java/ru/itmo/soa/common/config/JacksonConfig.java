package ru.itmo.soa.common.config;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.itmo.soa.common.error.BodyErrors;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Configuration
public class JacksonConfig {

    private static final List<Class<?>> NON_NULLABLE = List.of(
            String.class, Integer.class, Long.class, Float.class, Double.class,
            LocalDate.class, OffsetDateTime.class);

    @Bean
    public JsonNullableModule jsonNullableModule() {
        return new JsonNullableModule();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictJson() {
        return builder -> builder
                .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .postConfigurer(mapper -> {
                    NON_NULLABLE.forEach(type ->
                            mapper.configOverride(type).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.FAIL)));
                    mapper.addHandler(BodyErrors.HANDLER);
                });
    }
}
