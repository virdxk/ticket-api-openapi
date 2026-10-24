import { useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Resource } from './Resource.jsx';
import { resources } from './resources.js';
import './styles.css';

function App() {
  const [tab, setTab] = useState(0);
  return (
    <>
      <h1>Билеты, люди, бронирования</h1>
      <nav>
        {resources.map((r, i) => <button key={r.title} disabled={i === tab} onClick={() => setTab(i)}>{r.title}</button>)}
      </nav>
      <Resource key={tab} cfg={resources[tab]} />
    </>
  );
}

createRoot(document.getElementById('root')).render(<App />);
