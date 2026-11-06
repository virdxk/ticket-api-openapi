import { Button } from '../button';

export function Pagination({ page, totalPages, totalElements, onChange }) {
    return (
        <nav aria-label="Страницы списка" className="my-4 flex flex-wrap items-center gap-3 text-sm text-muted">
            <Button variant="secondary" disabled={page <= 1} onClick={() => onChange(page - 1)}>
                Назад
            </Button>
            <span>Страница {page} из {totalPages}, всего {totalElements}</span>
            <Button variant="secondary" disabled={page >= totalPages} onClick={() => onChange(page + 1)}>
                Вперёд
            </Button>
        </nav>
    );
}
