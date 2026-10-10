// Shared building blocks: DOM helper, formatting, HTTP, error panels, list view, forms, resource tab.

export function h(tag, props, ...children) {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(props || {})) {
    if (value === undefined || value === null || value === false) continue;
    if (key === 'class') el.className = value;
    else if (key === 'text') el.textContent = value;
    else if (key.startsWith('on')) el.addEventListener(key.slice(2), value);
    else if (key === 'value' || key === 'checked' || key === 'hidden') el[key] = value;
    else el.setAttribute(key, value === true ? '' : String(value));
  }
  for (const c of children.flat(Infinity)) {
    if (c === null || c === undefined || c === false) continue;
    el.append(c instanceof Node ? c : String(c));
  }
  return el;
}

export const btn = (text, onclick, cls = '') => h('button', { type: 'button', class: cls, onclick, text });

function select(options, value) {
  const s = h('select', null, options.map(o => h('option', { value: o.value, text: o.label })));
  if (value !== undefined) s.value = value;
  return s;
}

export const input = (placeholder, size) => h('input', { type: 'text', placeholder, size, autocomplete: 'off' });

// ---------- formatting ----------

export const dash = () => h('span', { class: 'null', title: 'null', text: '-' });

export function show(v) {
  if (v === null || v === undefined) return dash();
  if (v instanceof Node) return v;
  if (v === '') return h('span', { class: 'muted', text: '(пустая строка)' });
  return String(v);
}

export function fmtDateTime(s) {
  if (s == null) return dash();
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return String(s);
  return h('time', { datetime: s, title: s, text: d.toLocaleString('ru-RU') });
}

export function fmtDate(s) {
  if (s == null) return dash();
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(s));
  return m ? h('time', { datetime: s, title: s, text: `${m[3]}.${m[2]}.${m[1]}` }) : String(s);
}

export function fmtEnum(code, labels) {
  if (code == null) return dash();
  return labels[code] ? h('span', null, labels[code], ' ', h('code', { text: code })) : h('code', { text: String(code) });
}

export const fmtNumber = n => (n == null ? dash() : typeof n === 'number' ? n.toLocaleString('ru-RU', { maximumFractionDigits: 20 }) : String(n));

export const dl = pairs => h('dl', { class: 'card' }, pairs.map(([k, v]) => [h('dt', { text: k }), h('dd', null, show(v))]));

export function plural(n, one, few, many) {
  const a = Math.abs(n) % 100, b = a % 10;
  if (a > 10 && a < 20) return many;
  if (b === 1) return one;
  return b >= 2 && b <= 4 ? few : many;
}

export const enumOptions = labels => Object.entries(labels).map(([value, ru]) => ({ value, label: `${ru} (${value})` }));

// ---------- HTTP ----------

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

function requestDetails(req) {
  if (!req) return null;
  let text = `${req.method} ${req.url}`;
  if (req.body !== undefined) text += `\nContent-Type: ${req.contentType}\n\n${req.body}`;
  return h('details', { class: 'req' }, h('summary', { text: 'Отправленный запрос' }), h('pre', { text }));
}

function responseDetails(res) {
  if (!res || res.status === undefined) return null;
  let text = `HTTP ${res.status} ${res.statusText || ''}`.trim();
  if (res.location) text += `\nLocation: ${res.location}`;
  text += '\n\n' + (res.isJson && res.data !== null ? JSON.stringify(res.data, null, 2) : res.text || '(пустое тело)');
  return h('details', { class: 'req' }, h('summary', { text: 'Полученный ответ' }), h('pre', { text }));
}

