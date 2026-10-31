package ru.itmo.soa.common.config;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.itmo.soa.common.error.BodyErrors;

@Configuration
public class JacksonConfig {

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
                    mapper.setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.FAIL));
                    mapper.configOverride(JsonNullable.class)
                            .setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.SET));
                    mapper.addHandler(BodyErrors.HANDLER);
                });
    }
}
