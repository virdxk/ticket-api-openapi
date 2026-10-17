import { Fragment, isValidElement, useEffect, useRef, useState } from 'react';

export const dash = () => <span className="null" title="null">-</span>;

export function show(v) {
  if (v === null || v === undefined) return dash();
  if (isValidElement(v)) return v;
  if (v === '') return <span className="muted">(пустая строка)</span>;
  return String(v);
}

export function fmtDateTime(s) {
  if (s == null) return dash();
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return String(s);
  return <time dateTime={s} title={s}>{d.toLocaleString('ru-RU')}</time>;
}

export function fmtDate(s) {
  if (s == null) return dash();
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(s));
  return m ? <time dateTime={s} title={s}>{`${m[3]}.${m[2]}.${m[1]}`}</time> : String(s);
}

export function fmtEnum(code, labels) {
  if (code == null) return dash();
  return labels[code] ? <span>{labels[code]} <code>{code}</code></span> : <code>{String(code)}</code>;
}

export const fmtNumber = n => (n == null ? dash() : typeof n === 'number' ? n.toLocaleString('ru-RU', { maximumFractionDigits: 20 }) : String(n));

export const dl = pairs => (
  <dl className="card">{pairs.map(([k, v], i) => <Fragment key={i}><dt>{k}</dt><dd>{show(v)}</dd></Fragment>)}</dl>
);

export function plural(n, one, few, many) {
  const a = Math.abs(n) % 100, b = a % 10;
  if (a > 10 && a < 20) return many;
  if (b === 1) return one;
  return b >= 2 && b <= 4 ? few : many;
}

const enumOptions = labels => Object.entries(labels).map(([value, ru]) => ({ value, label: `${ru} (${value})` }));

const Options = ({ list }) => list.map(o => <option key={o.value} value={o.value}>{o.label}</option>);

export const Input = ({ placeholder, size, value, set }) => (
  <input type="text" placeholder={placeholder} size={size} autoComplete="off" value={value} onChange={e => set(e.target.value)} />
);

export const Btn = ({ text, onClick, cls }) => <button type="button" className={cls} onClick={onClick}>{text}</button>;

export class ApiError extends Error {
  constructor(kind, req, extra) {
    super(kind);
    Object.assign(this, { kind, request: req }, extra);
  }
}

export async function request(method, url, body, contentType = 'application/json') {
  const req = { method, url, body, contentType: body === undefined ? undefined : contentType };
  const init = { method, headers: { Accept: 'application/json' } };
  if (body !== undefined) {
    init.body = body;
    init.headers['Content-Type'] = contentType;
  }
  let resp;
  try {
    resp = await fetch(url, init);
  } catch (e) {
    throw new ApiError('network', req, { cause: e.message });
  }
  const text = await resp.text();
  let data = null;
  let isJson = true;
  if (text) {
    try { data = JSON.parse(text); } catch { isJson = false; }
  }
  const info = { status: resp.status, statusText: resp.statusText, data, text, isJson };
  if (!resp.ok) throw new ApiError('http', req, info);
  if (!isJson) throw new ApiError('body', req, info);
  return { ...info, location: resp.headers.get('Location'), request: req };
}

const STATUS_RU = {
  400: 'некорректный запрос', 404: 'не найдено', 405: 'метод не поддерживается', 409: 'конфликт',
  415: 'неподдерживаемый тип содержимого', 422: 'невалидные данные', 500: 'внутренняя ошибка',
  502: 'ошибка шлюза', 503: 'сервис недоступен', 504: 'сервис не ответил вовремя',
};

const describe = s => STATUS_RU[s] || (s >= 500 ? 'ошибка сервера' : 'ошибка запроса');

function RequestDetails({ req }) {
  if (!req) return null;
  let text = `${req.method} ${req.url}`;
  if (req.body !== undefined) text += `\nContent-Type: ${req.contentType}\n\n${req.body}`;
  return <details className="req"><summary>Отправленный запрос</summary><pre>{text}</pre></details>;
}

function ResponseDetails({ res }) {
  if (!res || res.status === undefined) return null;
  let text = `HTTP ${res.status} ${res.statusText || ''}`.trim();
  if (res.location) text += `\nLocation: ${res.location}`;
  text += '\n\n' + (res.isJson && res.data !== null ? JSON.stringify(res.data, null, 2) : res.text || '(пустое тело)');
  return <details className="req"><summary>Полученный ответ</summary><pre>{text}</pre></details>;
}

