const NUMBER_RE = /^\s*[+-]?(0|[1-9]\d*)(\.\d+)?([eE][+-]?\d+)?\s*$/;

export function fieldState(field, values) {
    const raw = values[field.name] ?? '';
    const isNull = !field.required && raw === '';
    return { raw, isNull, send: raw !== '' || isNull };
}

export function valueError(field, raw, required = false) {
    if (raw === '') {
        return required ? 'Заполните поле' : null;
    }
    if (field.number) {
        if (!NUMBER_RE.test(raw)) {
            return 'Введите число';
        }
        const text = raw.trim();
        if (field.integer && !/^[+-]?\d+$/.test(text)) {
            return 'Введите целое число';
        }
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
    const length = Array.from(raw).length;
    if (field.minLength !== undefined && length < field.minLength) {
        return `Минимальная длина: ${field.minLength}`;
    }
    if (field.maxLength !== undefined && length > field.maxLength) {
        return `Максимальная длина: ${field.maxLength}`;
    }
    if (field.format === 'date') {
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
        if (field.notAfterToday) {
            const now = new Date();
            const today = `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}-${`${now.getDate()}`.padStart(2, '0')}`;
            if (raw > today) {
                return 'Дата не может быть в будущем';
            }
        }
    }
    return null;
}

function rawValue(field, values) {
    return values[field.name] ?? '';
}

function hasActiveChildren(fields, field, values) {
    return fields.some((candidate) => candidate.name.startsWith(`${field.name}.`) && rawValue(candidate, values) !== '');
}

export function bodyFieldBlocked(fields, field, values, touched) {
    if (hasActiveChildren(fields, field, values)) {
        return true;
    }
    const dot = field.name.indexOf('.');
    if (dot < 0) {
        return false;
    }
    const parent = fields.find((candidate) => candidate.name === field.name.slice(0, dot));
    if (!parent) {
        return false;
    }
    if (rawValue(parent, values) !== '') {
        return true;
    }
    const siblings = fields.filter((candidate) => candidate.name.startsWith(`${field.name.slice(0, dot)}.`));
    return !siblings.some((candidate) => rawValue(candidate, values) !== '') && !touched[field.name];
}

export function bodyErrors(fields, values, { creating = false, touched = {} } = {}) {
    return fields.flatMap((field) => {
        if (bodyFieldBlocked(fields, field, values, touched) || (!creating && !touched[field.name])) {
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
        if (bodyFieldBlocked(fields, field, values, touched) || (!creating && !touched[field.name])) {
            continue;
        }
        const raw = rawValue(field, values);
        if (field.required && raw === '') {
            continue;
        }
        const text = raw === '' ? 'null' : field.number ? numberText(raw) : JSON.stringify(raw);
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
