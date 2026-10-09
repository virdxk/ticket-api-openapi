import { h } from './js/lib.js';
import { tickets, persons, bookings } from './js/resources.js';

const TABS = [
  { id: 'tickets', title: 'Билеты', create: tickets },
  { id: 'persons', title: 'Люди', create: persons },
  { id: 'bookings', title: 'Бронирования', create: bookings },
];

const nav = document.getElementById('tabs');
const main = document.getElementById('main');

for (const tab of TABS) {
  tab.view = tab.create();
  tab.panel = h('div', { hidden: true }, tab.view.element);
  tab.button = h('button', { type: 'button', class: 'tab', text: tab.title, onclick: () => { location.hash = tab.id; } });
  nav.append(tab.button);
  main.append(tab.panel);
}

function activate() {
  const tab = TABS.find(t => `#${t.id}` === location.hash) || TABS[0];
  for (const t of TABS) {
    t.panel.hidden = t !== tab;
    t.button.classList.toggle('active', t === tab);
  }
  if (!tab.loaded) {
    tab.loaded = true;
    tab.view.reload();
  }
}

window.addEventListener('hashchange', activate);
activate();
