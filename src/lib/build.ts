/**
 * Single source of truth for the visible build tag (About & Settings →
 * Diagnostics). ship_web_justquran.py bumps this alongside public/sw.js
 * CACHE_VERSION so the in-app "Build:" line always matches the deployed
 * service-worker cache version.
 */
export const BUILD_TAG = 'v119';
