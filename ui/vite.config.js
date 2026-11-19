// Supplied with the ui/ scaffold. The dev server forwards API calls to your Spring API on port 8080,
// so the browser code calls relative paths such as /payment-requests (no CORS setup needed).
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const API = process.env.API_BASE_URL || 'http://localhost:8080';
const apiPaths = ['/payment-requests', '/approval-queue', '/exception-cases', '/auth', '/me'];

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: Object.fromEntries(apiPaths.map((path) => [path, { target: API, changeOrigin: true }])),
  },
});
