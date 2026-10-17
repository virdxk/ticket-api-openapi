package ru.itmo.soa.common.error;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import java.util.Arrays;

public record Violation(String field, MessageSourceResolvable message) {

    public static Violation of(String field, String code, Object... args) {
        return new Violation(field, message(code, args));
    }

    public static Violation text(String field, String text) {
        return new Violation(field, new DefaultMessageSourceResolvable(null, null, text));
    }

    static MessageSourceResolvable message(String code, Object... args) {
        return new DefaultMessageSourceResolvable(new String[]{code},
                Arrays.stream(args).map(String::valueOf).toArray());
    }
}
