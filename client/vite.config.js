import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { defineConfig } from 'vite';

const target = process.env.VITE_API_TARGET ?? 'https://se.ifmo.ru';
const proxy = Object.fromEntries(
  ['/ticket-service', '/person-service', '/booking-service'].map((prefix) => [
    prefix,
    { target, secure: false, changeOrigin: true },
  ]),
);

export default defineConfig({
  base: './',
  plugins: [react(), tailwindcss()],
  server: { proxy },
});