function ErrorBody({ err }) {
  if (err.kind === 'network') return <><h3>Сервер недоступен: ответ не получен</h3><p>{err.cause}</p></>;
  const d = err.data;
  const title = err.kind === 'body' ? `HTTP ${err.status}: ответ не является корректным JSON` : `HTTP ${err.status}: ${describe(err.status)}`;
  let body;
  if (err.kind === 'http' && err.isJson && d && typeof d === 'object' && 'message' in d) {
    body = <>
      <p className="message">{String(d.message)}</p>
      {d.time && <p className="muted">Время: {fmtDateTime(d.time)}</p>}
      {Array.isArray(d.details) && d.details.length > 0 && <table className="details">
        <thead><tr><th>Поле</th><th>Ошибка</th></tr></thead>
        <tbody>{d.details.map((x, i) => <tr key={i}><td><code>{String(x.field)}</code></td><td>{String(x.message)}</td></tr>)}</tbody>
      </table>}
    </>;
  } else if (err.text) {
    body = <><p>Тело ответа не в формате Error:</p><pre>{err.text.slice(0, 2000)}</pre></>;
  } else {
    body = <p>Тело ответа пустое.</p>;
  }
  return <><h3>{title}</h3>{body}</>;
}

export function renderError(err) {
  if (!(err instanceof ApiError)) {
    console.error(err);
    return <div className="panel error" role="alert"><h3>Ошибка в клиенте</h3><p>{String(err && err.message ? err.message : err)}</p></div>;
  }
  return (
    <div className="panel error" role="alert">
      <ErrorBody err={err} />
      <RequestDetails req={err.request} />
      <ResponseDetails res={err} />
    </div>
  );
}

export const renderSuccess = (title, res, content) => (
  <div className="panel success">
    <h3>{title}</h3>
    {res && res.location && <p>Location: <code>{res.location}</code></p>}
    {content}
    {res && <RequestDetails req={res.request} />}
    <ResponseDetails res={res} />
  </div>
);

const OPS = [
  ['=', 'равно'], ['!=', 'не равно'], ['>', 'больше'], ['>=', 'больше или равно'],
  ['<', 'меньше'], ['<=', 'меньше или равно'], ['~', 'содержит'],
].map(([value, ru]) => ({ value, label: `${value}  ${ru}` }));

const DIRS = [{ value: 'asc', label: 'По возрастанию' }, { value: 'desc', label: 'По убыванию' }];

let seq = 0;
const nextId = () => ++seq;

function valueOptions(f) {
  const options = Object.entries(f.values).map(([value, ru]) => ({ value, label: value === ru ? value : `${value} (${ru})` }));
  return f.nullable ? [...options, { value: 'null', label: 'null (не задано)' }] : options;
}

const defaultValue = f => (f.values ? valueOptions(f)[0].value : '');

