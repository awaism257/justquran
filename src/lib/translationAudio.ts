// Translation narration audio (AI text-to-speech), streamed per verse from
// the project's own Cloudflare R2 bucket and cached on demand for offline.
//
// Sets (see public/data/translation-audio.json):
//   en/brian-allah — ClearQuran text, read by Brian (Microsoft Azure neural voice)
//   en/sonia-allah — ClearQuran text, read by Sonia (Microsoft Azure neural voice)
//   ur             — Jalandhari translation (public domain), Umbriel (Google Chirp3 HD)
//
// English sets carry a spoken Bismillah as {SSS}000.mp3 for every surah
// except 1 and 9 (matching the edition's print convention). The Urdu set has
// no separate Bismillah files; playback reuses ur/001001.mp3, exactly like
// the recitation player reuses Al-Hussary's 1:1.

import { loadBundle } from '@/lib/data';
import { registerStopper, stopAllAudio } from '@/lib/audioBus';

export type TranslationSetId = 'en/brian-allah' | 'en/sonia-allah' | 'ur';
export type BookLang = 'en' | 'ur';
export type EnglishVoice = 'brian' | 'sonia';

export interface TranslationSetInfo {
  lang: BookLang;
  voice: string;
  edition: string;
  format: string;
  surahs: Record<string, { v: number; bytes: number }>;
}

interface TranslationAudioManifest {
  base_url: string;
  sets: Record<string, TranslationSetInfo>;
}

let manifestPromise: Promise<TranslationAudioManifest> | null = null;

/** Loads the bundled audio manifest (base URL + per-surah sizes), once. */
export function loadTranslationManifest(): Promise<TranslationAudioManifest> {
  if (!manifestPromise) {
    manifestPromise = fetch(`${import.meta.env.BASE_URL}data/translation-audio.json`)
      .then((r) => {
        if (!r.ok) throw new Error(`Failed to load audio manifest: ${r.status}`);
        return r.json() as Promise<TranslationAudioManifest>;
      })
      .catch((e) => {
        manifestPromise = null;
        throw e;
      });
  }
  return manifestPromise;
}

export function setIdFor(lang: BookLang, voice: EnglishVoice): TranslationSetId {
  return lang === 'ur' ? 'ur' : voice === 'sonia' ? 'en/sonia-allah' : 'en/brian-allah';
}

export function setLabel(id: TranslationSetId): string {
  switch (id) {
    case 'en/brian-allah':
      return 'English · Brian';
    case 'en/sonia-allah':
      return 'English · Sonia';
    default:
      return 'Urdu · Jalandhari';
  }
}

let cachedBase: string | null = null;

/** Base URL for audio files. Falls back to the known public bucket URL. */
async function baseUrl(): Promise<string> {
  if (cachedBase) return cachedBase;
  try {
    const m = await loadTranslationManifest();
    cachedBase = m.base_url;
  } catch {
    cachedBase = 'https://pub-fafe102872f84521ab2a82e3dc2eeab0.r2.dev/';
  }
  return cachedBase;
}

/** e.g. verseUrl('ur', 2, 255) -> <base>ur/002255.mp3 */
export async function translationVerseUrl(
  set: TranslationSetId,
  s: number,
  v: number,
): Promise<string> {
  const sss = String(s).padStart(3, '0');
  const vvv = String(v).padStart(3, '0');
  return `${await baseUrl()}${set}/${sss}${vvv}.mp3`;
}

/** Spoken Bismillah that heads a surah (null for surahs 1 and 9). */
export async function bismillahUrl(set: TranslationSetId, s: number): Promise<string | null> {
  if (s === 1 || s === 9) return null;
  if (set === 'ur') return translationVerseUrl('ur', 1, 1);
  const sss = String(s).padStart(3, '0');
  return `${await baseUrl()}${set}/${sss}000.mp3`;
}

// ---- playback ----

let current: HTMLAudioElement | null = null;
let currentObjectUrl: string | null = null;
let onStopCallbacks = new Set<() => void>();

export type EndReason = 'ended' | 'stopped' | 'error';

export interface PlayHandle {
  stop: () => void;
  audio: HTMLAudioElement;
}

/** Stop whatever translation audio is playing (also called via the audio bus). */
export function stopTranslation(): void {
  if (current) {
    const a = current;
    current = null;
    a.pause();
    a.src = '';
  }
  if (currentObjectUrl) {
    URL.revokeObjectURL(currentObjectUrl);
    currentObjectUrl = null;
  }
  const cbs = onStopCallbacks;
  onStopCallbacks = new Set();
  cbs.forEach((cb) => {
    try {
      cb();
    } catch {
      /* non-fatal */
    }
  });
}

registerStopper(stopTranslation);

/** Returns an object URL for the cached copy of `url`, or the original URL. */
async function resolvePlayableUrl(url: string): Promise<string> {
  if (typeof caches === 'undefined') return url;
  const match = await caches.open(TRANS_AUDIO_CACHE).then((c) => c.match(url));
  if (!match) return url;
  const blob = await match.blob();
  return URL.createObjectURL(blob);
}

