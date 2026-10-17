import { useState } from 'react';
import {
  Action, Input, dash, dl, fmtDate, fmtDateTime, fmtEnum, fmtNumber, plural, request, renderSuccess,
} from './lib.jsx';

const TICKET_TYPES = { VIP: 'ВИП', USUAL: 'обычный', BUDGETARY: 'бюджетный', CHEAP: 'дешёвый' };
const VENUE_TYPES = { PUB: 'паб', BAR: 'бар', OPEN_AREA: 'открытая площадка', MALL: 'торговый центр', STADIUM: 'стадион' };
const enc = v => encodeURIComponent(String(v).trim());

const coords = c => (c == null ? dash() : `x = ${c.x}; y = ${c.y}`);

const venueShort = v => (v == null ? dash() : (
  <div><strong>{v.name}</strong><div className="muted">{fmtEnum(v.type, VENUE_TYPES)}{`, мест: ${v.capacity}, id ${v.id}`}</div></div>
));

const ticketCard = t => dl([
  ['id', t.id], ['Название', t.name], ['Координаты', coords(t.coordinates)],
  ['Дата создания', fmtDateTime(t.creationDate)], ['Цена', fmtNumber(t.price)], ['Комментарий', t.comment],
  ['Тип', fmtEnum(t.type, TICKET_TYPES)],
  ['Площадка', t.venue == null ? dash() : dl([
    ['id', t.venue.id], ['Название', t.venue.name], ['Вместимость', fmtNumber(t.venue.capacity)],
    ['Тип', fmtEnum(t.venue.type, VENUE_TYPES)]])],
]);

function TicketExtra({ ctx }) {
  const [type, setType] = useState('VIP');
  return (
    <section className="box">
      <h3>Удалить любой билет заданного типа</h3>
      <div className="toolbar">
        <label>{'Тип '}<select value={type} onChange={e => setType(e.target.value)}>
          {Object.entries(TICKET_TYPES).map(([value, ru]) => <option key={value} value={value}>{value === ru ? value : `${value} (${ru})`}</option>)}
        </select></label>
        <Action run={ctx.run} text="Удалить один билет" cls="danger" fn={async () => {
          const res = await request('DELETE', `https://se.ifmo.ru/~s412939/ticket-service/tickets/actions/by-type/${enc(type)}`);
          ctx.show(renderSuccess(`Удалён один билет типа ${type}`, res));
          ctx.reload();
        }} />
      </div>
    </section>
  );
}

export const tickets = {
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
  Extra: TicketExtra,
};

export const persons = {
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
};

const bookingCard = b => dl([
  ['id', b.id], ['id билета', b.ticketId], ['id человека', b.personId], ['Создано', fmtDateTime(b.createdAt)],
]);

function BookingExtra({ ctx }) {
  const [sellTicket, setSellTicket] = useState('');
  const [sellPerson, setSellPerson] = useState('');
  const [cancelPerson, setCancelPerson] = useState('');
  return (
    <>
      <section className="box">
        <h3>Продать VIP-копию билета</h3>
        <p className="hint">Создаёт копию билета с типом VIP и удвоенной ценой и бронирует её на человека.</p>
        <div className="toolbar">
          <label>{'Билет '}<Input placeholder="id билета" size={8} value={sellTicket} set={setSellTicket} /></label>
          <label>{'Человек '}<Input placeholder="id человека" size={8} value={sellPerson} set={setSellPerson} /></label>
          <Action run={ctx.run} text="Продать" cls="primary" fn={async () => {
            const res = await request('POST', `https://se.ifmo.ru/~s412939/booking-service/booking/sell/vip/${enc(sellTicket)}/${enc(sellPerson)}`);
            const b = res.data;
            ctx.show(renderSuccess(`VIP-копия продана: бронирование #${b.id}, новый билет #${b.ticketId}, человек #${b.personId}`, res, bookingCard(b)));
            ctx.reload();
          }} />
        </div>
      </section>
      <section className="box">
        <h3>Отменить все бронирования человека</h3>
        <div className="toolbar">
          <label>{'Человек '}<Input placeholder="id человека" size={8} value={cancelPerson} set={setCancelPerson} /></label>
          <Action run={ctx.run} text="Отменить" cls="danger" fn={async () => {
            const res = await request('DELETE', `https://se.ifmo.ru/~s412939/booking-service/booking/person/${enc(cancelPerson)}/cancel`);
            const { personId, cancelledBookings: n } = res.data;
            ctx.show(renderSuccess(n === 0
              ? `У человека #${personId} не было бронирований, ничего не отменено.`
              : `У человека #${personId} отменено ${n} ${plural(n, 'бронирование', 'бронирования', 'бронирований')}.`, res));
            ctx.reload();
          }} />
        </div>
      </section>
    </>
  );
}

export const bookings = {
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
  Extra: BookingExtra,
};
