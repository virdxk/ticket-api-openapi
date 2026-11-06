import { useState } from 'react';
import { List } from './List.jsx';
import { Item } from './Item.jsx';
import { Action } from './Action.jsx';

export function Resource({ cfg }) {
    const [reloadKey, setReloadKey] = useState(0);
    const onChanged = () => setReloadKey((key) => key + 1);
    return (
        <>
            <List cfg={cfg} reloadKey={reloadKey} />
            <Item cfg={cfg} onChanged={onChanged} />
            {cfg.actions.map((action) => (
                <Action key={action.title} action={action} onChanged={onChanged} />
            ))}
        </>
    );
}
