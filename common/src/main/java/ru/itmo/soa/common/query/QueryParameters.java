package ru.itmo.soa.common.query;

import jakarta.persistence.Version;
import ru.itmo.soa.common.error.ApiException;
import ru.itmo.soa.common.utils.Constants;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;

public final class QueryParameters {

    public static String whereClause(
            Class<?> entity,
            List<String> filters,
            Function<String, String> columnName,
            Map<String, Object> parameters
    ) {
        StringBuilder where = new StringBuilder();
        for (String filter : filters == null ? List.<String>of() : filters) {
            Matcher matcher = Constants.FILTER_FORMAT.matcher(filter);
            if (!matcher.matches()) {
                throw ApiException.badRequest(Constants.ERROR_FILTER_UNKNOWN, filter);
            }
            String field = matcher.group(1);
            String operator = matcher.group(2);
            String value = matcher.group(3);
            Class<?> type;
            try {
                type = fieldType(entity, field);
            } catch (NoSuchFieldException ex) {
                throw ApiException.badRequest(Constants.ERROR_FILTER_UNKNOWN, filter);
            }
            String column = columnName.apply(field);
            where.append(where.isEmpty() ? " where " : " and ");
            if (value.equals("null") && (operator.equals("=") || operator.equals("!="))) {
                where.append(column).append(operator.equals("=") ? " is null" : " is not null");
                continue;
            }
            String parameter = "filter" + parameters.size();
            if (operator.equals("~")) {
                where.append("lower(str(").append(column).append(")) like :").append(parameter).append(" escape '\\'");
                parameters.put(
                        parameter,
                        "%" + value.toLowerCase(Locale.ROOT)
                                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
                );
            } else {
                where.append(column).append(" ").append(operator.equals("!=") ? "<>" : operator)
                        .append(" :").append(parameter);
                parameters.put(parameter, filterValue(type, field, value));
            }
        }
        return where.toString();
    }

    public static String orderBy(Class<?> entity, List<String> sort, Function<String, String> columnName) {
        List<String> orders = new ArrayList<>();
        Set<String> sorted = new HashSet<>();
        for (String value : sort == null ? List.<String>of() : sort) {
            Matcher matcher = Constants.SORT_FORMAT.matcher(value);
            if (!matcher.matches()) {
                throw ApiException.badRequest(Constants.ERROR_SORT_VALUE, value);
            }
            String field = matcher.group(1);
            try {
                fieldType(entity, field);
            } catch (NoSuchFieldException ex) {
                throw ApiException.badRequest(Constants.ERROR_SORT_VALUE, value);
            }
            if (!sorted.add(field)) {
                throw ApiException.badRequest(Constants.ERROR_SORT_DUPLICATE, field);
            }
            orders.add(columnName.apply(field) + " " + matcher.group(2));
        }
        if (sorted.add("id")) {
            orders.add(columnName.apply("id") + " asc");
        }
        return String.join(", ", orders);
    }

    private static Class<?> fieldType(Class<?> entity, String path) throws NoSuchFieldException {
        Class<?> type = entity;
        for (String name : path.split("\\.", -1)) {
            Field field = type.getField(name);
            if (field.isAnnotationPresent(Version.class) || Modifier.isStatic(field.getModifiers())) {
                throw new NoSuchFieldException(path);
            }
            type = field.getType();
        }
        if (!Comparable.class.isAssignableFrom(type)) {
            throw new NoSuchFieldException(path);
        }
        return type;
    }

    private static Object filterValue(Class<?> type, String field, String value) {
        try {
            if (type == String.class) return value;
            if (type == Long.class) return Long.valueOf(value);
            if (type == Integer.class) return Integer.valueOf(value);
            if (type == Float.class) return Float.valueOf(value);
            if (type == Double.class) return Double.valueOf(value);
            if (type == LocalDate.class) return LocalDate.parse(value);
            if (type == OffsetDateTime.class) {
                return value.contains("T") ? OffsetDateTime.parse(value)
                        : LocalDate.parse(value).atStartOfDay().atOffset(ZoneOffset.UTC);
            }
            if (type.isEnum()) {
                for (Object option : type.getEnumConstants()) {
                    if (option.toString().equals(value)) return option;
                }
            }
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw ApiException.badRequest(Constants.ERROR_FILTER_VALUE, value, field);
        }
        throw ApiException.badRequest(Constants.ERROR_FILTER_VALUE, value, field);
    }
}
