import { useEffect, useState } from 'react';

const OPS = ['=', '!=', '>', '>=', '<', '<=', '~'];
const STATUS = {
  400: 'некорректный запрос', 404: 'не найдено', 409: 'конфликт', 422: 'невалидные данные',
  500: 'внутренняя ошибка сервера', 503: 'сервис недоступен',
};

async function request(method, url, body, contentType = 'application/json') {
  try {
    const res = await fetch(url, {
      method,
      headers: body === undefined ? {} : { 'Content-Type': contentType },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = text; }
    return { ok: res.ok, status: res.status, data };
  } catch (e) {
    return { ok: false, status: 0, data: { message: `Сервер недоступен: ${e.message}` } };
  }
}

function buildBody(fields, values) {
  const body = {};
  for (const f of fields) {
    const raw = (values[f.name] ?? '').trim();
    if (raw === '') continue;
    const value = raw === 'null' ? null : f.number && raw !== '' && !isNaN(raw) ? Number(raw) : raw;
    const path = f.name.split('.');
    let target = body;
    for (const key of path.slice(0, -1)) target = target[key] ??= {};
    target[path.at(-1)] = value;
  }
  return body;
}

function Result({ result }) {
  if (!result) return null;
  const { ok, status, data, title } = result;
  return (
    <div className={ok ? 'ok' : 'error'}>
      {ok ? <b>{title}</b> : <b>Ошибка {status || ''}: {STATUS[status] || 'ошибка запроса'}</b>}
      {!ok && data?.message && <p>{data.message}</p>}
      {!ok && data?.details?.length > 0 && (
        <table>
          <thead><tr><th>Поле</th><th>Ошибка</th></tr></thead>
          <tbody>{data.details.map((d, i) => <tr key={i}><td>{d.field}</td><td>{d.message}</td></tr>)}</tbody>
        </table>
      )}
      {ok && data && typeof data === 'object' && (
        <table><tbody>{Object.entries(data).map(([k, v]) => (
          <tr key={k}><td>{k}</td><td>{v !== null && typeof v === 'object' ? JSON.stringify(v) : String(v)}</td></tr>
        ))}</tbody></table>
      )}
      {data !== null && (
        <details><summary>Полученный ответ</summary><pre>{JSON.stringify(data, null, 2)}</pre></details>
      )}
    </div>
  );
}

function List({ cfg }) {
  const [filters, setFilters] = useState([]);
  const [sorts, setSorts] = useState([]);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [result, setResult] = useState(null);

  async function load(p = page) {
    const params = new URLSearchParams({ page: p, size });
    sorts.forEach(s => params.append('sort', `${s.field},${s.dir}`));
    filters.forEach(f => params.append('filter', `${f.field}${f.op}${f.value}`));
    setPage(p);
    setResult(await request('GET', `${cfg.url}?${params}`));
  }

  useEffect(() => { load(); }, []);

  const update = (list, setList, i, key, value) => setList(list.map((x, j) => (j === i ? { ...x, [key]: value } : x)));
  const fieldSelect = (value, onChange) => (
    <select value={value} onChange={e => onChange(e.target.value)}>
      {cfg.filterFields.map(f => <option key={f}>{f}</option>)}
    </select>
  );
  const data = result?.ok ? result.data : null;

  return (
    <section>
      <h2>Список</h2>
      <h3>Фильтры (все условия через И)</h3>
      {filters.map((f, i) => (
        <div key={i}>
          {fieldSelect(f.field, v => update(filters, setFilters, i, 'field', v))}
          <select value={f.op} onChange={e => update(filters, setFilters, i, 'op', e.target.value)}>
            {OPS.map(op => <option key={op}>{op}</option>)}
          </select>
          <input value={f.value} placeholder="значение или null" onChange={e => update(filters, setFilters, i, 'value', e.target.value)} />
          <button onClick={() => setFilters(filters.filter((_, j) => j !== i))}>x</button>
        </div>
      ))}
      <button onClick={() => setFilters([...filters, { field: 'id', op: '=', value: '' }])}>+ фильтр</button>
      <h3>Сортировка (по порядку приоритета)</h3>
      {sorts.map((s, i) => (
        <div key={i}>
          {fieldSelect(s.field, v => update(sorts, setSorts, i, 'field', v))}
          <select value={s.dir} onChange={e => update(sorts, setSorts, i, 'dir', e.target.value)}>
            <option value="asc">по возрастанию</option>
            <option value="desc">по убыванию</option>
          </select>
          <button onClick={() => setSorts(sorts.filter((_, j) => j !== i))}>x</button>
        </div>
      ))}
      <button onClick={() => setSorts([...sorts, { field: 'id', dir: 'asc' }])}>+ сортировка</button>
      <p>
        Размер страницы <input value={size} onChange={e => setSize(e.target.value)} />
        <button onClick={() => load(1)}>Показать</button>
      </p>
      {result && !result.ok && <Result result={result} />}
      {data && (
        <>
          <table>
            <thead><tr>{cfg.columns.map(([title]) => <th key={title}>{title}</th>)}</tr></thead>
            <tbody>{data.items.map(item => (
              <tr key={item.id}>{cfg.columns.map(([title, get]) => <td key={title}>{get(item)}</td>)}</tr>
            ))}</tbody>
          </table>
          <p>
            <button disabled={data.page <= 1} onClick={() => load(data.page - 1)}>Назад</button>
            {' '}Страница {data.page} из {data.totalPages}, всего {data.totalElements}{' '}
            <button disabled={data.page >= data.totalPages} onClick={() => load(data.page + 1)}>Вперёд</button>
          </p>
          <details><summary>Полученный ответ</summary><pre>{JSON.stringify(data, null, 2)}</pre></details>
        </>
      )}
    </section>
  );
}

function Item({ cfg }) {
  const [id, setId] = useState('');
  const [values, setValues] = useState({});
  const [result, setResult] = useState(null);
  const url = `${cfg.url}/${id.trim()}`;

  const run = async (title, ...args) => setResult({ ...(await request(...args)), title });

  return (
    <section>
      <h2>Элемент</h2>
      <p>
        id <input value={id} onChange={e => setId(e.target.value)} />
        <button onClick={() => run('Найден', 'GET', url)}>Получить</button>
        <button onClick={() => run('Удалён', 'DELETE', url)}>Удалить</button>
        <button onClick={() => run('Изменён', 'PATCH', url, buildBody(cfg.fields, values), 'application/merge-patch+json')}>
          Изменить (PATCH)
        </button>
      </p>
      {cfg.fields.map(f => (
        <p key={f.name}>
          <label>{f.label} </label>
          {f.values ? (
            <select value={values[f.name] ?? ''} onChange={e => setValues({ ...values, [f.name]: e.target.value })}>
              <option value="">-</option>
              {f.values.map(v => <option key={v}>{v}</option>)}
              <option>null</option>
            </select>
          ) : (
            <input value={values[f.name] ?? ''} onChange={e => setValues({ ...values, [f.name]: e.target.value })} />
          )}
        </p>
      ))}
      <p className="hint">Пустые поля не отправляются, значение null очищает поле.</p>
      <button onClick={() => run('Создан', 'POST', cfg.url, buildBody(cfg.fields, values))}>Создать (POST)</button>
      <Result result={result} />
    </section>
  );
}

function Action({ action }) {
  const [values, setValues] = useState(action.inputs.map(i => (i.values ? i.values[0] : '')));
  const [result, setResult] = useState(null);
  return (
    <section>
      <h2>{action.title}</h2>
      {action.inputs.map((input, i) => {
        const set = e => setValues(values.map((v, j) => (j === i ? e.target.value : v)));
        return (
          <span key={i}>
            {input.label}{' '}
            {input.values
              ? <select value={values[i]} onChange={set}>{input.values.map(v => <option key={v}>{v}</option>)}</select>
              : <input value={values[i]} onChange={set} />}
          </span>
        );
      })}
      <button onClick={async () => setResult({ ...(await request(action.method, action.path(values.map(v => v.trim())))), title: 'Выполнено' })}>
        Выполнить
      </button>
      <Result result={result} />
    </section>
  );
}

export function Resource({ cfg }) {
  return (
    <>
      <List cfg={cfg} />
      <Item cfg={cfg} />
      {cfg.actions.map(a => <Action key={a.title} action={a} />)}
    </>
  );
}
