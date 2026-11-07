package ru.itmo.soa.common.query;

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

    public String whereClause(
            Class<?> entity,
            List<String> filters,
            Function<String, String> columnName,
            Map<String, Object> parameters
    ) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }
        List<String> conditions = new ArrayList<>();
        for (String filter : filters) {
            conditions.add(filterCondition(entity, filter, columnName, parameters));
        }
        return " where " + String.join(" and ", conditions);
    }

    public String orderBy(Class<?> entity, List<String> sort, Function<String, String> columnName) {
        List<String> orders = new ArrayList<>();
        Set<String> sorted = new HashSet<>();
        List<String> values = sort == null ? List.of() : sort;
        for (String value : values) {
            Matcher matcher = parseSort(entity, value);
            String field = matcher.group(1);
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

    private Matcher parseFilter(String filter) {
        Matcher matcher = Constants.FILTER_FORMAT.matcher(filter);
        if (!matcher.matches()) {
            throw ApiException.badRequest(Constants.ERROR_FILTER_UNKNOWN, filter);
        }
        return matcher;
    }

    private Matcher parseSort(Class<?> entity, String sort) {
        Matcher matcher = Constants.SORT_FORMAT.matcher(sort);
        if (!matcher.matches()) {
            throw ApiException.badRequest(Constants.ERROR_SORT_VALUE, sort);
        }
        try {
            fieldType(entity, matcher.group(1));
        } catch (NoSuchFieldException ex) {
            throw ApiException.badRequest(Constants.ERROR_SORT_VALUE, sort);
        }
        return matcher;
    }

    private String filterCondition(
            Class<?> entity,
            String filter,
            Function<String, String> columnName,
            Map<String, Object> parameters
    ) {
        Matcher matcher = parseFilter(filter);
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
        if (value.equals("null") && (operator.equals("=") || operator.equals("!="))) {
            return column + (operator.equals("=") ? " is null" : " is not null");
        }
        String parameter = "filter" + parameters.size();
        if (operator.equals("~")) {
            parameters.put(parameter, containsPattern(value));
            return "lower(str(" + column + ")) like :" + parameter + " escape '\\'";
        }
        parameters.put(parameter, filterValue(type, field, value));
        String comparison = operator.equals("!=") ? "<>" : operator;
        return column + " " + comparison + " :" + parameter;
    }

    private String containsPattern(String value) {
        return "%" + value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private Class<?> fieldType(Class<?> entity, String path) throws NoSuchFieldException {
        Class<?> type = entity;
        for (String name : path.split("\\.", -1)) {
            Field field = type.getField(name);
            if (Modifier.isStatic(field.getModifiers())) {
                throw new NoSuchFieldException(path);
            }
            type = field.getType();
        }
        if (!Comparable.class.isAssignableFrom(type)) {
            throw new NoSuchFieldException(path);
        }
        return type;
    }

    private Object filterValue(Class<?> type, String field, String value) {
        try {
            if (type == String.class) return value;
            if (type == Long.class) return Long.valueOf(value);
            if (type == Integer.class) return Integer.valueOf(value);
            if (type == Float.class) return Float.valueOf(value);
            if (type == Double.class) return Double.valueOf(value);
            if (type == LocalDate.class) return LocalDate.parse(value);
            if (type == OffsetDateTime.class) return parseDateTime(value);
            if (type.isEnum()) return parseEnum(type, field, value);
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw ApiException.badRequest(Constants.ERROR_FILTER_VALUE, value, field);
        }
        throw ApiException.badRequest(Constants.ERROR_FILTER_VALUE, value, field);
    }

    private OffsetDateTime parseDateTime(String value) {
        return value.contains("T") ? OffsetDateTime.parse(value)
                : LocalDate.parse(value).atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private Object parseEnum(Class<?> type, String field, String value) {
        for (Object option : type.getEnumConstants()) {
            if (option.toString().equals(value)) return option;
        }
        throw ApiException.badRequest(Constants.ERROR_FILTER_VALUE, value, field);
    }
}
