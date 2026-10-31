const NUMBER_RE = /^\s*[+-]?(0|[1-9]\d*)(\.\d+)?([eE][+-]?\d+)?\s*$/;

export function fieldState(field, values) {
    const raw = values[field.name] ?? '';
    const isNull = !!values[`${field.name}$null`] || (!!field.values && raw === 'null');
    return { raw, isNull, send: isNull || raw !== '' };
}

export function bodyErrors(fields, values) {
    return fields
        .filter((f) => {
            const { raw, isNull, send } = fieldState(f, values);
            return send && !isNull && f.number && !NUMBER_RE.test(raw);
        })
        .map((f) => `Поле «${f.label}»: некорректное число`);
}

export function buildBody(fields, values) {
    const parts = [];
    const groups = new Map();
    for (const f of fields) {
        const { raw, isNull, send } = fieldState(f, values);
        if (!send) {
            continue;
        }
        const text = isNull ? 'null' : f.number ? numberText(raw) : JSON.stringify(raw);
        const dot = f.name.indexOf('.');
        if (dot < 0) {
            parts.push(`${JSON.stringify(f.name)}:${text}`);
            continue;
        }
        const name = f.name.slice(0, dot);
        let group = groups.get(name);
        if (!group) {
            group = [];
            groups.set(name, group);
            parts.push({ name, group });
        }
        group.push(`${JSON.stringify(f.name.slice(dot + 1))}:${text}`);
    }
    return `{${parts
        .map((p) => (typeof p === 'string' ? p : `${JSON.stringify(p.name)}:{${p.group.join(',')}}`))
        .join(',')}}`;
}

function numberText(raw) {
    const digits = raw.trim();
    return digits.startsWith('+') ? digits.slice(1) : digits;
}