/**
 * Play a single translation file (verse or Bismillah), cache-first.
 * Stops any other audio (recitation included) via the audio bus.
 */
export function playTranslationFile(
  urlPromise: Promise<string>,
  onEnd?: (reason?: EndReason) => void,
  onProgress?: (pct: number) => void,
  onStopCb?: () => void,
): PlayHandle {
  stopTranslation();
  stopAllAudio(stopTranslation); // silence recitation etc.
  const audio = new Audio();
  current = audio;
  if (onStopCb) onStopCallbacks.add(onStopCb);
  let objectUrl: string | null = null;
  let reported = false;
  const report = (reason: EndReason) => {
    if (reported) return;
    reported = true;
    onEnd?.(reason);
  };
  urlPromise
    .then((url) => resolvePlayableUrl(url))
    .then((src) => {
      if (current !== audio) return;
      if (!src.startsWith('http')) {
        objectUrl = src;
        currentObjectUrl = src;
      }
      audio.src = src;
      void audio.play().catch(() => {
        if (current === audio) current = null;
        report('error');
      });
    })
    .catch(() => {
      if (current === audio) current = null;
      report('error');
    });
  audio.addEventListener('ended', () => {
    if (current === audio) current = null;
    report('ended');
  });
  audio.addEventListener('error', () => {
    if (current === audio) current = null;
    report('error');
  });
  if (onProgress) {
    audio.addEventListener('timeupdate', () => {
      if (audio.duration > 0) onProgress((audio.currentTime / audio.duration) * 100);
    });
  }
  return {
    audio,
    stop: () => {
      if (current === audio) current = null;
      audio.pause();
      audio.src = '';
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
        if (currentObjectUrl === objectUrl) currentObjectUrl = null;
      }
      report('stopped');
    },
  };
}

// ---- offline cache (Cache Storage) ----

/** Dedicated cache bucket for downloaded translation narration. */
export const TRANS_AUDIO_CACHE = 'justquran-trans-audio-v1';

const LS_PREFIX = 'jq-trans-audio-status-v1:';

export function loadDownloadStatuses(set: TranslationSetId): Record<number, true> {
  try {
    const raw = localStorage.getItem(LS_PREFIX + set);
    if (!raw) return {};
    const parsed = JSON.parse(raw) as Record<string, true>;
    const out: Record<number, true> = {};
    for (const k of Object.keys(parsed)) out[Number(k)] = true;
    return out;
  } catch {
    return {};
  }
}

export function saveDownloadStatuses(set: TranslationSetId, done: Record<number, true>): void {
  try {
    localStorage.setItem(LS_PREFIX + set, JSON.stringify(done));
  } catch {
    /* storage full/unavailable — non-fatal */
  }
}

/**
 * Download one surah's narration (Bismillah included where the print has one)
 * into Cache Storage. Returns the number of files that failed and were skipped.
 */
export async function downloadSurahAudio(
  set: TranslationSetId,
  surah: number,
  onProgress?: (done: number, total: number) => void,
  isCancelled?: () => boolean,
): Promise<number> {
  const bundle = await loadBundle();
  const meta = bundle.surahs.find((s) => s.n === surah);
  if (!meta) return 0;
  const cache = await caches.open(TRANS_AUDIO_CACHE);
  const urls: string[] = [];
  const b = await bismillahUrl(set, surah);
  if (b) urls.push(b);
  for (let v = 1; v <= meta.ayahs; v++) urls.push(await translationVerseUrl(set, surah, v));
  let skipped = 0;
  for (let i = 0; i < urls.length; i++) {
    if (isCancelled?.()) throw new Error('cancelled');
    const url = urls[i];
    if (!(await cache.match(url))) {
      let ok = false;
      for (let attempt = 0; attempt < 2 && !ok; attempt++) {
        try {
          const res = await fetch(url);
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          await cache.put(url, res);
          ok = true;
        } catch {
          if (isCancelled?.()) throw new Error('cancelled');
          if (attempt === 1) skipped++;
        }
      }
    }
    onProgress?.(i + 1, urls.length);
  }
  return skipped;
}

/** Remove one surah's downloaded narration from the cache. */
export async function deleteSurahAudio(set: TranslationSetId, surah: number): Promise<void> {
  if (typeof caches === 'undefined') return;
  const bundle = await loadBundle();
  const meta = bundle.surahs.find((s) => s.n === surah);
  if (!meta) return;
  const cache = await caches.open(TRANS_AUDIO_CACHE);
  const b = await bismillahUrl(set, surah);
  if (b) await cache.delete(b);
  for (let v = 1; v <= meta.ayahs; v++) {
    await cache.delete(await translationVerseUrl(set, surah, v));
  }
}

/** Wipe the whole translation-audio cache bucket. */
export async function deleteAllTranslationAudio(): Promise<void> {
  try {
    await caches.delete(TRANS_AUDIO_CACHE);
  } catch {
    /* best effort */
  }
}
