export async function request(method, url, body, contentType = 'application/json') {
    try {
        const res = await fetch(url, {
            method,
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
        return { ok: false, status: 0, data: { message: `Сервер недоступен: ${e.message}` } };
    }
}
