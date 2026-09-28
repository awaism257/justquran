// Al-Hussary (Murattal) per-verse audio via everyayah.com.

import { registerStopper, stopAllAudio } from '@/lib/audioBus';

export const AUDIO_BASE = 'https://everyayah.com/data/Husary_64kbps';
/** Mirror of the same everyayah files, tried once if the primary host fails. */
const AUDIO_BASE_FALLBACK = 'https://mirrors.quranicaudio.com/everyayah/Husary_64kbps';

/** e.g. verseUrl(2, 255) -> .../002255.mp3 */
export function verseUrl(s: number, v: number): string {
  const sss = String(s).padStart(3, '0');
  const vvv = String(v).padStart(3, '0');
  return `${AUDIO_BASE}/${sss}${vvv}.mp3`;
}

// ONE persistent <audio> element for the whole session — verse chains swap
// this element's .src instead of creating a new element per verse. The first
// play() runs inside the user's tap/click, which permanently "unlocks" this
// element, so later plays fired from the 'ended' handler (outside any
// gesture) are still allowed. A fresh element per verse is blocked by strict
// desktop autoplay policies, which killed chained recitation there.
let sharedAudio: HTMLAudioElement | null = null;
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

interface PlaySession {
  onEnd?: (reason?: EndReason) => void;
  onProgress?: (pct: number) => void;
  reported: boolean;
  objectUrl: string | null;
  // If the primary host fails, retry the verse once via the mirror before
  // declaring failure. Skipped for cached blob: URLs.
  triedFallback: boolean;
  // Stall watchdog state: last audible progress, user-initiated pause, timer.
  lastBeat: number;
  userPaused: boolean;
  watchdog: number | null;
}

let session: PlaySession | null = null;
// Bumped on every play/stop so async completions and media events from an
// earlier src are ignored.
let playGen = 0;

function report(reason: EndReason): void {
  const s = session;
  if (!s || s.reported) return;
  s.reported = true;
  s.onEnd?.(reason);
}

function clearPlaybackState(): void {
  current = null;
  currentVerse = null;
}

/** A verse that makes no audible progress for this long (and isn't paused)
 *  is a stalled connection. Host outages usually show up as a HANG — no
 *  'error' event ever fires — which used to freeze the chain silently right
 *  after the Bismillah until the next user gesture. The watchdog trips the
 *  same fallback a hard error would: retry once via the mirror, then fail. */
const STALL_MS = 12000;

function beat(s: PlaySession): void {
  s.lastBeat = Date.now();
}

/** Switch the failed/stalled verse to the mirror host once; otherwise fail. */
function fallbackOrFail(s: PlaySession): void {
  const a = sharedAudio;
  if (!a) return;
  const src = a.src;
  // Primary host failed and we haven't tried the mirror yet → retry once.
  if (!s.triedFallback && src.startsWith(AUDIO_BASE)) {
    s.triedFallback = true;
    beat(s);
    const mirrorSrc = AUDIO_BASE_FALLBACK + src.slice(AUDIO_BASE.length);
    a.src = mirrorSrc;
    void a.play().catch(() => {
      // A rejection for a src the element no longer holds is the OLD
      // primary's play() unwinding — never a failure of the mirror.
      if (session !== s || a.src !== mirrorSrc) return;
      clearPlaybackState();
      report('error');
    });
    return;
  }
  clearPlaybackState();
  report('error');
}

function armWatchdog(s: PlaySession): void {
  if (s.watchdog !== null) window.clearInterval(s.watchdog);
  beat(s);
  s.watchdog = window.setInterval(() => {
    const a = sharedAudio;
    if (session !== s || !a) {
      if (s.watchdog !== null) window.clearInterval(s.watchdog);
      s.watchdog = null;
      return;
    }
    if (s.userPaused) {
      beat(s); // user paused — never trips
      return;
    }
    if (Date.now() - s.lastBeat > STALL_MS) fallbackOrFail(s);
  }, 2000);
}

