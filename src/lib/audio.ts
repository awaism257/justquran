// Al-Hussary (Murattal) per-verse audio via everyayah.com.

export const AUDIO_BASE = 'https://everyayah.com/data/Husary_64kbps';
/** Mirror of the same everyayah files, tried once if the primary host fails. */
const AUDIO_BASE_FALLBACK = 'https://mirrors.quranicaudio.com/everyayah/Husary_64kbps';

/** e.g. verseUrl(2, 255) -> .../002255.mp3 */
export function verseUrl(s: number, v: number): string {
  const sss = String(s).padStart(3, '0');
  const vvv = String(v).padStart(3, '0');
  return `${AUDIO_BASE}/${sss}${vvv}.mp3`;
}

let current: HTMLAudioElement | null = null;
let currentObjectUrl: string | null = null;

// Module-level store of the verse currently playing (or most recently played).
// Used to keep the view on the playing verse across folio↔card mode switches.
let currentVerse: { s: number; v: number } | null = null;
let lastVerse: { s: number; v: number } | null = null;

/** The verse whose audio is playing right now, or null when idle. */
export function getPlayingVerse(): { s: number; v: number } | null {
  return currentVerse;
}

/** The most recently played verse this session (survives stop/unmount). */
export function getLastPlayedVerse(): { s: number; v: number } | null {
  return lastVerse;
}

export interface PlayHandle {
  stop: () => void;
  audio: HTMLAudioElement;
}

/** Why playback ended: 'ended' = natural end, 'stopped' = user/replaced, 'error' = failure. */
export type EndReason = 'ended' | 'stopped' | 'error';

/**
 * Stream (or play from cache) a single verse.
 * onEnd is called when playback finishes (reason 'ended') or is stopped/replaced
 * (reason 'stopped') or fails (reason 'error').
 */
export function playVerse(
  s: number,
  v: number,
  onEnd?: (reason?: EndReason) => void,
  onProgress?: (pct: number) => void,
): PlayHandle {
  stopAudio();
  const url = verseUrl(s, v);
  const audio = new Audio();
  current = audio;
  currentVerse = { s, v };
  lastVerse = { s, v };
  let objectUrl: string | null = null;
  // onEnd must fire exactly once per playVerse call (a rejected play() promise
  // and the 'error' event can both fire for the same failure).
  let reported = false;
  const report = (reason: EndReason) => {
    if (reported) return;
    reported = true;
    onEnd?.(reason);
  };
  // If the primary host is unreachable/down, retry the verse once via the
  // mirror before declaring failure. Skipped for cached blob: URLs.
  let triedFallback = false;
  // Cache-first: if this verse was downloaded into Cache Storage, play the
  // cached copy (offline support); otherwise stream from the network.
  resolvePlayableUrl(url)
    .then((src) => {
      if (current !== audio) return;
      if (src !== url) objectUrl = src;
      audio.src = src;
      void audio.play().catch(() => {
        if (reported) return;
        if (current === audio) {
          current = null;
          currentVerse = null;
        }
        report('error');
      });
    })
    .catch(() => {
      // Cache lookup failed entirely — fall back to plain streaming.
      if (current !== audio) return;
      audio.src = url;
      void audio.play().catch(() => {
        if (reported) return;
        if (current !== audio) {
          current = null;
          currentVerse = null;
        }
        report('error');
      });
    });
  audio.addEventListener('ended', () => {
    if (current === audio) {
      current = null;
      currentVerse = null;
    }
    report('ended');
  });
  audio.addEventListener('error', () => {
    // Primary host failed and we haven't tried the mirror yet → retry once.
    if (!triedFallback && audio.src.startsWith(AUDIO_BASE)) {
      triedFallback = true;
      audio.src = AUDIO_BASE_FALLBACK + audio.src.slice(AUDIO_BASE.length);
      void audio.play().catch(() => {
        /* a further 'error' event follows and is reported below */
      });
      return;
    }
    if (current === audio) {
      current = null;
      currentVerse = null;
    }
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
      if (current === audio) {
        current = null;
        currentVerse = null;
      }
      audio.pause();
      audio.src = '';
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      report('stopped');
    },
  };
}

export function stopAudio() {
  if (current) {
    const a = current;
    current = null;
    currentVerse = null;
    a.pause();
    a.src = '';
  }
  if (currentObjectUrl) {
    URL.revokeObjectURL(currentObjectUrl);
    currentObjectUrl = null;
  }
}

export function isPlaying(): boolean {
  return !!current;
}

// ---- Offline cache (Cache Storage) ----

/** Dedicated Cache Storage bucket for downloaded recitation audio. */
export const AUDIO_CACHE = 'justquran-audio-v1';

/** Returns an object URL for the cached copy of `url`, or the original URL if not cached. */
async function resolvePlayableUrl(url: string): Promise<string> {
  if (typeof caches === 'undefined') return url;
  const match = await caches.open(AUDIO_CACHE).then((c) => c.match(url));
  if (!match) return url;
  const blob = await match.blob();
  return URL.createObjectURL(blob);
}

/** True if the given verse's audio is present in the offline cache. */
export async function isVerseCached(s: number, v: number): Promise<boolean> {
  if (typeof caches === 'undefined') return false;
  const match = await caches.open(AUDIO_CACHE).then((c) => c.match(verseUrl(s, v)));
  return !!match;
}
