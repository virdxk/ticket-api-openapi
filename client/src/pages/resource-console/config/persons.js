import { API_BASE as BASE } from '../api/resource-api.js';
import { birthday } from '../lib/format.js';

export const persons = {
    title: 'Люди',
    url: `${BASE}/person-service/persons`,
    fields: [
        {
            name: 'firstName',
            label: 'Имя',
            required: true,
            minLength: 1,
            placeholder: 'Не менее 1 символа',
        },
        {
            name: 'lastName',
            label: 'Фамилия',
            required: true,
            minLength: 1,
            placeholder: 'Не менее 1 символа',
        },
        {
            name: 'birthday',
            label: 'Дата рождения',
            required: true,
            format: 'date',
            notAfterToday: true,
            placeholder: 'ГГГГ-ММ-ДД, не позже сегодня',
        },
    ],
    filterFields: [
        { value: 'id', label: 'Номер человека' },
        { value: 'firstName', label: 'Имя' },
        { value: 'lastName', label: 'Фамилия' },
        { value: 'birthday', label: 'Дата рождения' },
    ],
    columns: [
        ['id', (p) => p.id],
        ['Имя', (p) => p.firstName],
        ['Фамилия', (p) => p.lastName],
        ['Дата рождения', (p) => birthday(p.birthday)],
    ],
    actions: [],
};
