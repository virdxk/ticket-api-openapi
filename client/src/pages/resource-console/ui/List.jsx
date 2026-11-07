import { Button, Input, Select, Pagination, RemoveButton } from '../../../shared/ui/index.js';
import { useId, useState } from 'react';
import { useResourceList } from '../model/use-resource-list.js';
import { todayText, valueError } from '../model/form-body.js';
import { Result } from './Result.jsx';

const OPS = ['=', '!=', '>', '>=', '<', '<=', '~'];
const SIZE_FIELD = { number: true, integer: true, minimum: 1, maximum: 100 };

function filterError(field, filter, attempted) {
    if (filter.value === 'null') {
        return filter.op === '=' || filter.op === '!=' ? null : 'null допускается только с = или !=';
    }
    return valueError(field, filter.value, attempted || filter.value !== '');
}

export function List({ cfg, reloadKey }) {
    const { filters, setFilters, sorts, setSorts, size, setSize, result, load } = useResourceList(cfg.url, reloadKey);
    const [attempted, setAttempted] = useState(false);
    const idPrefix = useId();

    function update(list, setList, i, key, value) {
        setList(list.map((x, j) => (j === i ? { ...x, [key]: value } : x)));
    }

    function show() {
        setAttempted(true);
        const invalidSize = valueError(SIZE_FIELD, String(size), true);
        const invalidFilter = filters.some((filter) => {
            const field = cfg.filterFields.find((candidate) => candidate.value === filter.field);
            return filterError(field, filter, true);
        });
        if (!invalidSize && !invalidFilter) {
            load(1);
        }
    }

    function addFilter() {
        setFilters([...filters, { field: 'id', op: '=', value: '' }]);
    }

    function removeFilter(i) {
        setFilters(filters.filter((_, j) => j !== i));
    }

    function addSort() {
        const field = cfg.filterFields.find((candidate) => !sorts.some((sort) => sort.field === candidate.value));
        if (field) {
            setSorts([...sorts, { field: field.value, dir: 'asc' }]);
        }
    }

    function removeSort(i) {
        setSorts(sorts.filter((_, j) => j !== i));
    }

    function sortOptions(current) {
        return cfg.filterFields.filter((field) => field.value === current || !sorts.some((sort) => sort.field === field.value));
    }

    const sizeError = valueError(SIZE_FIELD, String(size), attempted);
    const data = result?.ok ? result.data : null;

    return (
        <section className="my-4 min-w-0 space-y-3 rounded-2xl border border-line bg-white p-4 shadow-sm sm:p-5">
            <h2 className="text-base font-semibold tracking-[-0.015em] text-ink">Список</h2>
            <h3 className="text-[13px] font-medium text-muted">Фильтры (все условия через И)</h3>
            {filters.map((f, i) => {
                const field = cfg.filterFields.find((candidate) => candidate.value === f.field);
                const error = filterError(field, f, attempted);
                return (
                    <div key={i} className="grid grid-cols-[minmax(0,1fr)_5rem] items-start gap-3 sm:grid-cols-[minmax(0,1fr)_8rem_minmax(0,1fr)_2.25rem]">
                        <span className="flex min-w-0 flex-col gap-1.5">
                            <label className="shrink-0 text-[13px] text-ink" htmlFor={`${idPrefix}-filter-field-${i}`}>Поле</label>
                            <Select
                                className="w-full"
                                id={`${idPrefix}-filter-field-${i}`}
                                value={f.field}
                                onValueChange={(value) => update(filters, setFilters, i, 'field', value)}
                                options={cfg.filterFields}
                            />
                        </span>
                        <span className="flex min-w-0 flex-col gap-1.5">
                            <label className="shrink-0 text-[13px] text-ink" htmlFor={`${idPrefix}-filter-op-${i}`}>Условие</label>
                            <Select
                                className="w-full"
                                id={`${idPrefix}-filter-op-${i}`}
                                value={f.op}
                                onValueChange={(value) => update(filters, setFilters, i, 'op', value)}
                                options={OPS.map((op) => ({ value: op, label: op }))}
                            />
                        </span>
                        <span className="flex min-w-0 flex-col gap-1.5">
                            <label className="shrink-0 text-[13px] text-ink" htmlFor={`${idPrefix}-filter-value-${i}`}>Значение</label>
                            <Input
                                className="w-full"
                                id={`${idPrefix}-filter-value-${i}`}
                                type={field.format === 'date' ? 'date' : 'text'}
                                required
                                invalid={!!error}
                                aria-describedby={error ? `${idPrefix}-filter-value-${i}-error` : undefined}
                                max={field.notAfterToday ? todayText() : undefined}
                                value={f.value}
                                placeholder={field.format === 'datetime' ? 'Дата или дата и время ISO' : 'значение или null'}
                                onChange={(e) => update(filters, setFilters, i, 'value', e.target.value)}
                            />
                            {error && <span id={`${idPrefix}-filter-value-${i}-error`} className="block text-[13px] leading-5 text-error">{error}</span>}
                        </span>
                        <RemoveButton className="col-start-2 row-start-2 mt-[26px] sm:col-start-auto sm:row-start-auto" label={`Удалить фильтр ${i + 1}`} onClick={() => removeFilter(i)} />
                    </div>
                );
            })}
            <Button onClick={addFilter}>+ фильтр</Button>
            <h3 className="text-[13px] font-medium text-muted">Сортировка (по порядку приоритета)</h3>
            {sorts.map((s, i) => (
                <div key={i} className="grid grid-cols-[minmax(0,1fr)_2.25rem] items-start gap-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_2.25rem]">
                    <span className="col-span-2 flex min-w-0 flex-col gap-1.5 sm:col-span-1">
                        <label className="shrink-0 text-[13px] text-ink" htmlFor={`${idPrefix}-sort-field-${i}`}>Поле</label>
                        <Select
                            className="w-full"
                            id={`${idPrefix}-sort-field-${i}`}
                            value={s.field}
                            onValueChange={(value) => update(sorts, setSorts, i, 'field', value)}
                            options={sortOptions(s.field)}
                        />
                    </span>
                    <span className="flex min-w-0 flex-col gap-1.5">
                        <label className="shrink-0 text-[13px] text-ink" htmlFor={`${idPrefix}-sort-dir-${i}`}>Порядок</label>
                        <Select
                            className="w-full"
                            id={`${idPrefix}-sort-dir-${i}`}
                            value={s.dir}
                            onValueChange={(value) => update(sorts, setSorts, i, 'dir', value)}
                            options={[
                                { value: 'asc', label: 'по возрастанию' },
                                { value: 'desc', label: 'по убыванию' },
                            ]}
                        />
                    </span>
                    <RemoveButton className="mt-[26px]" label={`Удалить сортировку ${i + 1}`} onClick={() => removeSort(i)} />
                </div>
            ))}
            <Button disabled={sorts.length >= cfg.filterFields.length} onClick={addSort}>+ сортировка</Button>
            <p className="flex flex-wrap items-start gap-2">
                <span className="flex min-w-0 max-w-full items-start gap-2">
                    <label className="shrink-0 pt-2 text-[13px] text-ink" htmlFor={`${idPrefix}-size`}>Размер страницы</label>
                    <span className="min-w-0">
                        <Input
                            className="w-20"
                            id={`${idPrefix}-size`}
                            required
                            invalid={!!sizeError}
                            aria-describedby={sizeError ? `${idPrefix}-size-error` : undefined}
                            placeholder="1–100"
                            value={size}
                            onChange={(e) => setSize(e.target.value)}
                        />
                        {sizeError && <span id={`${idPrefix}-size-error`} className="mt-1 block w-28 text-[13px] leading-5 text-error">{sizeError}</span>}
                    </span>
                </span>
                <Button variant="primary" onClick={show}>Показать</Button>
            </p>
            {result && !result.ok && <Result result={result} />}
            {data && (
                <>
                    {data.items.length === 0 ? (
                        <p className="rounded-xl border border-line bg-canvas p-4 text-[13px] text-muted">Ничего не найдено. Измените фильтры или параметры запроса.</p>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full border-collapse text-left text-[13px]">
                                <thead>
                                    <tr>
                                        {cfg.columns.map(([title]) => (
                                            <th className="border-b border-line bg-canvas px-3 py-2 font-medium text-muted" key={title}>{title}</th>
                                        ))}
                                    </tr>
                                </thead>
                                <tbody>
                                    {data.items.map((item) => (
                                        <tr key={item.id}>
                                            {cfg.columns.map(([title, get]) => (
                                                <td className="border-b border-line px-3 py-2 align-top wrap-anywhere" key={title}>{get(item)}</td>
                                            ))}
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                    {data.totalPages > 0 && <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={load} />}
                    <details>
                        <summary className="cursor-pointer text-[13px] font-medium text-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent">Полученный ответ</summary>
                        <pre className="mt-3 overflow-x-auto rounded-lg border border-line bg-canvas p-3 text-xs leading-5 text-ink">{JSON.stringify(data, null, 2)}</pre>
                    </details>
                </>
            )}
        </section>
    );
}
