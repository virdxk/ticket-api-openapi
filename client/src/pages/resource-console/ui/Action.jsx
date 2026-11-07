import { Button, Input, Select } from '../../../shared/ui/index.js';
import { useId, useState } from 'react';
import { resourceApi } from '../api/resource-api.js';
import { valueError } from '../model/form-body.js';
import { Result } from './Result.jsx';

export function Action({ action, onChanged }) {
    const [values, setValues] = useState(action.inputs.map((i) => (i.values ? i.values[0].value : '')));
    const [submitted, setSubmitted] = useState(false);
    const [result, setResult] = useState(null);
    const [pending, setPending] = useState(false);
    const idPrefix = useId();

    function setValue(i, value) {
        setValues(values.map((v, j) => (j === i ? value : v)));
    }

    async function execute() {
        setSubmitted(true);
        const errors = action.inputs.flatMap((input, i) => {
            const error = valueError(input, values[i], true);
            return error ? [`Поле «${input.label}»: ${error}`] : [];
        });
        if (errors.length > 0) {
            setResult({ ok: false, status: 0, data: { message: errors.join('; ') } });
            return;
        }
        setPending(true);
        try {
            const response = await resourceApi.execute(action, values);
            setResult({ ...response, title: 'Выполнено' });
            if (response.ok) {
                onChanged();
            }
        } finally {
            setPending(false);
        }
    }

    return (
        <section className="my-4 min-w-0 space-y-3 rounded-2xl border border-line bg-white p-4 shadow-sm sm:p-5">
            <h2 className="text-base font-semibold tracking-[-0.015em] text-ink">{action.title}</h2>
            <div className="flex flex-wrap items-start gap-3">
                {action.inputs.map((input, i) => {
                    const set = (e) => setValue(i, e.target.value);
                    const error = input.values ? null : valueError(input, values[i], submitted);
                    return (
                        <span key={i} className="grid w-full min-w-0 grid-cols-[minmax(0,1fr)] gap-2 sm:flex sm:w-auto sm:items-start">
                            <label className="pt-2 text-[13px] text-ink" htmlFor={`${idPrefix}-${i}`}>{input.label}</label>
                            <span className="min-w-0 sm:w-64">
                                {input.values ? (
                                    <Select
                                        className="w-full"
                                        id={`${idPrefix}-${i}`}
                                        value={values[i]}
                                        onValueChange={(value) => setValue(i, value)}
                                        options={input.values}
                                    />
                                ) : (
                                    <Input
                                        className="w-full"
                                        id={`${idPrefix}-${i}`}
                                        invalid={!!error}
                                        aria-describedby={error ? `${idPrefix}-${i}-error` : undefined}
                                        placeholder={input.placeholder}
                                        value={values[i]}
                                        onChange={set}
                                    />
                                )}
                                {error && <span id={`${idPrefix}-${i}-error`} className="mt-1 block text-[13px] leading-5 text-error">{error}</span>}
                            </span>
                        </span>
                    );
                })}
                <Button variant="primary" disabled={pending} onClick={execute}>Выполнить</Button>
            </div>
            <Result result={result} />
        </section>
    );
}
