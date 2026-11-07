export function Input({ className = '', invalid = false, type = 'text', ...props }) {
    const state = invalid
        ? 'border-error focus:border-error focus:ring-error/20'
        : 'border-line focus:border-accent focus:ring-accent/20';
    const appearance = type === 'date' ? 'appearance-auto' : 'appearance-none';
    return (
        <input
            type={type}
            aria-invalid={invalid || undefined}
            className={`h-9 min-w-0 box-border rounded-lg border bg-white px-3 py-0 text-sm leading-5 text-ink transition-[border-color,box-shadow] duration-100 placeholder:text-muted focus:outline-none focus:ring-3 disabled:bg-canvas disabled:text-muted ${appearance} ${state} ${className}`}
            {...props}
        />
    );
}
