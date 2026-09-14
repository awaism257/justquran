// Bookmarks + last-read position, localStorage only. No account, no sync.

const BM_KEY = 'jq-bookmarks';
const LAST_KEY = 'jq-last-read';

export interface VerseRef {
  s: number;
  v: number;
}

function readBookmarks(): VerseRef[] {
  try {
    const raw = localStorage.getItem(BM_KEY);
    if (!raw) return [];
    const arr = JSON.parse(raw);
    return Array.isArray(arr) ? arr.filter((x) => x && typeof x.s === 'number' && typeof x.v === 'number') : [];
  } catch {
    return [];
  }
}

function write(list: VerseRef[]) {
  try {
    localStorage.setItem(BM_KEY, JSON.stringify(list));
    window.dispatchEvent(new Event('jq-bookmarks-changed'));
  } catch {
    /* storage unavailable */
  }
}

export function getBookmarks(): VerseRef[] {
  return readBookmarks();
}

export function isBookmarked(s: number, v: number): boolean {
  return readBookmarks().some((b) => b.s === s && b.v === v);
}

export function toggleBookmark(s: number, v: number): boolean {
  const list = readBookmarks();
  const i = list.findIndex((b) => b.s === s && b.v === v);
  let added: boolean;
  if (i >= 0) {
    list.splice(i, 1);
    added = false;
  } else {
    list.push({ s, v });
    added = true;
  }
  write(list);
  return added;
}

export function removeBookmark(s: number, v: number) {
  write(readBookmarks().filter((b) => !(b.s === s && b.v === v)));
}

// ---- last read ----
export interface LastRead extends VerseRef {
  at: number; // timestamp
}

export function getLastRead(): LastRead | null {
  try {
    const raw = localStorage.getItem(LAST_KEY);
    if (!raw) return null;
    const o = JSON.parse(raw);
    return o && typeof o.s === 'number' && typeof o.v === 'number' ? (o as LastRead) : null;
  } catch {
    return null;
  }
}

export function setLastRead(s: number, v: number) {
  try {
    localStorage.setItem(LAST_KEY, JSON.stringify({ s, v, at: Date.now() }));
  } catch {
    /* storage unavailable */
  }
}
