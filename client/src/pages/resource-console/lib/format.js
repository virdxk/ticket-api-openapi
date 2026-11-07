import { enumLabel } from '../config/enums.js';

const FIELD_LABELS = {
    id: 'Номер',
    name: 'Название',
    coordinates: {
        label: 'Координаты',
        fields: {
            x: 'Координата X',
            y: 'Координата Y',
        },
    },
    creationDate: 'Дата создания',
    price: 'Цена',
    comment: 'Комментарий',
    type: 'Тип',
    venue: {
        label: 'Площадка',
        fields: {
            id: 'Номер площадки',
            name: 'Название площадки',
            capacity: 'Вместимость площадки',
            type: 'Тип площадки',
        },
    },
    firstName: 'Имя',
    lastName: 'Фамилия',
    birthday: 'Дата рождения',
    ticketId: 'Номер билета',
    personId: 'Номер человека',
    createdAt: 'Дата создания',
    cancelledBookings: 'Отменено бронирований',
    items: 'Элементы',
    page: 'Страница',
    size: 'Размер страницы',
    totalElements: 'Всего элементов',
    totalPages: 'Всего страниц',
    message: 'Сообщение',
    time: 'Время',
    details: 'Детали',
};

export function fieldLabel(field) {
    let entry = { fields: FIELD_LABELS };
    for (const key of field.split('.')) {
        entry = entry?.fields?.[key];
    }
    return typeof entry === 'string' ? entry : entry?.label ?? field;
}

export function show(value) {
    return value === null || value === undefined ? '-' : enumLabel(value);
}

export function date(value) {
    return value ? new Date(value).toLocaleString('ru-RU') : '-';
}

export function birthday(value) {
    if (!value) {
        return '-';
    }
    const [y, m, d] = String(value).split('-');
    return `${d}.${m}.${y}`;
}

export function venue(value) {
    if (!value) {
        return '-';
    }
    return `${value.name}, ${enumLabel(value.type)}, мест: ${value.capacity} (номер ${value.id})`;
}
