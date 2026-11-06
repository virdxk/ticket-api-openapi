const variants = {
    primary: 'border-transparent bg-accent text-white enabled:hover:bg-[#3a3a3a] enabled:active:bg-[#171717]',
    secondary: 'border-line bg-white text-ink enabled:hover:bg-[#f5f5f5] enabled:active:bg-[#e8e8e8]',
    danger: 'border-line bg-canvas text-ink enabled:hover:bg-[#e8e8e8] enabled:active:bg-[#dedede]',
};

export function Button({ variant = 'secondary', className = '', type = 'button', ...props }) {
    return (
        <button
            type={type}
            className={`inline-flex h-9 box-border items-center justify-center rounded-lg border px-3 py-0 text-sm leading-5 font-medium shadow-[0_1px_2px_rgba(0,0,0,0.04)] transition-colors duration-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant]} ${className}`}
            {...props}
        />
    );
}
