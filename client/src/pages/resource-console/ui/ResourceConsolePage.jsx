import { useState } from 'react';
import { Tabs } from '../../../shared/ui/index.js';
import { resources } from '../config/resources.js';
import { Resource } from './Resource.jsx';

export function ResourceConsolePage() {
    const [tab, setTab] = useState(0);
    return (
        <main className="mx-auto max-w-[1100px] px-4 py-6 sm:px-6 sm:py-9">
            <h1 className="mb-5 text-2xl font-semibold tracking-[-0.025em] text-ink sm:text-[28px]">Билеты, люди, бронирования</h1>
            <Tabs items={resources.map((resource, value) => ({ value, label: resource.title }))} value={tab} onChange={setTab} />
            <Resource key={tab} cfg={resources[tab]} />
        </main>
    );
}
