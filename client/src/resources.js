const BASE = 'https://se.ifmo.ru/~s412939';
const TICKET_TYPES = ['VIP', 'USUAL', 'BUDGETARY', 'CHEAP'];
const VENUE_TYPES = ['PUB', 'BAR', 'OPEN_AREA', 'MALL', 'STADIUM'];

const show = v => (v === null || v === undefined ? '-' : String(v));
const date = v => (v ? new Date(v).toLocaleString('ru-RU') : '-');

export const resources = [
  {
    title: 'Билеты',
    url: `${BASE}/ticket-service/tickets`,
    fields: [
      { name: 'name', label: 'Название' },
      { name: 'coordinates.x', label: 'Координата X', number: true },
      { name: 'coordinates.y', label: 'Координата Y (≤ 747)', number: true },
      { name: 'price', label: 'Цена (> 0, может быть null)', number: true },
      { name: 'comment', label: 'Комментарий (≤ 681 символа)' },
      { name: 'type', label: 'Тип (может быть null)', values: TICKET_TYPES },
      { name: 'venue', label: 'Площадка (null, чтобы убрать)' },
      { name: 'venue.name', label: 'Площадка: название' },
      { name: 'venue.capacity', label: 'Площадка: вместимость (> 0)', number: true },
      { name: 'venue.type', label: 'Площадка: тип', values: VENUE_TYPES },
    ],
    filterFields: ['id', 'name', 'coordinates.x', 'coordinates.y', 'creationDate', 'price', 'comment', 'type',
      'venue.id', 'venue.name', 'venue.capacity', 'venue.type'],
    columns: [
      ['id', t => t.id],
      ['Название', t => t.name],
      ['Координаты', t => `(${t.coordinates.x}; ${t.coordinates.y})`],
      ['Создан', t => date(t.creationDate)],
      ['Цена', t => show(t.price)],
      ['Комментарий', t => t.comment],
      ['Тип', t => show(t.type)],
      ['Площадка', t => (t.venue ? `${t.venue.name}, ${t.venue.type}, мест: ${t.venue.capacity} (id ${t.venue.id})` : '-')],
    ],
    actions: [
      { title: 'Удалить любой билет заданного типа', method: 'DELETE', inputs: [{ label: 'Тип', values: TICKET_TYPES }],
        path: ([type]) => `${BASE}/ticket-service/tickets/actions/by-type/${type}` },
    ],
  },
  {
    title: 'Люди',
    url: `${BASE}/person-service/persons`,
    fields: [
      { name: 'firstName', label: 'Имя' },
      { name: 'lastName', label: 'Фамилия' },
      { name: 'birthday', label: 'Дата рождения (ГГГГ-ММ-ДД)' },
    ],
    filterFields: ['id', 'firstName', 'lastName', 'birthday'],
    columns: [
      ['id', p => p.id],
      ['Имя', p => p.firstName],
      ['Фамилия', p => p.lastName],
      ['Дата рождения', p => new Date(p.birthday).toLocaleDateString('ru-RU')],
    ],
    actions: [],
  },
  {
    title: 'Бронирования',
    url: `${BASE}/booking-service/booking/bookings`,
    fields: [
      { name: 'ticketId', label: 'id билета', number: true },
      { name: 'personId', label: 'id человека', number: true },
    ],
    filterFields: ['id', 'ticketId', 'personId', 'createdAt'],
    columns: [
      ['id', b => b.id],
      ['id билета', b => b.ticketId],
      ['id человека', b => b.personId],
      ['Создано', b => date(b.createdAt)],
    ],
    actions: [
      { title: 'Продать VIP-копию билета', method: 'POST', inputs: [{ label: 'id билета' }, { label: 'id человека' }],
        path: ([ticket, person]) => `${BASE}/booking-service/booking/sell/vip/${ticket}/${person}` },
      { title: 'Отменить все бронирования человека', method: 'DELETE', inputs: [{ label: 'id человека' }],
        path: ([person]) => `${BASE}/booking-service/booking/person/${person}/cancel` },
    ],
  },
];
