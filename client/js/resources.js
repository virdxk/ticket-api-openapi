import {
  h, dash, dl, fmtDate, fmtDateTime, fmtEnum, fmtNumber, plural, input, request, renderSuccess, createResourceTab,
} from './lib.js';

const TICKET_TYPES = { VIP: 'VIP', USUAL: 'обычный', BUDGETARY: 'бюджетный', CHEAP: 'дешёвый' };
const VENUE_TYPES = { PUB: 'паб', BAR: 'бар', OPEN_AREA: 'открытая площадка', MALL: 'торговый центр', STADIUM: 'стадион' };
const enc = v => encodeURIComponent(String(v).trim());

// ---------- tickets ----------

const coords = c => (c == null ? dash() : `x = ${c.x}; y = ${c.y}`);

const venueShort = v => (v == null ? dash() : h('div', null, h('strong', { text: v.name }),
  h('div', { class: 'muted' }, fmtEnum(v.type, VENUE_TYPES), `, мест: ${v.capacity}, id ${v.id}`)));

const ticketCard = t => dl([
  ['id', t.id], ['Название', t.name], ['Координаты', coords(t.coordinates)],
  ['Дата создания', fmtDateTime(t.creationDate)], ['Цена', fmtNumber(t.price)], ['Комментарий', t.comment],
  ['Тип', fmtEnum(t.type, TICKET_TYPES)],
  ['Площадка', t.venue == null ? dash() : dl([
    ['id', t.venue.id], ['Название', t.venue.name], ['Вместимость', fmtNumber(t.venue.capacity)],
    ['Тип', fmtEnum(t.venue.type, VENUE_TYPES)]])],
]);

export const tickets = () => createResourceTab({
  name: 'tickets',
  basePath: 'https://se.ifmo.ru/~s412939/ticket-service/tickets',
  noun: 'Билет',
  msg: { created: id => `Билет #${id} создан`, updated: id => `Билет #${id} обновлён`, deleted: id => `Билет #${id} удалён` },
  fields: [
    { value: 'id', label: 'ID' },
    { value: 'name', label: 'Название' },
    { value: 'coordinates.x', label: 'Координата X' },
    { value: 'coordinates.y', label: 'Координата Y (≤ 747)' },
    { value: 'creationDate', label: 'Дата создания', placeholder: '2026-10-01 или 2026-10-01T12:00:00Z' },
    { value: 'price', label: 'Цена (> 0)', nullable: true },
    { value: 'comment', label: 'Комментарий (≤ 681 символа)' },
    { value: 'type', label: 'Тип билета', values: TICKET_TYPES, nullable: true },
    { value: 'venue.id', label: 'Площадка: ID', nullable: true },
    { value: 'venue.name', label: 'Площадка: название', nullable: true },
    { value: 'venue.capacity', label: 'Площадка: вместимость (> 0)', nullable: true },
    { value: 'venue.type', label: 'Площадка: тип', values: VENUE_TYPES, nullable: true },
  ],
  columns: [
    ['id', t => t.id],
    ['Название', t => t.name],
    ['Координаты', t => coords(t.coordinates)],
    ['Создан', t => fmtDateTime(t.creationDate)],
    ['Цена', t => fmtNumber(t.price)],
    ['Комментарий', t => t.comment],
    ['Тип', t => fmtEnum(t.type, TICKET_TYPES)],
    ['Площадка', t => venueShort(t.venue)],
  ],
  card: ticketCard,
  form: [
    { key: 'name', label: 'Название' },
    { key: 'coordinates', label: 'Координаты', kind: 'group', children: [
      { key: 'x', label: 'x', kind: 'number', hint: 'пусто = null' },
      { key: 'y', label: 'y', kind: 'number', hint: 'пусто = null' },
    ] },
    { key: 'price', label: 'Цена', kind: 'number', nullable: true, hint: 'пусто = null' },
    { key: 'comment', label: 'Комментарий', kind: 'textarea' },
    { key: 'type', label: 'Тип', kind: 'enum', labels: TICKET_TYPES },
    { key: 'venue', label: 'Площадка', kind: 'group', nullable: true, children: [
      { key: 'name', label: 'Название' },
      { key: 'capacity', label: 'Вместимость', kind: 'number', hint: 'пусто = null' },
      { key: 'type', label: 'Тип', kind: 'enum', labels: VENUE_TYPES },
    ] },
  ],
  example: {
    name: 'Концертный билет', coordinates: { x: 12.5, y: 100 }, price: 2500, comment: 'Первый ряд', type: 'USUAL',
    venue: { name: 'Главный стадион', capacity: 50000, type: 'STADIUM' },
  },
  extra: ctx => {
    const type = h('select', null, Object.entries(TICKET_TYPES).map(([value, ru]) =>
      h('option', { value, text: value === ru ? value : `${value} (${ru})` })));
    return h('section', { class: 'box' }, h('h3', { text: 'Удалить любой билет заданного типа' }),
      h('div', { class: 'toolbar' }, h('label', null, 'Тип ', type),
        ctx.action('Удалить один билет', async () => {
          const res = await request('DELETE', `https://se.ifmo.ru/~s412939/ticket-service/tickets/actions/by-type/${enc(type.value)}`);
          ctx.show(renderSuccess(`Удалён один билет типа ${type.value}`, res));
          ctx.reload();
        }, 'danger')));
  },
});