export function renderError(err) {
  const panel = h('div', { class: 'panel error', role: 'alert' });
  if (!(err instanceof ApiError)) {
    console.error(err);
    panel.append(h('h3', { text: 'Ошибка в клиенте' }), h('p', { text: String(err && err.message ? err.message : err) }));
    return panel;
  }
  if (err.kind === 'network') {
    panel.append(h('h3', { text: 'Сервер недоступен: ответ не получен' }), h('p', { text: err.cause }));
  } else {
    panel.append(h('h3', { text: err.kind === 'body'
      ? `HTTP ${err.status}: ответ не является корректным JSON`
      : `HTTP ${err.status}: ${describe(err.status)}` }));
    const d = err.data;
    if (err.kind === 'http' && err.isJson && d && typeof d === 'object' && 'message' in d) {
      panel.append(h('p', { class: 'message', text: String(d.message) }));
      if (d.time) panel.append(h('p', { class: 'muted' }, 'Время: ', fmtDateTime(d.time)));
      if (Array.isArray(d.details) && d.details.length) {
        panel.append(h('table', { class: 'details' },
          h('thead', null, h('tr', null, h('th', { text: 'Поле' }), h('th', { text: 'Ошибка' }))),
          h('tbody', null, d.details.map(x => h('tr', null, h('td', null, h('code', { text: String(x.field) })), h('td', { text: String(x.message) }))))));
      }
    } else if (err.text) {
      panel.append(h('p', { text: 'Тело ответа не в формате Error:' }), h('pre', { text: err.text.slice(0, 2000) }));
    } else {
      panel.append(h('p', { text: 'Тело ответа пустое.' }));
    }
  }
  panel.append(...[requestDetails(err.request), responseDetails(err)].filter(Boolean));
  return panel;
}

export function renderSuccess(title, res, content) {
  return h('div', { class: 'panel success' },
    h('h3', { text: title }),
    res && res.location && h('p', null, 'Location: ', h('code', { text: res.location })),
    content,
    res && requestDetails(res.request),
    responseDetails(res));
}

// ---------- list view: paging, multi-level sort, multi filter ----------

const OPS = [
  ['=', 'равно'], ['!=', 'не равно'], ['>', 'больше'], ['>=', 'больше или равно'],
  ['<', 'меньше'], ['<=', 'меньше или равно'], ['~', 'содержит'],
].map(([value, ru]) => ({ value, label: `${value}  ${ru}` }));

