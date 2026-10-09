package ru.itmo.soa.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // A single "sort=price,desc" must stay one list element instead of being split on the comma.
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, List.class, (Converter<String, List>) List::of);
    }

    // The client page is served by se.ifmo.ru and calls the services through an ssh tunnel on localhost.
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PATCH", "DELETE")
                .exposedHeaders("Location")
                .allowPrivateNetwork(true);
    }
}