function ListView({ cfg, listRef, rowActions }) {
  const fieldOptions = cfg.fields.map(f => ({ value: f.value, label: f.label }));
  const newSort = () => ({ id: nextId(), field: cfg.fields[0].value, dir: 'asc' });
  const [page, setPage] = useState('1');
  const [size, setSize] = useState('20');
  const [sorts, setSorts] = useState(() => [newSort()]);
  const [filters, setFilters] = useState([]);
  const [current, setCurrent] = useState(null);
  const [url, setUrl] = useState('');
  const [out, setOut] = useState(null);
  const [pageInfo, setPageInfo] = useState('');
  const state = useRef();
  state.current = { page, size, sorts, filters };

  const edit = (setList, id, patch) => setList(list => list.map(r => (r.id === id ? { ...r, ...patch } : r)));
  const remove = (setList, id) => setList(list => list.filter(r => r.id !== id));
  const move = (id, delta) => setSorts(list => {
    const i = list.findIndex(r => r.id === id), j = i + delta;
    if (j < 0 || j >= list.length) return list;
    const next = [...list];
    [next[i], next[j]] = [next[j], next[i]];
    return next;
  });

  function buildUrl(q) {
    const p = new URLSearchParams();
    if (q.page.trim() !== '') p.append('page', q.page.trim());
    if (q.size.trim() !== '') p.append('size', q.size.trim());
    for (const r of q.sorts) p.append('sort', `${r.field},${r.dir}`);
    for (const r of q.filters) p.append('filter', `${r.field}${r.op}${r.value}`);
    const qs = p.toString();
    return qs ? `${cfg.basePath}?${qs}` : cfg.basePath;
  }

  async function load(over) {
    const target = buildUrl({ ...state.current, ...over });
    setUrl(`GET ${target}`);
    setOut(<p className="muted">Загрузка...</p>);
    try {
      const res = await request('GET', target);
      const c = res.data;
      setCurrent(c);
      setOut(<>{renderTable(c.items)}<RequestDetails req={res.request} /><ResponseDetails res={res} /></>);
      setPage(String(c.page));
      setPageInfo(`Страница ${c.page} из ${c.totalPages}, всего ${c.totalElements} (по ${c.size} на странице)`);
    } catch (e) {
      setCurrent(null);
      setPageInfo('');
      setOut(renderError(e));
    }
  }
  listRef.current = () => load();

  function renderTable(items) {
    if (!items.length) return <p className="muted">Ничего не найдено.</p>;
    return (
      <div className="table-wrap"><table className="list">
        <thead><tr>{cfg.columns.map(c => <th key={c[0]}>{c[0]}</th>)}<th>Действия</th></tr></thead>
        <tbody>{items.map((item, i) => <tr key={i}>
          {cfg.columns.map(c => <td key={c[0]}>{show(c[1](item))}</td>)}
          <td className="actions">{rowActions(item)}</td>
        </tr>)}</tbody>
      </table></div>
    );
  }

  const go = delta => {
    const next = String((current ? current.page : Number(page) || 1) + delta);
    setPage(next);
    load({ page: next });
  };

  const reset = () => {
    const q = { page: '1', size: '20', sorts: [newSort()], filters: [] };
    setSorts(q.sorts);
    setFilters(q.filters);
    setPage(q.page);
    setSize(q.size);
    load(q);
  };

  return (
    <div>
      <details className="query">
        <summary><strong>Фильтры и сортировка</strong> <span className="muted counter">{`${filters.length} фильтров · ${sorts.length} сортировок`}</span></summary>
        <div className="qblock">
          <h4>Фильтры · все условия через И</h4>
          <div className="rows">{filters.map(r => {
            const f = cfg.fields.find(x => x.value === r.field);
            return (
              <div key={r.id} className="row filter-row">
                <select value={r.field} onChange={e => edit(setFilters, r.id, { field: e.target.value, value: defaultValue(cfg.fields.find(x => x.value === e.target.value)) })}><Options list={fieldOptions} /></select>
                <select value={r.op} onChange={e => edit(setFilters, r.id, { op: e.target.value })}><Options list={OPS} /></select>
                {f.values
                  ? <select value={r.value} onChange={e => edit(setFilters, r.id, { value: e.target.value })}><Options list={valueOptions(f)} /></select>
                  : <Input placeholder={f.placeholder || (f.nullable ? 'Значение или null' : 'Значение')} size={18} value={r.value} set={v => edit(setFilters, r.id, { value: v })} />}
                <Btn text="✕" cls="small" onClick={() => remove(setFilters, r.id)} />
              </div>
            );
          })}</div>
          <Btn text="+ Фильтр" cls="small" onClick={() => setFilters(l => [...l, { id: nextId(), field: cfg.fields[0].value, op: OPS[0].value, value: defaultValue(cfg.fields[0]) }])} />
        </div>
        <div className="qblock">
          <h4>Сортировка · приоритет сверху вниз</h4>
          <div className="rows sorts">{sorts.map(r => (
            <div key={r.id} className="row sort-row">
              <select value={r.field} onChange={e => edit(setSorts, r.id, { field: e.target.value })}><Options list={fieldOptions} /></select>
              <select value={r.dir} onChange={e => edit(setSorts, r.id, { dir: e.target.value })}><Options list={DIRS} /></select>
              <Btn text="↑" cls="small" onClick={() => move(r.id, -1)} />
              <Btn text="↓" cls="small" onClick={() => move(r.id, 1)} />
              <Btn text="✕" cls="small" onClick={() => remove(setSorts, r.id)} />
            </div>
          ))}</div>
          <Btn text="+ Сортировка" cls="small" onClick={() => setSorts(l => [...l, newSort()])} />
        </div>
        <p className="muted hint">Фильтровать и сортировать можно по любому полю. «содержит» ищет подстроку без учёта регистра. Для полей, которые могут быть не заданы, «равно null» и «не равно null» проверяют наличие значения. Даты: ГГГГ-ММ-ДД или ГГГГ-ММ-ДДTчч:мм:ссZ.</p>
        <div className="toolbar">
          <Btn text="Применить" cls="primary" onClick={() => { setPage('1'); load({ page: '1' }); }} />
          <Btn text="Сбросить" onClick={reset} />
        </div>
      </details>
      <div className="toolbar">
        <label>{'Страница '}<Input placeholder="1" size={4} value={page} set={setPage} /></label>
        <label>{'Размер '}<Input placeholder="20" size={4} value={size} set={setSize} /></label>
        <Btn text="Показать" cls="primary" onClick={() => load()} />
      </div>
      <p><code className="url">{url}</code></p>
      <div>{out}</div>
      <div className="pager">
        <button type="button" disabled={!current || current.page <= 1} onClick={() => go(-1)}>← Назад</button>
        <span className="page-info">{pageInfo}</span>
        <button type="button" disabled={!current || current.page >= current.totalPages} onClick={() => go(1)}>Вперёд →</button>
      </div>
    </div>
  );
}

