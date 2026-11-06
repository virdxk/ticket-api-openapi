import { API_BASE as BASE } from '../api/resource-api.js';
import { date } from '../lib/format.js';

export const bookings = {
    title: 'Бронирования',
    url: `${BASE}/booking-service/booking/bookings`,
    fields: [
        {
            name: 'ticketId',
            label: 'Номер билета',
            required: true,
            number: true,
            integer: true,
            minimum: 1,
            placeholder: 'Целое число от 1',
        },
        {
            name: 'personId',
            label: 'Номер человека',
            required: true,
            number: true,
            integer: true,
            minimum: 1,
            placeholder: 'Целое число от 1',
        },
    ],
    filterFields: [
        { value: 'id', label: 'Номер бронирования' },
        { value: 'ticketId', label: 'Номер билета' },
        { value: 'personId', label: 'Номер человека' },
        { value: 'createdAt', label: 'Дата создания' },
    ],
    columns: [
        ['id', (b) => b.id],
        ['Номер билета', (b) => b.ticketId],
        ['Номер человека', (b) => b.personId],
        ['Создано', (b) => date(b.createdAt)],
    ],
    actions: [
        {
            title: 'Продать VIP-копию билета',
            method: 'POST',
            inputs: [
                {
                    label: 'Номер билета',
                    number: true,
                    integer: true,
                    minimum: 1,
                    placeholder: 'Целое число от 1',
                },
                {
                    label: 'Номер человека',
                    number: true,
                    integer: true,
                    minimum: 1,
                    placeholder: 'Целое число от 1',
                },
            ],
            path: ([ticket, person]) => `${BASE}/booking-service/booking/sell/vip/${ticket}/${person}`,
        },
        {
            title: 'Отменить все бронирования человека',
            method: 'DELETE',
            inputs: [
                {
                    label: 'Номер человека',
                    number: true,
                    integer: true,
                    minimum: 1,
                    placeholder: 'Целое число от 1',
                },
            ],
            path: ([person]) => `${BASE}/booking-service/booking/person/${person}/cancel`,
        },
    ],
};
