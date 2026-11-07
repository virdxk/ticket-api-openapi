import { fieldLabel, show } from '../lib/format.js';

const STATUS = {
    400: 'некорректный запрос',
    404: 'не найдено',
    409: 'конфликт',
    422: 'невалидные данные',
    500: 'внутренняя ошибка сервера',
    503: 'сервис недоступен',
};

export function Result({ result }) {
    if (!result) {
        return null;
    }
    const { ok, status, data, title } = result;
    return (
        <div className="space-y-3 rounded-xl border border-line bg-canvas p-3 text-ink">
            {ok ? (
                <b>{title}</b>
            ) : (
                <b>{status ? `Ошибка ${status}: ${STATUS[status] || 'ошибка запроса'}` : 'Ошибка запроса'}</b>
            )}
            {!ok && data?.message && <p className="flex flex-wrap items-center gap-2">{data.message}</p>}
            {!ok && data?.details?.length > 0 && (
                <div className="overflow-x-auto">
                    <table className="w-full border-collapse text-left text-[13px]">
                        <thead>
                            <tr>
                                <th className="border-b border-line bg-canvas px-3 py-2 font-medium text-muted">Поле</th>
                                <th className="border-b border-line bg-canvas px-3 py-2 font-medium text-muted">Ошибка</th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.details.map((d, i) => (
                                <tr key={i}>
                                    <td className="border-b border-line px-3 py-2 align-top wrap-anywhere">{fieldLabel(d.field)}</td>
                                    <td className="border-b border-line px-3 py-2 align-top wrap-anywhere">{d.message}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            {ok && data && typeof data === 'object' && (
                <div className="overflow-x-auto">
                    <table className="w-full border-collapse text-left text-[13px]">
                        <tbody>
                            {Object.entries(data).map(([k, v]) => (
                                <tr key={k}>
                                    <td className="border-b border-line px-3 py-2 align-top wrap-anywhere">{fieldLabel(k)}</td>
                                    <td className="border-b border-line px-3 py-2 align-top wrap-anywhere">{v !== null && typeof v === 'object' ? JSON.stringify(v) : show(v)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            {data !== null && (
                <details>
                    <summary className="cursor-pointer text-[13px] font-medium text-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent">Полученный ответ</summary>
                    <pre className="mt-3 overflow-x-auto rounded-lg border border-line bg-canvas p-3 text-xs leading-5 text-ink">{JSON.stringify(data, null, 2)}</pre>
                </details>
            )}
        </div>
    );
}
