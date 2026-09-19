const CACHE_VERSION = 'justquran-v95';

/*
 * ⚠️ PROJECT RULE — READ BEFORE EDITING ⚠️
 * After ANY web change, bump CACHE_VERSION (must remain the first line).
 * The activate handler below deletes any cache not matching the current
 * version, so bumping the version is what rolls out new app code to
 * installed PWAs. Audio lives in the separate 'justquran-audio-v1' cache,
 * which is managed by the app itself and is intentionally preserved here.
 */

const APP_CACHE = CACHE_VERSION;
const AUDIO_CACHE = 'justquran-audio-v1';

// App shell + fully-offline assets (text, fonts, icons) precached on install.
// NOTE: paths are relative (not '/...') so they resolve correctly both at
// the site root (Netlify PWA) and under the Android WebView base
// (/assets/www/) — relative URLs in a service worker resolve against the
// worker's own location.
const PRECACHE_URLS = [
  'index.html',
  'manifest.webmanifest',
  'fonts/amiri-quran.ttf',
  'fonts/digitalkhatt-indopak-v2.otf',
  'fonts/nastaliq.ttf',
  'fonts/noto-naskh.ttf',
  'data/quran-bundle.json',
  'icons/icon-192.png',
  'icons/icon-512.png',
  'icons/icon-maskable-512.png',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(APP_CACHE)
      .then((cache) => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((key) => key !== APP_CACHE && key !== AUDIO_CACHE)
            .map((key) => caches.delete(key)),
        ),
      )
      .then(() => self.clients.claim()),
  );
});

// Cache-first helper that also checks the audio cache (downloads made by the
// app are written there; match by request URL across all caches).
async function cacheFirstAcrossCaches(request, options) {
  const cached = await caches.match(request, options);
  if (cached) return cached;
  const response = await fetch(request);
  if (response && response.ok) {
    const cache = await caches.open(APP_CACHE);
    cache.put(request, response.clone());
  }
  return response;
}

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);

  // Recitation audio (everyayah.com): cache-first; the app pre-populates the
  // justquran-audio-v1 cache for downloaded surahs. Anything streamed while
  // online is stored in the app cache for future offline plays.
  if (url.origin === 'https://everyayah.com') {
    event.respondWith(cacheFirstAcrossCaches(request));
    return;
  }

  // Same-origin requests only below.
  if (url.origin !== self.location.origin) return;

  // SPA navigation: network first, fall back to the cached shell when offline.
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response && response.ok) {
            const clone = response.clone();
            caches.open(APP_CACHE).then((cache) => cache.put('/index.html', clone));
          }
          return response;
        })
        .catch(() => caches.match('index.html')),
    );
    return;
  }

  // Static offline assets: fonts, data bundle, icons, manifest — cache-first.
  if (
    url.pathname.startsWith('/fonts/') ||
    url.pathname.startsWith('/data/') ||
    url.pathname.startsWith('/icons/') ||
    url.pathname === '/manifest.webmanifest'
  ) {
    event.respondWith(cacheFirstAcrossCaches(request));
    return;
  }

  // Everything else (built JS/CSS assets): cache-first with network fill.
  event.respondWith(
    caches.match(request).then((cached) => {
      if (cached) return cached;
      return fetch(request).then((response) => {
        if (response && response.ok) {
          const clone = response.clone();
          caches.open(APP_CACHE).then((cache) => cache.put(request, clone));
        }
        return response;
      });
    }),
  );
});