function createListView(cfg) {
  const fieldOptions = cfg.fields.map(f => ({ value: f.value, label: f.label }));
  const pageIn = input('1', 4);
  const sizeIn = input('20', 4);
  pageIn.value = '1';
  sizeIn.value = '20';
  const sortRows = h('div', { class: 'rows sorts' });
  const filterRows = h('div', { class: 'rows' });
  const counter = h('span', { class: 'muted counter' });
  const urlLine = h('code', { class: 'url' });
  const out = h('div');
  const pageInfo = h('span', { class: 'page-info' });
  const prevBtn = btn('← Назад', () => go(-1));
  const nextBtn = btn('Вперёд →', () => go(1));
  let current = null;

  const updatePager = () => {
    prevBtn.disabled = !current || current.page <= 1;
    nextBtn.disabled = !current || current.page >= current.totalPages;
  };

  const updateCounter = () => {
    counter.textContent = `${filterRows.children.length} фильтров · ${sortRows.children.length} сортировок`;
  };

  function addSort(fieldName = cfg.fields[0].value) {
    const field = select(fieldOptions);
    field.value = fieldName;
    const dir = select([{ value: 'asc', label: 'По возрастанию' }, { value: 'desc', label: 'По убыванию' }]);
    const row = h('div', { class: 'row sort-row' }, field, dir,
      btn('↑', () => row.previousElementSibling && sortRows.insertBefore(row, row.previousElementSibling), 'small'),
      btn('↓', () => row.nextElementSibling && sortRows.insertBefore(row.nextElementSibling, row), 'small'),
      btn('✕', () => { row.remove(); updateCounter(); }, 'small'));
    row.param = () => `${field.value},${dir.value}`;
    sortRows.append(row);
    updateCounter();
  }

  // The value control follows the field: a list of values for enums, free text otherwise.
  function valueControl(f) {
    if (f.values) {
      const options = Object.entries(f.values).map(([value, ru]) => ({ value, label: value === ru ? value : `${value} (${ru})` }));
      return select(f.nullable ? [...options, { value: 'null', label: 'null (не задано)' }] : options);
    }
    const control = input(f.placeholder || (f.nullable ? 'Значение или null' : 'Значение'), 18);
    return control;
  }

  function addFilter() {
    const field = select(fieldOptions);
    const op = select(OPS);
    let value = valueControl(cfg.fields[0]);
    field.addEventListener('change', () => {
      const next = valueControl(cfg.fields.find(x => x.value === field.value));
      value.replaceWith(next);
      value = next;
    });
    const row = h('div', { class: 'row filter-row' }, field, op, value,
      btn('✕', () => { row.remove(); updateCounter(); }, 'small'));
    row.param = () => `${field.value}${op.value}${value.value}`;
    filterRows.append(row);
    updateCounter();
  }

  function buildUrl() {
    const p = new URLSearchParams();
    if (pageIn.value.trim() !== '') p.append('page', pageIn.value.trim());
    if (sizeIn.value.trim() !== '') p.append('size', sizeIn.value.trim());
    for (const r of sortRows.children) p.append('sort', r.param());
    for (const r of filterRows.children) p.append('filter', r.param());
    const qs = p.toString();
    return qs ? `${cfg.basePath}?${qs}` : cfg.basePath;
  }

  async function load() {
    const url = buildUrl();
    urlLine.textContent = `GET ${url}`;
    out.replaceChildren(h('p', { class: 'muted', text: 'Загрузка...' }));
    try {
      const res = await request('GET', url);
      current = res.data;
      out.replaceChildren(renderTable(res.data.items), requestDetails(res.request), responseDetails(res));
      pageIn.value = String(current.page);
      pageInfo.textContent = `Страница ${current.page} из ${current.totalPages}, всего ${current.totalElements} `
        + `(по ${current.size} на странице)`;
    } catch (e) {
      current = null;
      pageInfo.textContent = '';
      out.replaceChildren(renderError(e));
    }
    updatePager();
  }

  function renderTable(items) {
    if (!items.length) return h('p', { class: 'muted', text: 'Ничего не найдено.' });
    return h('div', { class: 'table-wrap' }, h('table', { class: 'list' },
      h('thead', null, h('tr', null, cfg.columns.map(c => h('th', { text: c[0] })), h('th', { text: 'Действия' }))),
      h('tbody', null, items.map(item => h('tr', null,
        cfg.columns.map(c => h('td', null, show(c[1](item)))),
        h('td', { class: 'actions' }, cfg.rowActions(item)))))));
  }

  const go = delta => {
    const base = current ? current.page : Number(pageIn.value) || 1;
    pageIn.value = String(base + delta);
    load();
  };

  const reset = () => {
    sortRows.replaceChildren();
    filterRows.replaceChildren();
    addSort();
    pageIn.value = '1';
    sizeIn.value = '20';
    load();
  };
  addSort();

  const element = h('div', null,
    h('details', { class: 'query' },
      h('summary', null, h('strong', { text: 'Фильтры и сортировка' }), ' ', counter),
      h('div', { class: 'qblock' }, h('h4', { text: 'Фильтры · все условия через И' }), filterRows,
        btn('+ Фильтр', addFilter, 'small')),
      h('div', { class: 'qblock' }, h('h4', { text: 'Сортировка · приоритет сверху вниз' }), sortRows,
        btn('+ Сортировка', () => addSort(), 'small')),
      h('p', { class: 'muted hint', text: 'Фильтровать и сортировать можно по любому полю. «содержит» ищет подстроку '
        + 'без учёта регистра. Для полей, которые могут быть не заданы, «равно null» и «не равно null» проверяют '
        + 'наличие значения. Даты: ГГГГ-ММ-ДД или ГГГГ-ММ-ДДTчч:мм:ссZ.' }),
      h('div', { class: 'toolbar' }, btn('Применить', () => { pageIn.value = '1'; load(); }, 'primary'),
        btn('Сбросить', reset))),
    h('div', { class: 'toolbar' },
      h('label', null, 'Страница ', pageIn), h('label', null, 'Размер ', sizeIn),
      btn('Показать', load, 'primary')),
    h('p', null, urlLine),
    out,
    h('div', { class: 'pager' }, prevBtn, pageInfo, nextBtn));

  return { element, reload: load };
}