function numOrRaw(s) {
  const t = s.trim();
  if (t === '') return null;
  const n = Number(t);
  return Number.isFinite(n) ? n : s;
}

function readField(f, path, s) {
  if (f.kind === 'enum') return s[path] === '' ? null : s[path];
  if (f.kind === 'group') {
    if (f.nullable && s[path] === 'null') return null;
    return Object.fromEntries(f.children.map(c => [c.key, readField(c, `${path}.${c.key}`, s)]));
  }
  if (f.nullable && s[`${path}?`]) return null;
  return f.kind === 'number' ? numOrRaw(s[path]) : s[path];
}

function writeField(f, path, v, s) {
  if (f.kind === 'enum') s[path] = v == null ? '' : v;
  else if (f.kind === 'group') {
    if (f.nullable) s[path] = v == null ? 'null' : 'obj';
    f.children.forEach(c => writeField(c, `${path}.${c.key}`, v ? v[c.key] : undefined, s));
  } else {
    if (f.nullable) s[`${path}?`] = v === null;
    s[path] = v == null ? '' : String(v);
  }
}

const fillState = (spec, o) => {
  const s = {};
  spec.forEach(f => writeField(f, f.key, o[f.key], s));
  return s;
};

function useForm(spec) {
  const [values, setValues] = useState(() => fillState(spec, {}));
  const [include, setInclude] = useState({});
  return {
    values,
    include,
    set: (path, v) => {
      setValues(s => ({ ...s, [path]: v }));
      setInclude(i => ({ ...i, [path.split(/[.?]/)[0]]: true }));
    },
    setInclude: (key, v) => setInclude(i => ({ ...i, [key]: v })),
    value: patch => Object.fromEntries(spec.filter(f => !patch || include[f.key]).map(f => [f.key, readField(f, f.key, values)])),
    fill: o => { setValues(fillState(spec, o)); setInclude({}); },
  };
}

function FieldRow({ f, include, children }) {
  return (
    <div className="field">
      <div className="label">{include ? <label>{include} {f.label}</label> : <span>{f.label}</span>}{f.hint && <small>{f.hint}</small>}</div>
      {children}
    </div>
  );
}

function Control({ f, path, form }) {
  const s = form.values;
  if (f.kind === 'enum') {
    return <select value={s[path]} onChange={e => form.set(path, e.target.value)}><option value="">(null) не задано</option><Options list={enumOptions(f.labels)} /></select>;
  }
  if (f.kind === 'group') {
    const body = (
      <div className="group" hidden={f.nullable && s[path] === 'null'}>
        {f.children.map(c => <FieldRow key={c.key} f={c}><Control f={c} path={`${path}.${c.key}`} form={form} /></FieldRow>)}
      </div>
    );
    if (!f.nullable) return body;
    return (
      <div>
        <select value={s[path]} onChange={e => form.set(path, e.target.value)}><option value="null">нет (null)</option><option value="obj">указать</option></select>
        {body}
      </div>
    );
  }
  const props = { value: s[path], onChange: e => form.set(path, e.target.value), disabled: f.nullable && s[`${path}?`] };
  const el = f.kind === 'textarea' ? <textarea rows={2} {...props} /> : <input type="text" placeholder={f.placeholder} autoComplete="off" {...props} />;
  if (!f.nullable) return el;
  return (
    <span className="inline">{el}<label><input type="checkbox" checked={s[`${path}?`]} onChange={e => form.set(`${path}?`, e.target.checked)} /> null</label></span>
  );
}

function Form({ spec, form, patch }) {
  return (
    <div className="form">{spec.map(f => (
      <FieldRow key={f.key} f={f} include={patch && <input type="checkbox" title="Отправить это поле" checked={!!form.include[f.key]} onChange={e => form.setInclude(f.key, e.target.checked)} />}>
        <Control f={f} path={f.key} form={form} />
      </FieldRow>
    ))}</div>
  );
}

export function Action({ text, fn, cls, run }) {
  const [busy, setBusy] = useState(false);
  return <button type="button" className={cls} disabled={busy} onClick={async () => { setBusy(true); await run(fn); setBusy(false); }}>{text}</button>;
}

