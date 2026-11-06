import { API_BASE as BASE } from '../api/resource-api.js';
import { show, date, venue } from '../lib/format.js';
const TICKET_TYPES = ['VIP', 'USUAL', 'BUDGETARY', 'CHEAP'];
const VENUE_TYPES = ['PUB', 'BAR', 'OPEN_AREA', 'MALL', 'STADIUM'];

export const tickets = {
    title: 'Билеты',
    url: `${BASE}/ticket-service/tickets`,
    fields: [
        {
            name: 'name',
            label: 'Название',
            required: true,
            minLength: 1,
            placeholder: 'Не менее 1 символа',
        },
        {
            name: 'coordinates.x',
            label: 'Координата X',
            required: true,
            number: true,
            placeholder: 'Число',
        },
        {
            name: 'coordinates.y',
            label: 'Координата Y',
            required: true,
            number: true,
            maximum: 747,
            placeholder: 'Число не более 747',
        },
        {
            name: 'price',
            label: 'Цена',
            number: true,
            exclusiveMinimum: 0,
            placeholder: 'Число больше 0',
        },
        {
            name: 'comment',
            label: 'Комментарий',
            required: true,
            maxLength: 681,
            placeholder: 'Не более 681 символа',
        },
        {
            name: 'type',
            label: 'Тип',
            values: TICKET_TYPES,
        },
        {
            name: 'venue',
            label: 'Площадка',
            placeholder: 'Пусто — без площадки',
        },
        {
            name: 'venue.name',
            label: 'Площадка: название',
            required: true,
            minLength: 1,
            placeholder: 'Не менее 1 символа',
        },
        {
            name: 'venue.capacity',
            label: 'Площадка: вместимость',
            required: true,
            number: true,
            integer: true,
            minimum: 1,
            placeholder: 'Целое число от 1',
        },
        {
            name: 'venue.type',
            label: 'Площадка: тип',
            required: true,
            values: VENUE_TYPES,
        },
    ],
    filterFields: [
        { value: 'id', label: 'Номер билета' },
        { value: 'name', label: 'Название' },
        { value: 'coordinates.x', label: 'Координата X' },
        { value: 'coordinates.y', label: 'Координата Y' },
        { value: 'creationDate', label: 'Дата создания' },
        { value: 'price', label: 'Цена' },
        { value: 'comment', label: 'Комментарий' },
        { value: 'type', label: 'Тип билета' },
        { value: 'venue.id', label: 'Номер площадки' },
        { value: 'venue.name', label: 'Название площадки' },
        { value: 'venue.capacity', label: 'Вместимость площадки' },
        { value: 'venue.type', label: 'Тип площадки' },
    ],
    columns: [
        ['id', (t) => t.id],
        ['Название', (t) => t.name],
        ['Координаты', (t) => `(${t.coordinates.x}; ${t.coordinates.y})`],
        ['Создан', (t) => date(t.creationDate)],
        ['Цена', (t) => show(t.price)],
        ['Комментарий', (t) => t.comment],
        ['Тип', (t) => show(t.type)],
        ['Площадка', (t) => venue(t.venue)],
    ],
    actions: [
        {
            title: 'Удалить любой билет заданного типа',
            method: 'DELETE',
            inputs: [
                {
                    label: 'Тип',
                    values: TICKET_TYPES,
                },
            ],
            path: ([type]) => `${BASE}/ticket-service/tickets/actions/by-type/${type}`,
        },
    ],
};
