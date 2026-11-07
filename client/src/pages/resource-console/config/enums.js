export const TICKET_TYPES = [
    { value: 'VIP', label: 'VIP' },
    { value: 'USUAL', label: 'Обычный' },
    { value: 'BUDGETARY', label: 'Бюджетный' },
    { value: 'CHEAP', label: 'Дешёвый' },
];

export const VENUE_TYPES = [
    { value: 'PUB', label: 'Паб' },
    { value: 'BAR', label: 'Бар' },
    { value: 'OPEN_AREA', label: 'Открытая площадка' },
    { value: 'MALL', label: 'Торговый центр' },
    { value: 'STADIUM', label: 'Стадион' },
];

const ENUM_LABELS = new Map([...TICKET_TYPES, ...VENUE_TYPES].map((option) => [option.value, option.label]));

export function enumLabel(value) {
    return ENUM_LABELS.get(value) ?? String(value);
}
