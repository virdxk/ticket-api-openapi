package ru.itmo.soa.common.utils;

import java.util.regex.Pattern;

public final class Constants {

    public static final String RESOURCE_ID_PATH = "/{id}";
    public static final String ERROR_INTERNAL = "error.internal";
    public static final String ERROR_CONFLICT = "error.conflict";
    public static final String ERROR_BODY_INVALID = "error.body.invalid";
    public static final String ERROR_BODY_EMPTY = "error.body.empty";
    public static final String ERROR_FIELD_UNKNOWN = "error.field.unknown";
    public static final String ERROR_PARAM_VALUE = "error.param.value";
    public static final String ERROR_PARAM_CONSTRAINTS = "error.param.constraints";
    public static final String ERROR_FILTER_UNKNOWN = "error.filter.unknown";
    public static final String ERROR_FILTER_VALUE = "error.filter.value";
    public static final String ERROR_SORT_VALUE = "error.sort.value";
    public static final String ERROR_SORT_DUPLICATE = "error.sort.duplicate";
    public static final String ERROR_HTTP = "error.http";
    public static final String ERROR_HTTP_PREFIX = "error.http.";

    public static final Pattern FILTER_FORMAT = Pattern.compile(
            "^([A-Za-z.]+?)(!=|>=|<=|=|>|<|~)(.*)$",
            Pattern.DOTALL
    );
    public static final Pattern SORT_FORMAT = Pattern.compile("^([A-Za-z.]+),(asc|desc)$");
}
