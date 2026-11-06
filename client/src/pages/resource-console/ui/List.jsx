import { Button, Input, Select, Pagination } from '../../../shared/ui/index.js';
import { useId } from 'react';
import { useResourceList } from '../model/use-resource-list.js';
import { Result } from './Result.jsx';

const OPS = ['=', '!=', '>', '>=', '<', '<=', '~'];

export function List({ cfg, reloadKey }) {
    const { filters, setFilters, sorts, setSorts, size, setSize, result, load } = useResourceList(cfg.url, reloadKey);
    const idPrefix = useId();

    function update(list, setList, i, key, value) {
        setList(list.map((x, j) => (j === i ? { ...x, [key]: value } : x)));
    }

    function show() {
        load(1);
    }

    function addFilter() {
        setFilters([...filters, { field: 'id', op: '=', value: '' }]);
    }

    function removeFilter(i) {
        setFilters(filters.filter((_, j) => j !== i));
    }

    function addSort() {
        setSorts([...sorts, { field: 'id', dir: 'asc' }]);
    }

    function removeSort(i) {
        setSorts(sorts.filter((_, j) => j !== i));
    }

    const fieldSelect = (id, value, onChange) => (
        <Select className="w-full" id={id} value={value} onValueChange={onChange} options={cfg.filterFields} />
    );
    const data = result?.ok ? result.data : null;

    return (
        <section className="my-4 min-w-0 space-y-3 rounded-2xl border border-line bg-white p-4 shadow-sm sm:p-5">
            <h2 className="text-base font-semibold tracking-[-0.015em] text-ink">Список</h2>
            <h3 className="text-[13px] font-medium text-muted">Фильтры (все условия через И)</h3>
            {filters.map((f, i) => (
                <div key={i} className="grid grid-cols-[minmax(0,1fr)_5rem] items-end gap-3 sm:grid-cols-[minmax(0,1fr)_8rem_minmax(0,1fr)_2.25rem]">
                    <span className="flex min-w-0 flex-col gap-1.5">
                        <label className="shrink-0 text-[13px] text-ink" htmlFor={`${idPrefix}-filter-field-${i}`}>Поле </label>
                        {fieldSelect(`${idPrefix}-filter-field-${i}`, f.field, (v) =>
                            update(filters, setFilters, i, 'field', v),
                        )}
                    </span>
                    <span className="flex min-w-0 flex-col gap-1.5">
                        <label className="shrink-0 text-[13px] text-ink" htmlFor={`${idPrefix}-filter-op-${i}`}>Условие </label>
                        <Select
                            className="w-full"
                            id={`${idPrefix}-filter-op-${i}`}
                            value={f.op}
                            onValueChange={(value) => update(filters, setFilters, i, 'op', value)}
                            options={OPS.map((op) => ({ value: op, label: op }))}
                        />
                    </span>
                    <span className="flex min-w-0 flex-col gap-1.5">
                        <label className="shrink-0 text-[13px] text-ink" htmlFor={`${idPrefix}-filter-value-${i}`}>Значение </label>
                        <Input
                            className="w-full"
                            id={`${idPrefix}-filter-value-${i}`}
                            value={f.value}
                            placeholder="значение или null"
                            onChange={(e) => update(filters, setFilters, i, 'value', e.target.value)}
                        />
                    </span>
                    <button
                        type="button"
                        aria-label={`Удалить фильтр ${i + 1}`}
                        title={`Удалить фильтр ${i + 1}`}
                        className="col-start-2 row-start-2 inline-flex size-9 items-center justify-center justify-self-end rounded-lg border border-line bg-white text-muted hover:bg-canvas hover:text-ink active:bg-black/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent sm:col-start-auto sm:row-start-auto"
                        onClick={() => removeFilter(i)}
                    >
                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M3 6h18M9 6V4h6v2M5 6l1 14h12l1-14M10 10v6M14 10v6" />
                        </svg>
                    </button>
                </div>
            ))}
            <Button onClick={addFilter}>+ фильтр</Button>
            <h3 className="text-[13px] font-medium text-muted">Сортировка (по порядку приоритета)</h3>
            {sorts.map((s, i) => (
                <div key={i} className="grid grid-cols-[minmax(0,1fr)_2.25rem] items-end gap-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_2.25rem]">
                    <span className="col-span-2 flex min-w-0 flex-col gap-1.5 sm:col-span-1">
                        <label className="shrink-0 text-[13px] text-ink" htmlFor={`${idPrefix}-sort-field-${i}`}>Поле </label>
                        {fieldSelect(`${idPrefix}-sort-field-${i}`, s.field, (v) =>
                            update(sorts, setSorts, i, 'field', v),
                        )}
                    </span>
                    <span className="flex min-w-0 flex-col gap-1.5">
                        <label className="shrink-0 text-[13px] text-ink" htmlFor={`${idPrefix}-sort-dir-${i}`}>Порядок </label>
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
                    <button
                        type="button"
                        aria-label={`Удалить сортировку ${i + 1}`}
                        title={`Удалить сортировку ${i + 1}`}
                        className="inline-flex size-9 items-center justify-center rounded-lg border border-line bg-white text-muted hover:bg-canvas hover:text-ink active:bg-black/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
                        onClick={() => removeSort(i)}
                    >
                        <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M3 6h18M9 6V4h6v2M5 6l1 14h12l1-14M10 10v6M14 10v6" />
                        </svg>
                    </button>
                </div>
            ))}
            <Button onClick={addSort}>+ сортировка</Button>
            <p className="flex flex-wrap items-center gap-2">
                <span className="flex min-w-0 max-w-full items-center gap-2">
                    <label className="shrink-0 text-[13px] text-ink" htmlFor={`${idPrefix}-size`}>Размер страницы </label>
                    <Input className="w-20" id={`${idPrefix}-size`} value={size} onChange={(e) => setSize(e.target.value)} />
                </span>
                <Button variant="primary" onClick={show}>Показать</Button>
            </p>
            {result && !result.ok && <Result result={result} />}
            {data && (
                <>
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
                    <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={load} />
                    <details>
                        <summary className="cursor-pointer text-[13px] font-medium text-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent">Полученный ответ</summary>
                        <pre className="mt-3 overflow-x-auto rounded-lg border border-line bg-canvas p-3 text-xs leading-5 text-ink">{JSON.stringify(data, null, 2)}</pre>
                    </details>
                </>
            )}
        </section>
    );
}