// ---------- forms ----------

// Empty number field becomes null, a non-numeric value is sent as the raw string so the server can reject it.
function numOrRaw(s) {
  const t = s.trim();
  if (t === '') return null;
  const n = Number(t);
  return Number.isFinite(n) ? n : s;
}

function makeControl(f) {
  if (f.kind === 'enum') {
    const s = select([{ value: '', label: '(null) не задано' }, ...enumOptions(f.labels)]);
    return { element: s, get: () => (s.value === '' ? null : s.value), set: v => { s.value = v == null ? '' : v; } };
  }
  if (f.kind === 'group') {
    const kids = f.children.map(c => ({ c, ctl: makeControl(c) }));
    const body = h('div', { class: 'group' }, kids.map(({ c, ctl }) => fieldRow(c, ctl.element)));
    const getObj = () => Object.fromEntries(kids.map(({ c, ctl }) => [c.key, ctl.get()]));
    const setObj = o => kids.forEach(({ c, ctl }) => ctl.set(o ? o[c.key] : undefined));
    if (!f.nullable) return { element: body, get: getObj, set: setObj };
    const mode = select([{ value: 'null', label: 'нет (null)' }, { value: 'obj', label: 'указать' }], 'null');
    const sync = () => { body.hidden = mode.value === 'null'; };
    mode.addEventListener('change', sync);
    sync();
    return {
      element: h('div', null, mode, body),
      get: () => (mode.value === 'null' ? null : getObj()),
      set: v => { mode.value = v == null ? 'null' : 'obj'; setObj(v); sync(); },
    };
  }
  const el = f.kind === 'textarea' ? h('textarea', { rows: 2 }) : input(f.placeholder);
  const read = () => (f.kind === 'number' ? numOrRaw(el.value) : el.value);
  const write = v => { el.value = v == null ? '' : String(v); };
  if (!f.nullable) return { element: el, get: read, set: write };
  const isNull = h('input', { type: 'checkbox' });
  const sync = () => { el.disabled = isNull.checked; };
  isNull.addEventListener('change', sync);
  return {
    element: h('span', { class: 'inline' }, el, h('label', null, isNull, ' null')),
    get: () => (isNull.checked ? null : read()),
    set: v => { isNull.checked = v === null; write(v); sync(); },
  };
}

function fieldRow(f, control, include) {
  const label = include ? h('label', null, include, ' ', f.label) : h('span', { text: f.label });
  return h('div', { class: 'field' }, h('div', { class: 'label' }, label, f.hint && h('small', { text: f.hint })), control);
}

// In patch mode every top-level field gets an "include" checkbox: only checked fields are sent.
function buildForm(spec, patch) {
  const entries = spec.map(f => {
    const ctl = makeControl(f);
    const include = patch ? h('input', { type: 'checkbox', title: 'Отправить это поле' }) : null;
    const row = fieldRow(f, ctl.element, include);
    if (include) {
      const mark = e => { if (e.target !== include) include.checked = true; };
      row.addEventListener('input', mark);
      row.addEventListener('change', mark);
    }
    return { f, ctl, include, row };
  });
  return {
    element: h('div', { class: 'form' }, entries.map(e => e.row)),
    value() {
      const o = {};
      for (const { f, ctl, include } of entries) if (!include || include.checked) o[f.key] = ctl.get();
      return o;
    },
    fill(o) {
      for (const { f, ctl, include } of entries) {
        ctl.set(o[f.key]);
        if (include) include.checked = false;
      }
    },
  };
}

