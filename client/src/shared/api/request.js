import JSONbig from 'json-bigint';

const json = JSONbig({ storeAsString: true });

export async function request(method, url, body, contentType = 'application/json', signal) {
    try {
        const res = await fetch(url, {
            method,
            signal,
            headers: body === undefined ? {} : { 'Content-Type': contentType },
            body: body === undefined ? undefined : typeof body === 'string' ? body : JSON.stringify(body),
        });
        const text = await res.text();
        let data = null;
        try {
            data = text ? json.parse(text) : null;
        } catch {
            data = text;
        }
        return { ok: res.ok, status: res.status, data };
    } catch (e) {
        if (e.name === 'AbortError') {
            return { ok: false, status: 0, data: null, aborted: true };
        }
        return { ok: false, status: 0, data: { message: `Сервер недоступен: ${e.message}` } };
    }
}
