// Supplied (do not edit). Dev server for the browser client: `npm run dev`, then open the printed URL.
// API calls go to the same origin (for example fetch('/payment-requests')) and Vite forwards them to
// your Spring API. Set API_BASE_URL to point somewhere other than http://localhost:8080.
import { defineConfig } from 'vite';

const api = process.env.API_BASE_URL || 'http://localhost:8080';
const routes = ['/payment-requests', '/approval-queue', '/exception-cases', '/auth', '/me', '/actuator'];

export default defineConfig({
  server: {
    port: 5173,
    proxy: Object.fromEntries(routes.map((route) => [route, { target: api, changeOrigin: true }])),
  },
});