// ---------- resource tab: list + get/delete + create + patch + extra operations ----------

export function createResourceTab(cfg) {
  const result = h('div', { class: 'result' });
  const itemUrl = id => `${cfg.basePath}/${encodeURIComponent(String(id).trim())}`;

  const ctx = {
    show(node) {
      result.replaceChildren(h('div', { class: 'closable' }, btn('✕', () => result.replaceChildren(), 'small close'), node));
      result.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    },
    async run(fn) {
      try { await fn(); } catch (e) { ctx.show(renderError(e)); }
    },
    reload: () => list.reload(),
  };

  const action = (text, fn, cls) => {
    const b = btn(text, async () => { b.disabled = true; await ctx.run(fn); b.disabled = false; }, cls);
    return b;
  };

  const needId = el => {
    if (el.value.trim() !== '') return true;
    ctx.show(h('div', { class: 'panel info' }, h('p', { text: 'Укажите id.' })));
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

  const list = createListView({
    ...cfg,
    rowActions: item => [
      action('Открыть', () => getOne(item.id), 'small'),
      action('Изменить', () => { patchId.value = String(item.id); patchForm.fill(item); patchCard.scrollIntoView({ behavior: 'smooth' }); }, 'small'),
      action('Удалить', () => deleteOne(item.id), 'small danger'),
    ],
  });

  const getId = input('id', 8);
  const getCard = h('section', { class: 'box' }, h('h3', { text: 'Найти или удалить по id' }),
    h('div', { class: 'toolbar' }, getId,
      action('Получить', async () => { if (needId(getId)) await getOne(getId.value); }, 'primary'),
      action('Удалить', async () => { if (needId(getId)) await deleteOne(getId.value.trim()); }, 'danger')));

  const createForm = buildForm(cfg.form, false);
  const createCard = h('section', { class: 'box' }, h('h3', { text: `Создать (POST ${cfg.basePath})` }),
    createForm.element,
    h('div', { class: 'toolbar' },
      action('Создать', async () => {
        const res = await request('POST', cfg.basePath, JSON.stringify(createForm.value()));
        ctx.show(renderSuccess(cfg.msg.created(res.data && res.data.id), res, res.data && cfg.card(res.data)));
        ctx.reload();
      }, 'primary'),
      btn('Заполнить примером', () => createForm.fill(cfg.example), 'link')));

  const patchId = input('id', 8);
  const patchForm = buildForm(cfg.form, true);
  const patchCard = h('section', { class: 'box' }, h('h3', { text: 'Изменить (PATCH, application/merge-patch+json)' }),
    h('div', { class: 'toolbar' }, h('label', null, 'id ', patchId),
      action('Загрузить текущие значения', async () => {
        if (!needId(patchId)) return;
        const res = await request('GET', itemUrl(patchId.value));
        patchForm.fill(res.data);
      })),
    h('p', { class: 'hint', text: 'Отправляются только отмеченные галочкой поля.' }),
    patchForm.element,
    h('div', { class: 'toolbar' },
      action('Отправить PATCH', async () => {
        if (!needId(patchId)) return;
        const res = await request('PATCH', itemUrl(patchId.value), JSON.stringify(patchForm.value()), 'application/merge-patch+json');
        ctx.show(renderSuccess(cfg.msg.updated(res.data && res.data.id), res, res.data && cfg.card(res.data)));
        ctx.reload();
      }, 'primary')));

  const element = h('div', null,
    h('section', { class: 'box' }, h('h3', { text: `Список (GET ${cfg.basePath})` }), list.element),
    result,
    h('div', { class: 'grid' }, getCard, createCard, patchCard, cfg.extra ? cfg.extra({ ...ctx, action }) : null));

  return { element, reload: list.reload };
}

