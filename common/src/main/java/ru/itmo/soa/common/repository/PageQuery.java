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
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.stereotype.Repository;
import ru.itmo.soa.common.error.ApiException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class PageQuery {

    private static final Pattern FILTER = Pattern.compile("^([A-Za-z.]+?)(!=|>=|<=|=|>|<|~)(.*)$", Pattern.DOTALL);
    private static final Pattern SORT = Pattern.compile("^([A-Za-z.]+),(asc|desc)$");

    private static final ConcurrentMap<Class<?>, EntityShape> SHAPES = new ConcurrentHashMap<>();

    public record Result<T>(List<T> items, long totalElements, int totalPages) {
    }

    private record EntityShape(Set<String> fields, Set<String> joins, Map<String, Class<?>> types) {
    }

    private record FilterCondition(String field, String op, Comparable<?> value, boolean nullLiteral) {
    }

    private record SortField(String field, boolean ascending) {
    }

    private final EntityManager em;

    public PageQuery(EntityManager em) {
        this.em = em;
    }

    public <T> Result<T> find(Class<T> entity, int page, int size, List<String> sort, List<String> filter) {
        EntityShape shape = SHAPES.computeIfAbsent(entity, this::describe);
        List<FilterCondition> conditions = parseFilters(shape, filter);
        checkConflicts(conditions);
        List<SortField> sorts = parseSorts(shape, sort);

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> count = cb.createQuery(Long.class);
        Root<T> countRoot = count.from(entity);
        count.select(cb.count(countRoot))
                .where(predicates(cb, countRoot, new HashMap<>(), shape, conditions));
        long total = em.createQuery(count).getSingleResult();

        CriteriaQuery<T> select = cb.createQuery(entity);
        Root<T> root = select.from(entity);
        Map<String, From<?, ?>> joined = fetchJoins(root, shape.joins());
        select.select(root)
                .where(predicates(cb, root, joined, shape, conditions))
                .orderBy(orders(cb, root, joined, shape, sorts));

        long offset = (long) (page - 1) * size;
        List<T> items = offset >= total ? List.of() : em.createQuery(select)
                .setFirstResult((int) offset)
                .setMaxResults(size)
                .getResultList();
        return new Result<>(items, total, (int) ((total + size - 1) / size));
    }

    private EntityShape describe(Class<?> entity) {
        Set<String> fields = new HashSet<>();
        Set<String> joins = new HashSet<>();
        Map<String, Class<?>> types = new HashMap<>();
        describe(em.getMetamodel().entity(entity), "", fields, joins, types);
        return new EntityShape(fields, joins, types);
    }

    private void describe(ManagedType<?> type, String prefix, Set<String> fields, Set<String> joins,
            Map<String, Class<?>> types) {
        for (Attribute<?, ?> attribute : type.getAttributes()) {
            if (attribute instanceof SingularAttribute<?, ?> singular && singular.isVersion()) {
                continue;
            }
            String name = prefix + attribute.getName();
            switch (attribute.getPersistentAttributeType()) {
                case BASIC -> {
                    fields.add(name);
                    types.put(name, attribute.getJavaType());
                }
                case EMBEDDED -> describe(em.getMetamodel().embeddable(attribute.getJavaType()), name + ".",
                        fields, joins, types);
                case ONE_TO_ONE, MANY_TO_ONE -> {
                    joins.add(name);
                    describe(em.getMetamodel().entity(attribute.getJavaType()), name + ".", fields, joins, types);
                }
                default -> {
                }
            }
        }
    }

    private static List<FilterCondition> parseFilters(EntityShape shape, List<String> filter) {
        List<FilterCondition> conditions = new ArrayList<>();
        for (String raw : filter == null ? List.<String>of() : filter) {
            Matcher m = FILTER.matcher(raw);
            if (!m.matches() || !shape.fields().contains(m.group(1))) {
                throw ApiException.badRequest("error.filter.unknown", raw);
            }
            String field = m.group(1);
            String op = m.group(2);
            String text = m.group(3);
            boolean nullLiteral = text.equals("null") && (op.equals("=") || op.equals("!="));
            Comparable<?> value = nullLiteral ? null : op.equals("~") ? text : parse(shape.types().get(field), field, text);
            conditions.add(new FilterCondition(field, op, value, nullLiteral));
        }
        return conditions;
    }

    private static List<SortField> parseSorts(EntityShape shape, List<String> sort) {
        List<SortField> sorts = new ArrayList<>();
        Set<String> sorted = new HashSet<>();
        for (String raw : sort == null ? List.<String>of() : sort) {
            Matcher m = SORT.matcher(raw);
            if (!m.matches() || !shape.fields().contains(m.group(1))) {
                throw ApiException.badRequest("error.sort.value", raw);
            }
            if (!sorted.add(m.group(1))) {
                throw ApiException.badRequest("error.sort.duplicate", m.group(1));
            }
            sorts.add(new SortField(m.group(1), m.group(2).equals("asc")));
        }
        return sorts;
    }

    private static void checkConflicts(List<FilterCondition> conditions) {
        Map<String, List<FilterCondition>> byField = new LinkedHashMap<>();
        for (FilterCondition condition : conditions) {
            byField.computeIfAbsent(condition.field(), f -> new ArrayList<>()).add(condition);
        }
        byField.forEach(PageQuery::checkSatisfiable);
    }

    private static void checkSatisfiable(String field, List<FilterCondition> conditions) {
        boolean isNull = false;
        boolean notNull = false;
        Comparable<?> equal = null;
        Comparable<?> lower = null;
        boolean lowerStrict = false;
        Comparable<?> upper = null;
        boolean upperStrict = false;
        List<Comparable<?>> excluded = new ArrayList<>();
        for (FilterCondition c : conditions) {
            if (c.nullLiteral()) {
                if (c.op().equals("=")) isNull = true; else notNull = true;
                continue;
            }
            notNull = true;
            if (c.op().equals("~")) continue;
            Comparable<?> v = c.value();
            switch (c.op()) {
                case "=" -> {
                    if (equal != null && compare(equal, v) != 0) conflict(field);
                    equal = v;
                }
                case "!=" -> excluded.add(v);
                case ">", ">=" -> {
                    int cmp = lower == null ? 1 : compare(v, lower);
                    if (cmp > 0 || (cmp == 0 && c.op().equals(">"))) { lower = v; lowerStrict = c.op().equals(">"); }
                }
                default -> {
                    int cmp = upper == null ? -1 : compare(v, upper);
                    if (cmp < 0 || (cmp == 0 && c.op().equals("<"))) { upper = v; upperStrict = c.op().equals("<"); }
                }
            }
        }
        if (isNull && notNull) conflict(field);
        if (lower != null && upper != null) {
            int cmp = compare(lower, upper);
            if (cmp > 0 || (cmp == 0 && (lowerStrict || upperStrict))) conflict(field);
            if (cmp == 0) {
                Comparable<?> pinned = lower;
                if (excluded.stream().anyMatch(x -> compare(x, pinned) == 0)) conflict(field);
            }
        }
        if (equal != null) {
            Comparable<?> eq = equal;
            if (excluded.stream().anyMatch(x -> compare(x, eq) == 0)) conflict(field);
            if (lower != null && (lowerStrict ? compare(eq, lower) <= 0 : compare(eq, lower) < 0)) conflict(field);
            if (upper != null && (upperStrict ? compare(eq, upper) >= 0 : compare(eq, upper) > 0)) conflict(field);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(Comparable<?> a, Comparable<?> b) {
        if (a instanceof Enum<?> ea && b instanceof Enum<?> eb) {
            return ea.name().compareTo(eb.name());
        }
        if (a instanceof OffsetDateTime da && b instanceof OffsetDateTime db) {
            return da.toInstant().compareTo(db.toInstant());
        }
        return ((Comparable) a).compareTo(b);
    }

    private static void conflict(String field) {
        throw ApiException.badRequest("error.filter.conflict", field);
    }

    private static Predicate[] predicates(CriteriaBuilder cb, Root<?> root, Map<String, From<?, ?>> joined,
            EntityShape shape, List<FilterCondition> conditions) {
        List<Predicate> result = new ArrayList<>();
        for (FilterCondition condition : conditions) {
            result.add(predicate(cb, path(root, joined, shape.joins(), condition.field()), condition));
        }
        return result.toArray(Predicate[]::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate predicate(CriteriaBuilder cb, Path<?> path, FilterCondition condition) {
        if (condition.op().equals("~")) {
            String pattern = "%" + condition.value().toString().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            // Hibernate registers str() as a cast-to-string function; as(String.class) emits no SQL cast
            Expression<String> text = path.getJavaType() == String.class
                    ? (Expression<String>) path
                    : cb.function("str", String.class, path);
            return cb.like(cb.lower(text), pattern, '\\');
        }
        if (condition.nullLiteral()) {
            return condition.op().equals("=") ? cb.isNull(path) : cb.isNotNull(path);
        }
        Comparable value = condition.value();
        Expression<Comparable> expr = (Expression<Comparable>) path;
        return switch (condition.op()) {
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
            EntityShape shape, List<SortField> sorts) {
        List<Order> result = new ArrayList<>();
        for (SortField sort : sorts) {
            Path<?> path = path(root, joined, shape.joins(), sort.field());
            result.add(sort.ascending() ? cb.asc(path) : cb.desc(path));
        }
        result.add(cb.asc(root.get("id")));
        return result;
    }

    private static Map<String, From<?, ?>> fetchJoins(Root<?> root, Set<String> joins) {
        Map<String, From<?, ?>> joined = new HashMap<>();
        for (String name : joins) {
            StringBuilder prefix = new StringBuilder();
            From<?, ?> parent = root;
            for (String part : name.split("\\.")) {
                prefix.append(part);
                From<?, ?> current = joined.get(prefix.toString());
                if (current == null) {
                    // a fetch join implements Join and can seed predicate/order paths
                    current = (From<?, ?>) parent.fetch(part, JoinType.LEFT);
                    joined.put(prefix.toString(), current);
                }
                parent = current;
                prefix.append('.');
            }
        }
        return joined;
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
