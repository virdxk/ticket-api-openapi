const NUMBER_RE = /^\s*[+-]?(0|[1-9]\d*)(\.\d+)?([eE][+-]?\d+)?\s*$/;
const INTEGER_RE = /^[+-]?\d+$/;
const INT64_MIN = -(2n ** 63n);
const INT64_MAX = 2n ** 63n - 1n;

export function rawValue(field, values) {
    return values[field.name] ?? '';
}

export function todayText() {
    const now = new Date();
    return `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}-${`${now.getDate()}`.padStart(2, '0')}`;
}

function calendarDateError(raw) {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(raw);
    if (!match) {
        return 'Формат даты: ГГГГ-ММ-ДД';
    }
    const [, year, month, day] = match.map(Number);
    const parsed = new Date(0);
    parsed.setUTCFullYear(year, month - 1, day);
    parsed.setUTCHours(0, 0, 0, 0);
    if (parsed.getUTCFullYear() !== year || parsed.getUTCMonth() !== month - 1 || parsed.getUTCDate() !== day) {
        return 'Такой даты не существует';
    }
    return null;
}

export function valueError(field, raw, required = false) {
    if (raw === '') {
        return required ? 'Заполните поле' : null;
    }
    if (raw === 'null' && !field.required) {
        return null;
    }
    if (field.values && !field.values.some((option) => option.value === raw)) {
        return 'Недопустимое значение';
    }
    if (field.number) {
        if (!NUMBER_RE.test(raw)) {
            return 'Введите число';
        }
        const text = raw.trim();
        if (field.integer) {
            if (!INTEGER_RE.test(text)) {
                return 'Введите целое число';
            }
            const value = BigInt(text);
            if (value < INT64_MIN || value > INT64_MAX) {
                return 'Значение вне диапазона int64';
            }
            if (field.minimum !== undefined && value < BigInt(field.minimum)) {
                return `Значение должно быть не меньше ${field.minimum}`;
            }
            if (field.maximum !== undefined && value > BigInt(field.maximum)) {
                return `Значение должно быть не больше ${field.maximum}`;
            }
        } else {
            const value = Number(text);
            if (!Number.isFinite(value)) {
                return 'Введите конечное число';
            }
            if (field.minimum !== undefined && value < field.minimum) {
                return `Значение должно быть не меньше ${field.minimum}`;
            }
            if (field.exclusiveMinimum !== undefined && value <= field.exclusiveMinimum) {
                return `Значение должно быть больше ${field.exclusiveMinimum}`;
            }
            if (field.maximum !== undefined && value > field.maximum) {
                return `Значение должно быть не больше ${field.maximum}`;
            }
        }
    }
    const length = Array.from(raw).length;
    if (field.minLength !== undefined && length < field.minLength) {
        return `Минимальная длина: ${field.minLength}`;
    }
    if (field.maxLength !== undefined && length > field.maxLength) {
        return `Максимальная длина: ${field.maxLength}`;
    }
    if (field.format === 'date' || field.format === 'datetime') {
        const datePart = raw.slice(0, 10);
        const dateError = calendarDateError(datePart);
        if (dateError) {
            return dateError;
        }
        if (field.format === 'datetime' && raw.includes('T')) {
            const valid = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?(?:Z|[+-]\d{2}:\d{2})$/.test(raw)
                && !Number.isNaN(Date.parse(raw));
            if (!valid) {
                return 'Некорректные дата и время';
            }
        } else if (field.format === 'datetime' && raw.length !== 10) {
            return 'Формат: ГГГГ-ММ-ДД или дата и время ISO';
        }
        if (field.notAfterToday && raw > todayText()) {
            return 'Дата не может быть в будущем';
        }
    }
    return null;
}

function hasActiveChildren(fields, field, values) {
    return fields.some((candidate) => candidate.name.startsWith(`${field.name}.`) && rawValue(candidate, values) !== '');
}

function parentField(fields, field) {
    const dot = field.name.indexOf('.');
    return dot < 0 ? null : fields.find((candidate) => candidate.name === field.name.slice(0, dot)) ?? null;
}

export function fieldDisabled(fields, field, values) {
    if (field.presence) {
        return false;
    }
    if (hasActiveChildren(fields, field, values)) {
        return true;
    }
    const parent = parentField(fields, field);
    if (!parent) {
        return false;
    }
    return parent.presence ? rawValue(parent, values) !== 'edit' : rawValue(parent, values) !== '';
}

export function groupTouched(fields, field, values, touched) {
    const dot = field.name.indexOf('.');
    if (dot < 0) {
        return false;
    }
    const prefix = `${field.name.slice(0, dot)}.`;
    if (fields.some((candidate) => candidate.name.startsWith(prefix) && touched[candidate.name])) {
        return true;
    }
    const parent = parentField(fields, field);
    return !!parent?.presence && touched[parent.name] && rawValue(parent, values) === 'edit';
}

export function bodyFieldBlocked(fields, field, values, touched) {
    if (field.presence) {
        return rawValue(field, values) === 'edit' || hasActiveChildren(fields, field, values);
    }
    if (hasActiveChildren(fields, field, values)) {
        return true;
    }
    const parent = parentField(fields, field);
    if (!parent) {
        return false;
    }
    if (parent.presence && rawValue(parent, values) !== 'edit') {
        return true;
    }
    if (!parent.presence && rawValue(parent, values) !== '') {
        return true;
    }
    return !groupTouched(fields, field, values, touched) && !hasActiveChildren(fields, parent, values);
}

function included(field, fields, values, touched, creating) {
    return creating || touched[field.name] || groupTouched(fields, field, values, touched);
}

export function bodyErrors(fields, values, { creating = false, touched = {} } = {}) {
    return fields.flatMap((field) => {
        if (bodyFieldBlocked(fields, field, values, touched) || !included(field, fields, values, touched, creating)) {
            return [];
        }
        const error = valueError(field, rawValue(field, values), !!field.required);
        return error ? [`Поле «${field.label}»: ${error}`] : [];
    });
}

export function buildBody(fields, values, { creating = false, touched = {} } = {}) {
    const parts = [];
    const groups = new Map();
    for (const field of fields) {
        if (bodyFieldBlocked(fields, field, values, touched) || !included(field, fields, values, touched, creating)) {
            continue;
        }
        const raw = rawValue(field, values);
        if (field.required && raw === '') {
            continue;
        }
        const text = raw === '' || raw === 'null' ? 'null' : field.number ? numberText(raw) : JSON.stringify(raw);
        const dot = field.name.indexOf('.');
        if (dot < 0) {
            parts.push(`${JSON.stringify(field.name)}:${text}`);
            continue;
        }
        const name = field.name.slice(0, dot);
        let group = groups.get(name);
        if (!group) {
            group = [];
            groups.set(name, group);
            parts.push({ name, group });
        }
        group.push(`${JSON.stringify(field.name.slice(dot + 1))}:${text}`);
    }
    return `{${parts
        .map((part) => (typeof part === 'string' ? part : `${JSON.stringify(part.name)}:{${part.group.join(',')}}`))
        .join(',')}}`;
}

function numberText(raw) {
    const digits = raw.trim();
    return digits.startsWith('+') ? digits.slice(1) : digits;
}
