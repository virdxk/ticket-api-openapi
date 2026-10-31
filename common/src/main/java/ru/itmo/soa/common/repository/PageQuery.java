package ru.itmo.soa.common.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import org.springframework.stereotype.Repository;
import ru.itmo.soa.common.error.ApiException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class PageQuery {

    private static final Pattern FILTER = Pattern.compile("^([A-Za-z.]+?)(!=|>=|<=|=|>|<|~)(.*)$", Pattern.DOTALL);
    private static final Pattern SORT = Pattern.compile("^([A-Za-z.]+),(asc|desc)$");

    public record Result<T>(List<T> items, long totalElements, int totalPages) {
    }

    private final EntityManager em;

    public PageQuery(EntityManager em) {
        this.em = em;
    }

    public <T> Result<T> find(Class<T> entity, int page, int size, List<String> sort, List<String> filter) {
        Set<String> fields = new HashSet<>();
        Set<String> joins = new HashSet<>();
        describe(em.getMetamodel().entity(entity), "", fields, joins);

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> count = cb.createQuery(Long.class);
        Root<T> countRoot = count.from(entity);
        count.select(cb.count(countRoot))
                .where(predicates(cb, countRoot, fields, joins, filter));
        long total = em.createQuery(count).getSingleResult();

        CriteriaQuery<T> select = cb.createQuery(entity);
        Root<T> root = select.from(entity);
        Map<String, From<?, ?>> joined = new HashMap<>();
        select.select(root)
                .where(predicates(cb, root, joined, fields, joins, filter))
                .orderBy(orders(cb, root, joined, fields, joins, sort));

        long offset = (long) (page - 1) * size;
        List<T> items = offset >= total ? List.of() : em.createQuery(select)
                .setFirstResult((int) offset)
                .setMaxResults(size)
                .getResultList();
        return new Result<>(items, total, (int) ((total + size - 1) / size));
    }

    private void describe(ManagedType<?> type, String prefix, Set<String> fields, Set<String> joins) {
        for (Attribute<?, ?> attribute : type.getAttributes()) {
            String name = prefix + attribute.getName();
            switch (attribute.getPersistentAttributeType()) {
                case BASIC -> fields.add(name);
                case EMBEDDED -> describe(em.getMetamodel().embeddable(attribute.getJavaType()), name + ".",
                        fields, joins);
                case ONE_TO_ONE, MANY_TO_ONE -> {
                    joins.add(name);
                    describe(em.getMetamodel().entity(attribute.getJavaType()), name + ".", fields, joins);
                }
                default -> {
                }
            }
        }
    }

    private static Predicate[] predicates(CriteriaBuilder cb, Root<?> root, Set<String> fields, Set<String> joins,
            List<String> filter) {
        return predicates(cb, root, new HashMap<>(), fields, joins, filter);
    }

    private static Predicate[] predicates(CriteriaBuilder cb, Root<?> root, Map<String, From<?, ?>> joined,
            Set<String> fields, Set<String> joins, List<String> filter) {
        List<Predicate> result = new ArrayList<>();
        Map<String, List<String[]>> byField = new HashMap<>();
        for (String raw : filter == null ? List.<String>of() : filter) {
            Matcher m = FILTER.matcher(raw);
            if (!m.matches() || !fields.contains(m.group(1))) {
                throw ApiException.badRequest("error.filter.unknown", raw);
            }
            Path<?> path = path(root, joined, joins, m.group(1));
            result.add(predicate(cb, path, m.group(1), m.group(2), m.group(3)));
            byField.computeIfAbsent(m.group(1), f -> new ArrayList<>()).add(new String[]{m.group(2), m.group(3)});
            checkSatisfiable(m.group(1), path.getJavaType(), byField.get(m.group(1)));
        }
        return result.toArray(Predicate[]::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void checkSatisfiable(String field, Class<?> type, List<String[]> conditions) {
        boolean isNull = false;
        boolean notNull = false;
        Comparable equal = null;
        Comparable lower = null;
        boolean lowerStrict = false;
        Comparable upper = null;
        boolean upperStrict = false;
        List<Comparable> excluded = new ArrayList<>();
        for (String[] c : conditions) {
            String op = c[0];
            if (op.equals("~")) continue;
            if (c[1].equals("null") && (op.equals("=") || op.equals("!="))) {
                if (op.equals("=")) isNull = true; else notNull = true;
                continue;
            }
            notNull = true;
            Comparable v = parse(type, field, c[1]);
            switch (op) {
                case "=" -> {
                    if (equal != null && equal.compareTo(v) != 0) conflict(field);
                    equal = v;
                }
                case "!=" -> excluded.add(v);
                case ">", ">=" -> {
                    int cmp = lower == null ? 1 : v.compareTo(lower);
                    if (cmp > 0 || (cmp == 0 && op.equals(">"))) { lower = v; lowerStrict = op.equals(">"); }
                }
                default -> {
                    int cmp = upper == null ? -1 : v.compareTo(upper);
                    if (cmp < 0 || (cmp == 0 && op.equals("<"))) { upper = v; upperStrict = op.equals("<"); }
                }
            }
        }
        if (isNull && notNull) conflict(field);
        if (lower != null && upper != null) {
            int cmp = lower.compareTo(upper);
            if (cmp > 0 || (cmp == 0 && (lowerStrict || upperStrict))) conflict(field);
        }
        if (equal != null) {
            Comparable eq = equal;
            if (excluded.stream().anyMatch(x -> x.compareTo(eq) == 0)) conflict(field);
            if (lower != null && (lowerStrict ? eq.compareTo(lower) <= 0 : eq.compareTo(lower) < 0)) conflict(field);
            if (upper != null && (upperStrict ? eq.compareTo(upper) >= 0 : eq.compareTo(upper) > 0)) conflict(field);
        }
    }

    private static void conflict(String field) {
        throw ApiException.badRequest("error.filter.conflict", field);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate predicate(CriteriaBuilder cb, Path<?> path, String field, String op, String raw) {
        if (op.equals("~")) {
            String pattern = "%" + raw.toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            return cb.like(cb.lower(path.as(String.class)), pattern, '\\');
        }
        if (raw.equals("null") && (op.equals("=") || op.equals("!="))) {
            return op.equals("=") ? cb.isNull(path) : cb.isNotNull(path);
        }
        Comparable value = parse(path.getJavaType(), field, raw);
        Expression<Comparable> expr = (Expression<Comparable>) path;
        return switch (op) {
            case "=" -> cb.equal(expr, value);
            case "!=" -> cb.notEqual(expr, value);
            case ">" -> cb.greaterThan(expr, value);
            case ">=" -> cb.greaterThanOrEqualTo(expr, value);
            case "<" -> cb.lessThan(expr, value);
            default -> cb.lessThanOrEqualTo(expr, value);
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparable parse(Class<?> type, String field, String raw) {
        try {
            if (type == Long.class) return Long.valueOf(raw);
            if (type == Integer.class) return Integer.valueOf(raw);
            if (type == Float.class) return Float.valueOf(raw);
            if (type == Double.class) return Double.valueOf(raw);
            if (type == LocalDate.class) return LocalDate.parse(raw);
            if (type == OffsetDateTime.class) {
                return raw.contains("T") ? OffsetDateTime.parse(raw)
                        : LocalDate.parse(raw).atStartOfDay().atOffset(ZoneOffset.UTC);
            }
            if (type.isEnum()) return Enum.valueOf((Class<Enum>) type, raw);
            return raw;
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw ApiException.badRequest("error.filter.value", raw, field);
        }
    }

    private static List<Order> orders(CriteriaBuilder cb, Root<?> root, Map<String, From<?, ?>> joined,
            Set<String> fields, Set<String> joins, List<String> sort) {
        List<Order> result = new ArrayList<>();
        Set<String> sorted = new HashSet<>();
        for (String raw : sort == null ? List.<String>of() : sort) {
            Matcher m = SORT.matcher(raw);
            if (!m.matches() || !fields.contains(m.group(1))) {
                throw ApiException.badRequest("error.sort.value", raw);
            }
            if (!sorted.add(m.group(1))) {
                throw ApiException.badRequest("error.sort.duplicate", m.group(1));
            }
            Path<?> path = path(root, joined, joins, m.group(1));
            result.add(m.group(2).equals("asc") ? cb.asc(path) : cb.desc(path));
        }
        result.add(cb.asc(root.get("id")));
        return result;
    }

    private static Path<?> path(Root<?> root, Map<String, From<?, ?>> joined, Set<String> joins, String field) {
        String[] parts = field.split("\\.");
        Path<?> path = joins.contains(parts[0])
                ? joined.computeIfAbsent(parts[0], j -> root.join(j, JoinType.LEFT))
                : root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }
        return path;
    }
}
