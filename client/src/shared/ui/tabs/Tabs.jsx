export function Tabs({ items, value, onChange }) {
    return (
        <nav aria-label="Ресурсы" className="mb-6 inline-flex max-w-full flex-wrap gap-1 rounded-xl border border-black/5 bg-[#e8e8e8] p-1">
            {items.map((item) => (
                <button
                    key={item.value}
                    type="button"
                    disabled={item.value === value}
                    aria-current={item.value === value ? 'page' : undefined}
                    onClick={() => onChange(item.value)}
                    className={`min-h-9 rounded-lg px-4 py-1.5 text-sm font-medium transition-colors duration-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent ${item.value === value ? 'bg-white text-ink shadow-[0_1px_3px_rgba(0,0,0,0.12)]' : 'text-[#626262] hover:bg-white/50 hover:text-ink active:bg-white/70'}`}
                >
                    {item.label}
                </button>
            ))}
        </nav>
    );
}
