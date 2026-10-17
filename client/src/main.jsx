import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { createRoot } from 'react-dom/client';
import { ResourceTab } from './lib.jsx';
import { tickets, persons, bookings } from './resources.jsx';
import './styles.css';

const TABS = [
  { id: 'tickets', title: 'Билеты', cfg: tickets },
  { id: 'persons', title: 'Люди', cfg: persons },
  { id: 'bookings', title: 'Бронирования', cfg: bookings },
];

function App() {
  const [hash, setHash] = useState(location.hash);
  useEffect(() => {
    const sync = () => setHash(location.hash);
    window.addEventListener('hashchange', sync);
    return () => window.removeEventListener('hashchange', sync);
  }, []);
  const active = (TABS.find(t => `#${t.id}` === hash) || TABS[0]).id;
  return (
    <>
      {createPortal(TABS.map(t => (
        <button key={t.id} type="button" className={t.id === active ? 'tab active' : 'tab'} onClick={() => { location.hash = t.id; }}>{t.title}</button>
      )), document.getElementById('tabs'))}
      {TABS.map(t => <div key={t.id} hidden={t.id !== active}><ResourceTab cfg={t.cfg} active={t.id === active} /></div>)}
    </>
  );
}

createRoot(document.getElementById('main')).render(<App />);