function disarmWatchdog(s: PlaySession | null): void {
  if (s && s.watchdog !== null) {
    window.clearInterval(s.watchdog);
    s.watchdog = null;
  }
}

function ensureAudio(): HTMLAudioElement {
  if (!sharedAudio) {
    sharedAudio = new Audio();
    sharedAudio.addEventListener('ended', () => {
      if (!session) return;
      clearPlaybackState();
      report('ended');
    });
    sharedAudio.addEventListener('error', () => {
      const s = session;
      if (!s || !sharedAudio) return; // element reset between plays — not a failure
      fallbackOrFail(s);
    });
    sharedAudio.addEventListener('playing', () => {
      const s = session;
      if (s) {
        s.userPaused = false;
        beat(s);
      }
    });
    sharedAudio.addEventListener('pause', () => {
      const s = session;
      if (s) s.userPaused = true;
    });
    sharedAudio.addEventListener('timeupdate', () => {
      const s = session;
      if (s) beat(s);
      if (s?.onProgress && sharedAudio && sharedAudio.duration > 0) {
        s.onProgress((sharedAudio.currentTime / sharedAudio.duration) * 100);
      }
    });
  }
  return sharedAudio;
}

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
  // Silence any other audio source (translation narration, and any previous
  // recitation on the shared element) before playing.
  stopAllAudio();
  const url = verseUrl(s, v);
  const audio = ensureAudio();
  const gen = ++playGen;
  const sess: PlaySession = {
    onEnd,
    onProgress,
    reported: false,
    objectUrl: null,
    triedFallback: false,
    lastBeat: Date.now(),
    userPaused: false,
    watchdog: null,
  };
  session = sess;
  armWatchdog(sess);
  current = audio;
  currentVerse = { s, v };
  lastVerse = { s, v };
  /** play() rejection handler that ignores STALE rejections: when the
      primary src errors, the 'error' event swaps in the mirror, and the old
      play() promise then rejects with NotSupportedError — that rejection
      must not fail the verse the fallback is already playing. (This stale
      rejection used to kill the whole chain right after the Bismillah.) */
  const playChecked = (src: string) => {
    audio.src = src;
    void audio.play().catch(() => {
      if (gen !== playGen) return;
      if (audio.src !== src) return; // stale — a fallback/new src owns the element
      clearPlaybackState();
      report('error');
    });
  };
  // Cache-first: if this verse was downloaded into Cache Storage, play the
  // cached copy (offline support); otherwise stream from the network.
  resolvePlayableUrl(url)
    .then((src) => {
      if (gen !== playGen) return;
      if (src !== url) {
        sess.objectUrl = src;
        currentObjectUrl = src;
      }
      playChecked(src);
    })
    .catch(() => {
      // Cache lookup failed entirely — fall back to plain streaming.
      if (gen !== playGen) return;
      playChecked(url);
    });
  return {
    audio,
    stop: () => {
      if (gen !== playGen) return;
      playGen++;
      disarmWatchdog(sess);
      session = null;
      clearPlaybackState();
      audio.pause();
      audio.removeAttribute('src');
      audio.load();
      if (sess.objectUrl) {
        URL.revokeObjectURL(sess.objectUrl);
        if (currentObjectUrl === sess.objectUrl) currentObjectUrl = null;
      }
      if (!sess.reported) {
        sess.reported = true;
        sess.onEnd?.('stopped');
      }
    },
  };
}

export function stopAudio() {
  playGen++;
  const s = session;
  disarmWatchdog(s);
  session = null;
  clearPlaybackState();
  if (sharedAudio) {
    sharedAudio.pause();
    sharedAudio.removeAttribute('src');
    sharedAudio.load();
  }
  if (s?.objectUrl) {
    URL.revokeObjectURL(s.objectUrl);
  } else if (currentObjectUrl) {
    URL.revokeObjectURL(currentObjectUrl);
  }
  currentObjectUrl = null;
}

export function isPlaying(): boolean {
  return !!current;
}

// Starting recitation anywhere in the app silences translation narration
// (and vice versa) — only one audio source plays at a time.
registerStopper(stopAudio);

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
