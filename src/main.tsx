import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import './index.css';
import App from './App.tsx';
import { SettingsProvider } from '@/lib/settings';

createRoot(document.getElementById('root')!).render(
  <BrowserRouter>
    <SettingsProvider>
      <App />
    </SettingsProvider>
  </BrowserRouter>,
);

// Register the service worker (production only) for full offline support.
// Update hardening (iOS home-screen PWAs are slow to notice new versions):
//  1. updateViaCache:'none' — never let the HTTP cache serve a stale sw.js.
//  2. Proactive reg.update() on every load AND on return to foreground.
//  3. controllerchange → reload once to swap in the new app shell
//     (guard prevents reload loops).
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  let refreshing = false;
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (refreshing) return;
    refreshing = true;
    location.reload();
  });

  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('./sw.js', { updateViaCache: 'none' })
      .then((reg) => {
        const checkForUpdate = () => reg.update().catch(() => {});
        checkForUpdate();
        document.addEventListener('visibilitychange', () => {
          if (document.visibilityState === 'visible') checkForUpdate();
        });
      })
      .catch(() => {
        // Offline support unavailable; app still works online.
      });
  });
}
