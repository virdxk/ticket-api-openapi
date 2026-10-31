export function buildBody(fields, values) {
    const body = {};
    for (const f of fields) {
        const raw = (values[f.name] ?? '').trim();
        if (raw === '') {
            continue;
        }
        const value = raw === 'null' ? null : f.number && !isNaN(raw) ? Number(raw) : raw;
        const path = f.name.split('.');
        let target = body;
        for (const key of path.slice(0, -1)) {
            target = target[key] ??= {};
        }
        target[path.at(-1)] = value;
    }
    return body;
}
