package ru.itmo.soa.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String message, OffsetDateTime time, List<Detail> details) {

    public record Detail(String field, String message) {
    }

    public static ApiError of(String message, List<Detail> details) {
        return new ApiError(message, OffsetDateTime.now(ZoneOffset.UTC), details);
    }
}
