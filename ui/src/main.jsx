// Starter (yours to edit). Mounts the app inside a browser router. Keep providers (for example an
// AuthProvider) INSIDE <App />: the supplied checks render <App /> inside their own router.
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import App from './App.jsx';
import './styles.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
);
