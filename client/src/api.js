// JSON.parse rounds integers above 2^53, so displayed huge ids can differ from the server's; request bodies keep exact digits.
export async function request(method, url, body, contentType = 'application/json', signal) {
    try {
        const res = await fetch(url, {
            method,
            signal,
            headers: body === undefined ? {} : { 'Content-Type': contentType },
            body: body === undefined ? undefined : JSON.stringify(body),
        });
        const text = await res.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : null;
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