export function ResourceTab({ cfg, active }) {
  const [result, setResult] = useState(null);
  const [getId, setGetId] = useState('');
  const [patchId, setPatchId] = useState('');
  const createForm = useForm(cfg.form);
  const patchForm = useForm(cfg.form);
  const resultRef = useRef();
  const patchRef = useRef();
  const listRef = useRef();
  const loaded = useRef(false);
  const itemUrl = id => `${cfg.basePath}/${encodeURIComponent(String(id).trim())}`;

  useEffect(() => {
    if (active && !loaded.current) {
      loaded.current = true;
      listRef.current();
    }
  }, [active]);

  useEffect(() => {
    if (result) resultRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [result]);

  const ctx = {
    show: node => setResult({ node }),
    async run(fn) {
      try { await fn(); } catch (e) { ctx.show(renderError(e)); }
    },
    reload: () => listRef.current(),
  };

  const needId = v => {
    if (v.trim() !== '') return true;
    ctx.show(<div className="panel info"><p>Укажите id.</p></div>);
    return false;
  };

  async function getOne(id) {
    const res = await request('GET', itemUrl(id));
    ctx.show(renderSuccess(`${cfg.noun} #${res.data.id}`, res, cfg.card(res.data)));
  }

  async function deleteOne(id) {
    if (!confirm(`Удалить: ${cfg.noun.toLowerCase()} #${id}?`)) return;
    const res = await request('DELETE', itemUrl(id));
    ctx.show(renderSuccess(cfg.msg.deleted(id), res));
    ctx.reload();
  }

  const rowActions = item => <>
    <Action run={ctx.run} text="Открыть" cls="small" fn={() => getOne(item.id)} />
    <Action run={ctx.run} text="Изменить" cls="small" fn={() => { setPatchId(String(item.id)); patchForm.fill(item); patchRef.current.scrollIntoView({ behavior: 'smooth' }); }} />
    <Action run={ctx.run} text="Удалить" cls="small danger" fn={() => deleteOne(item.id)} />
  </>;

  const Extra = cfg.Extra;

  return (
    <div>
      <section className="box"><h3>{`Список (GET ${cfg.basePath})`}</h3><ListView cfg={cfg} listRef={listRef} rowActions={rowActions} /></section>
      <div className="result" ref={resultRef}>{result && <div className="closable"><Btn text="✕" cls="small close" onClick={() => setResult(null)} />{result.node}</div>}</div>
      <div className="grid">
        <section className="box" ref={patchRef}>
          <h3>Изменить (PATCH, application/merge-patch+json)</h3>
          <div className="toolbar">
            <label>{'id '}<Input placeholder="id" size={8} value={patchId} set={setPatchId} /></label>
            <Action run={ctx.run} text="Загрузить текущие значения" fn={async () => {
              if (!needId(patchId)) return;
              const res = await request('GET', itemUrl(patchId));
              patchForm.fill(res.data);
            }} />
          </div>
          <p className="hint">Отправляются только отмеченные галочкой поля.</p>
          <Form spec={cfg.form} form={patchForm} patch />
          <div className="toolbar">
            <Action run={ctx.run} text="Отправить PATCH" cls="primary" fn={async () => {
              if (!needId(patchId)) return;
              const res = await request('PATCH', itemUrl(patchId), JSON.stringify(patchForm.value(true)), 'application/merge-patch+json');
              ctx.show(renderSuccess(cfg.msg.updated(res.data && res.data.id), res, res.data && cfg.card(res.data)));
              ctx.reload();
            }} />
          </div>
        </section>
        <section className="box">
          <h3>{`Создать (POST ${cfg.basePath})`}</h3>
          <Form spec={cfg.form} form={createForm} />
          <div className="toolbar">
            <Action run={ctx.run} text="Создать" cls="primary" fn={async () => {
              const res = await request('POST', cfg.basePath, JSON.stringify(createForm.value(false)));
              ctx.show(renderSuccess(cfg.msg.created(res.data && res.data.id), res, res.data && cfg.card(res.data)));
              ctx.reload();
            }} />
            <Btn text="Заполнить примером" cls="link" onClick={() => createForm.fill(cfg.example)} />
          </div>
        </section>
        <section className="box">
          <h3>Найти или удалить по id</h3>
          <div className="toolbar">
            <Input placeholder="id" size={8} value={getId} set={setGetId} />
            <Action run={ctx.run} text="Получить" cls="primary" fn={async () => { if (needId(getId)) await getOne(getId); }} />
            <Action run={ctx.run} text="Удалить" cls="danger" fn={async () => { if (needId(getId)) await deleteOne(getId.trim()); }} />
          </div>
        </section>
        {Extra && <Extra ctx={ctx} />}
      </div>
    </div>
  );
}