// ---------- persons ----------

export const persons = () => createResourceTab({
  name: 'persons',
  basePath: 'https://se.ifmo.ru/~s412939/person-service/persons',
  noun: 'Человек',
  msg: { created: id => `Человек #${id} создан`, updated: id => `Человек #${id} обновлён`, deleted: id => `Человек #${id} удалён` },
  fields: [
    { value: 'id', label: 'ID' },
    { value: 'firstName', label: 'Имя' },
    { value: 'lastName', label: 'Фамилия' },
    { value: 'birthday', label: 'Дата рождения', placeholder: 'ГГГГ-ММ-ДД' },
  ],
  columns: [
    ['id', p => p.id], ['Имя', p => p.firstName], ['Фамилия', p => p.lastName], ['Дата рождения', p => fmtDate(p.birthday)],
  ],
  card: p => dl([['id', p.id], ['Имя', p.firstName], ['Фамилия', p.lastName], ['Дата рождения', fmtDate(p.birthday)]]),
  form: [
    { key: 'firstName', label: 'Имя' },
    { key: 'lastName', label: 'Фамилия' },
    { key: 'birthday', label: 'Дата рождения', placeholder: 'ГГГГ-ММ-ДД' },
  ],
  example: { firstName: 'Иван', lastName: 'Иванов', birthday: '2004-05-17' },
});

// ---------- bookings ----------

const bookingCard = b => dl([
  ['id', b.id], ['id билета', b.ticketId], ['id человека', b.personId], ['Создано', fmtDateTime(b.createdAt)],
]);

export const bookings = () => createResourceTab({
  name: 'bookings',
  basePath: 'https://se.ifmo.ru/~s412939/booking-service/booking/bookings',
  noun: 'Бронирование',
  msg: {
    created: id => `Бронирование #${id} создано`, updated: id => `Бронирование #${id} обновлено`,
    deleted: id => `Бронирование #${id} удалено`,
  },
  fields: [
    { value: 'id', label: 'ID' },
    { value: 'ticketId', label: 'ID билета' },
    { value: 'personId', label: 'ID человека' },
    { value: 'createdAt', label: 'Время создания', placeholder: '2026-10-01 или 2026-10-01T12:00:00Z' },
  ],
  columns: [
    ['id', b => b.id], ['id билета', b => b.ticketId], ['id человека', b => b.personId], ['Создано', b => fmtDateTime(b.createdAt)],
  ],
  card: bookingCard,
  form: [
    { key: 'ticketId', label: 'id билета', kind: 'number', hint: 'пусто = null' },
    { key: 'personId', label: 'id человека', kind: 'number', hint: 'пусто = null' },
  ],
  example: { ticketId: 1, personId: 1 },
  extra: ctx => {
    const sellTicket = input('id билета', 8);
    const sellPerson = input('id человека', 8);
    const cancelPerson = input('id человека', 8);
    return [
      h('section', { class: 'box' }, h('h3', { text: 'Продать VIP-копию билета' }),
        h('p', { class: 'hint', text: 'Создаёт копию билета с типом VIP и удвоенной ценой и бронирует её на человека.' }),
        h('div', { class: 'toolbar' }, h('label', null, 'Билет ', sellTicket), h('label', null, 'Человек ', sellPerson),
          ctx.action('Продать', async () => {
            const res = await request('POST', `https://se.ifmo.ru/~s412939/booking-service/booking/sell/vip/${enc(sellTicket.value)}/${enc(sellPerson.value)}`);
            const b = res.data;
            ctx.show(renderSuccess(`VIP-копия продана: бронирование #${b.id}, новый билет #${b.ticketId}, человек #${b.personId}`,
              res, bookingCard(b)));
            ctx.reload();
          }, 'primary'))),
      h('section', { class: 'box' }, h('h3', { text: 'Отменить все бронирования человека' }),
        h('div', { class: 'toolbar' }, h('label', null, 'Человек ', cancelPerson),
          ctx.action('Отменить', async () => {
            const res = await request('DELETE', `https://se.ifmo.ru/~s412939/booking-service/booking/person/${enc(cancelPerson.value)}/cancel`);
            const { personId, cancelledBookings: n } = res.data;
            ctx.show(renderSuccess(n === 0
              ? `У человека #${personId} не было бронирований, ничего не отменено.`
              : `У человека #${personId} отменено ${n} ${plural(n, 'бронирование', 'бронирования', 'бронирований')}.`, res));
            ctx.reload();
          }, 'danger'))),
    ];
  },
});
