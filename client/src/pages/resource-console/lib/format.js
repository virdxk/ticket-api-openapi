export function show(value) {
    return value === null || value === undefined ? '-' : String(value);
}

export function date(value) {
    return value ? new Date(value).toLocaleString('ru-RU') : '-';
}

export function birthday(value) {
    if (!value) {
        return '-';
    }
    const [y, m, d] = String(value).split('-');
    return `${d}.${m}.${y}`;
}

export function venue(v) {
    if (!v) {
        return '-';
    }
    return `${v.name}, ${v.type}, мест: ${v.capacity} (id ${v.id})`;
}
