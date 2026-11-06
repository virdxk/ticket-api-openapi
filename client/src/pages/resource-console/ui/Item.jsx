import { Button, Input, Select } from '../../../shared/ui/index.js';
import { useId, useState } from 'react';
import { resourceApi } from '../api/resource-api.js';
import { bodyErrors, bodyFieldBlocked, buildBody, fieldState, valueError } from '../model/form-body.js';
import { Result } from './Result.jsx';

const ID_FIELD = { number: true, integer: true, minimum: 1 };

export function Item({ cfg, onChanged }) {
    const [id, setId] = useState('');
    const [idAttempted, setIdAttempted] = useState(false);
    const [values, setValues] = useState({});
    const [touched, setTouched] = useState({});
    const [createAttempted, setCreateAttempted] = useState(false);
    const [result, setResult] = useState(null);
    const [pending, setPending] = useState(null);
    const idPrefix = useId();

    async function run(title, key, makeCall) {
        setPending(key);
        try {
            const response = await makeCall();
            setResult({ ...response, title });
            if (response.ok && key !== 'get') {
                onChanged();
            }
        } finally {
            setPending(null);
        }
    }

    function withId(action) {
        setIdAttempted(true);
        const clean = id.trim();
        const error = valueError(ID_FIELD, clean, true);
        if (error) {
            setResult({ ok: false, status: 0, data: { message: `Номер: ${error}` } });
            return;
        }
        action(`${cfg.url}/${clean}`);
    }

    function validBody(creating) {
        const errors = bodyErrors(cfg.fields, values, { creating, touched });
        if (errors.length > 0) {
            setResult({ ok: false, status: 0, data: { message: errors.join('; ') } });
            return null;
        }
        return buildBody(cfg.fields, values, { creating, touched });
    }

    function get() {
        withId((url) => run('Найден', 'get', () => resourceApi.get(url)));
    }

    function remove() {
        withId((url) => run('Удалён', 'remove', () => resourceApi.remove(url)));
    }

    function update() {
        const body = validBody(false);
        if (body === null) {
            return;
        }
        withId((url) =>
            run('Изменён', 'update', () => resourceApi.update(url, body)),
        );
    }

    function create() {
        setCreateAttempted(true);
        const body = validBody(true);
        if (body === null) {
            return;
        }
        run('Создан', 'create', () => resourceApi.create(cfg.url, body));
    }

    function setValue(name, value) {
        setValues({ ...values, [name]: value });
        setTouched({ ...touched, [name]: true });
    }

    const parents = cfg.fields.filter((f) => cfg.fields.some((x) => x.name.startsWith(`${f.name}.`)));
    const isActive = (f) => fieldState(f, values).raw !== '';
    const isBlocked = (f) => {
        const dot = f.name.indexOf('.');
        if (dot > 0) {
            const parent = parents.find((x) => x.name === f.name.slice(0, dot));
            return !!parent && isActive(parent);
        }
        return parents.includes(f) && cfg.fields.some((x) => x.name.startsWith(`${f.name}.`) && isActive(x));
    };
    const idError = valueError(ID_FIELD, id, idAttempted);

    return (
        <section className="my-4 min-w-0 space-y-3 rounded-2xl border border-line bg-white p-4 shadow-sm sm:p-5">
            <h2 className="text-base font-semibold tracking-[-0.015em] text-ink">Элемент</h2>
            <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-2 sm:grid-cols-[15rem_minmax(0,24rem)_auto]">
                <label className="pt-2 text-[13px] text-ink" htmlFor={`${idPrefix}-id`}>Номер</label>
                <span className="min-w-0">
                    <Input
                        className="w-full"
                        id={`${idPrefix}-id`}
                        required
                        invalid={!!idError}
                        aria-describedby={idError ? `${idPrefix}-id-error` : undefined}
                        placeholder="Целое число от 1"
                        value={id}
                        onChange={(e) => setId(e.target.value)}
                    />
                    {idError && <span id={`${idPrefix}-id-error`} className="mt-1 block text-[13px] leading-5 text-error">{idError}</span>}
                </span>
                <span className="flex flex-wrap gap-2">
                    <Button disabled={pending !== null} onClick={get}>Получить</Button>
                    <Button variant="danger" disabled={pending !== null} onClick={remove}>Удалить</Button>
                    <Button disabled={pending !== null} onClick={update}>Изменить</Button>
                </span>
            </div>
            {cfg.fields.map((f) => {
                const { raw } = fieldState(f, values);
                const disabled = isBlocked(f);
                const blocked = disabled || bodyFieldBlocked(cfg.fields, f, values, touched);
                const required = f.required && !blocked && (createAttempted || touched[f.name]);
                const error = !blocked && (raw !== '' || required) ? valueError(f, raw, required) : null;
                return (
                    <div key={f.name} className="grid grid-cols-[minmax(0,1fr)] items-start gap-2 sm:grid-cols-[15rem_minmax(0,24rem)]">
                        <label className="pt-2 text-[13px] text-ink" htmlFor={`${idPrefix}-${f.name}`}>{f.label}</label>
                        <span className="min-w-0">
                            {f.values ? (
                                <Select
                                    className="w-full"
                                    id={`${idPrefix}-${f.name}`}
                                    disabled={disabled}
                                    required={f.required}
                                    invalid={!!error}
                                    aria-describedby={error ? `${idPrefix}-${f.name}-error` : undefined}
                                    value={raw}
                                    onValueChange={(value) => setValue(f.name, value)}
                                    options={[
                                        { value: '', label: '-' },
                                        ...f.values.map((value) => ({ value, label: value })),
                                    ]}
                                />
                            ) : (
                                <Input
                                    className="w-full"
                                    id={`${idPrefix}-${f.name}`}
                                    disabled={disabled}
                                    required={f.required}
                                    invalid={!!error}
                                    aria-describedby={error ? `${idPrefix}-${f.name}-error` : undefined}
                                    placeholder={f.placeholder}
                                    value={raw}
                                    onChange={(e) => setValue(f.name, e.target.value)}
                                />
                            )}
                            {error && <span id={`${idPrefix}-${f.name}-error`} className="mt-1 block text-[13px] leading-5 text-error">{error}</span>}
                        </span>
                    </div>
                );
            })}
            <p className="text-[13px] leading-5 text-muted">
                Обязательные поля нужно заполнить. При создании пустое необязательное поле — null; при изменении неизменённые поля не отправляются, очищенное необязательное — null.
            </p>
            <Button variant="primary" disabled={pending !== null} onClick={create}>Создать</Button>
            <Result result={result} />
        </section>
    );
}
