export function RemoveButton({ label, onClick, className = '' }) {
    return (
        <button
            type="button"
            aria-label={label}
            title={label}
            className={`inline-flex size-9 items-center justify-center justify-self-end rounded-lg border border-line bg-white text-muted hover:bg-canvas hover:text-ink active:bg-black/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent ${className}`}
            onClick={onClick}
        >
            <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                <path d="M3 6h18M9 6V4h6v2M5 6l1 14h12l1-14M10 10v6M14 10v6" />
            </svg>
        </button>
    );
}
