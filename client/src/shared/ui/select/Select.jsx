import * as SelectPrimitive from '@radix-ui/react-select';

export function Select({ id, value, onValueChange, options, disabled, required = false, invalid = false, className = '', ...props }) {
    const state = invalid
        ? 'border-error focus:border-error focus:ring-error/20'
        : 'border-line focus:border-accent focus:ring-accent/20';
    return (
        <SelectPrimitive.Root value={`value:${value}`} onValueChange={(value) => onValueChange(value.slice(6))} disabled={disabled}>
            <SelectPrimitive.Trigger
                id={id}
                aria-required={required || undefined}
                aria-invalid={invalid || undefined}
                className={`flex h-9 min-w-0 box-border items-center justify-between gap-2 rounded-lg border bg-white px-3 text-left text-sm leading-5 text-ink transition-[border-color,box-shadow] duration-100 focus:outline-none focus:ring-3 disabled:bg-canvas disabled:text-muted [&>span:first-child]:truncate ${state} ${className}`}
                {...props}
            >
                <SelectPrimitive.Value />
                <SelectPrimitive.Icon asChild>
                    <svg aria-hidden="true" className="size-4 shrink-0 text-muted" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                        <path d="m6 9 6 6 6-6" />
                    </svg>
                </SelectPrimitive.Icon>
            </SelectPrimitive.Trigger>
            <SelectPrimitive.Portal>
                <SelectPrimitive.Content
                    position="popper"
                    sideOffset={6}
                    collisionPadding={8}
                    className="z-50 max-h-[min(20rem,var(--radix-select-content-available-height))] w-[var(--radix-select-trigger-width)] overflow-hidden rounded-xl border border-line bg-white p-1 shadow-lg"
                >
                    <SelectPrimitive.ScrollUpButton className="flex h-6 items-center justify-center bg-white text-muted">
                        <svg aria-hidden="true" className="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round"><path d="m6 15 6-6 6 6" /></svg>
                    </SelectPrimitive.ScrollUpButton>
                    <SelectPrimitive.Viewport>
                        {options.map((option) => (
                            <SelectPrimitive.Item
                                key={option.value}
                                value={`value:${option.value}`}
                                className="relative flex min-h-9 cursor-default select-none items-center rounded-lg py-2 pr-3 pl-8 text-sm leading-5 text-ink outline-none data-[highlighted]:bg-canvas data-[state=checked]:font-medium"
                            >
                                <SelectPrimitive.ItemIndicator className="absolute left-2 flex items-center">
                                    <svg aria-hidden="true" className="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round"><path d="m5 12 4 4L19 6" /></svg>
                                </SelectPrimitive.ItemIndicator>
                                <SelectPrimitive.ItemText>{option.label}</SelectPrimitive.ItemText>
                            </SelectPrimitive.Item>
                        ))}
                    </SelectPrimitive.Viewport>
                    <SelectPrimitive.ScrollDownButton className="flex h-6 items-center justify-center bg-white text-muted">
                        <svg aria-hidden="true" className="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round"><path d="m6 9 6 6 6-6" /></svg>
                    </SelectPrimitive.ScrollDownButton>
                </SelectPrimitive.Content>
            </SelectPrimitive.Portal>
        </SelectPrimitive.Root>
    );
}
