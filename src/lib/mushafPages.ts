// Dynamic mushaf pagination: page boundaries are COMPUTED at the reader's
// chosen Arabic font scale, not taken from a fixed printed-page map. A page
// is a whole number of rendered lines, sliced out of the surah's flow at
// character offsets (always line-break points, i.e. word boundaries, so
// Arabic shaping is preserved), so nothing can ever clip, by construction.
//
// This module holds the pure measurement/pagination math plus two caches:
//  - paginations: full per-surah page data (slice points, per-page anchor
//    verses, verse→page map), keyed by (surah, frame size, font scale).
//  - counts: per-surah PAGE COUNTS only, for the global "Page X of Y" label.
//    Filled live (the surah being read is measured exactly) and by background
//    probes (see PagedFolio); persisted in localStorage per frame key.

/** A page boundary: the line starting here begins a new page. `char` is the
    offset within verse `verse`'s text (0 = the verse starts the page). */
export interface Slice {
  verse: number;
  char: number;
}

export interface Pagination {
  surah: number;
  /** number of pages */
  count: number;
  /** starts[p] = first line of page p (starts[0] is always verse 1, char 0) */
  starts: Slice[];
  /** anchors[i] = verse that owns the first line of page i (reading position). */
  anchors: number[];
  /** pageOfVerse[v] = 0-based page containing verse v's first line (v ≥ 1). */
  pageOfVerse: number[];
}

/** A measured line box (text-run rects merged per visual line). */
interface MeasuredLine {
  top: number;
  bottom: number;
  verse: number;
  /** char offset within `verse` where this line starts (0 when measured
      without char info, e.g. page-count probes) */
  char: number;
}

/**
 * Measure the rendered folio lines under `root` (the .folio-inner element of
 * a full-surah folio render). Line tops/bottoms are relative to the root's
 * own top. Verse text runs carry the verse number (the first run span of
 * each verse has id `v<N>`); offsets accumulate across a verse's runs so a
 * line start maps to a (verse, char) slice point. With `withChars`, the
 * exact first character of each line is found by binary search on Range
 * rects (line breaks only happen at spaces, so slice points are always word
 * boundaries and Arabic shaping is preserved when a page renders its slice).
 */
export function measureFlow(root: HTMLElement, withChars = true): MeasuredLine[] {
  const rootTop = root.getBoundingClientRect().top;
  const paras = root.querySelectorAll<HTMLElement>('.folio-ar');
  if (!paras.length) return [];
  const lineH = parseFloat(getComputedStyle(paras[0]).lineHeight) || 1;
  // Main text line boxes are ~2.5× the font size tall; the verse-badge digits
  // (~10px) and waqaf glyphs (~25px, absolutely positioned) also produce text
  // rects — filter anything well under a real line box.
  const minH = lineH * 0.6;
  const lines: MeasuredLine[] = [];
  let verse = 0;
  let charBase = 0; // offset of the current text node's first char in `verse`
  const range = document.createRange();
  paras.forEach((para) => {
    const walker = document.createTreeWalker(para, NodeFilter.SHOW_TEXT);
    let node: Text | null;
    while ((node = walker.nextNode() as Text | null)) {
      if (!node.data.trim()) continue; // separators never own a line
      const el = node.parentElement;
      if (!el || !el.classList.contains('verse-run')) continue;
      const m = el.id.match(/^v(\d+)$/);
      if (m) {
        verse = parseInt(m[1], 10);
        charBase = 0;
      }
      const nodeStart = charBase;
      charBase += node.data.length;
      range.selectNodeContents(node);
      const rects = range.getClientRects();
      for (let i = 0; i < rects.length; i++) {
        const r = rects[i];
        if (r.width < 0.5 || r.height < minH) continue;
        const top = r.top - rootTop;
        const bottom = r.bottom - rootTop;
        const last = lines[lines.length - 1];
        if (last && Math.abs(last.top - top) < lineH * 0.5) {
          if (bottom > last.bottom) last.bottom = bottom;
        } else {
          let char = nodeStart;
          if (withChars && node.data.length > 1) {
            // first char index whose box sits on THIS line
            let lo = 0;
            let hi = node.data.length - 1;
            while (lo < hi) {
              const mid = (lo + hi) >> 1;
              range.setStart(node, mid);
              range.setEnd(node, mid + 1);
              const cr = range.getClientRects();
              const ct = cr.length ? cr[0].top - rootTop : top;
              if (ct < top - lineH * 0.25) lo = mid + 1;
              else hi = mid;
            }
            range.selectNodeContents(node);
            char = nodeStart + lo;
          }
          lines.push({ top, bottom, verse, char });
        }
      }
    }
  });
  range.detach();
  return lines;
}

/** Greedy line-boundary pagination: page 0 starts at the flow top (surah
    band included); every later page starts at the first line that did not
    fit the previous page. A page therefore always holds a whole number of
    lines — clipping is impossible by construction. */
export function paginate(
  surah: number,
  lines: MeasuredLine[],
  availH: number,
  firstVerse: number,
  lastVerse: number,
): Pagination {
  const starts: Slice[] = [{ verse: firstVerse, char: 0 }];
  const anchors: number[] = [firstVerse];
  if (lines.length > 0 && availH > 0) {
    anchors[0] = Math.max(firstVerse, lines[0].verse);
    let start = 0;
    for (const line of lines) {
      if (line.bottom - start > availH + 0.5) {
        start = line.top;
        starts.push({ verse: line.verse, char: line.char });
        anchors.push(Math.max(firstVerse, line.verse));
      }
    }
  }
  // verse → page (page containing the verse's FIRST line)
  const count = starts.length;
  const pageOfVerse: number[] = new Array(lastVerse + 1).fill(0);
  let p = 0;
  let cur = firstVerse;
  for (const line of lines) {
    while (
      p < count - 1 &&
      (line.verse > starts[p + 1].verse ||
        (line.verse === starts[p + 1].verse && line.char >= starts[p + 1].char))
    ) {
      p++;
    }
    if (line.verse >= cur) {
      for (let v = cur; v <= line.verse; v++) pageOfVerse[v] = p;
      cur = line.verse + 1;
    }
  }
  for (let v = Math.max(firstVerse, cur); v <= lastVerse; v++) pageOfVerse[v] = count - 1;
  pageOfVerse[firstVerse - 1] = 0; // v=0 sentinel (Bismillah prelude) → page 0
  return { surah, count, starts, anchors, pageOfVerse };
}

/** Verses of one page, sliced from the surah's verse list at the page's
    char-offset boundaries. A verse whose END is not on this page is flagged
    `cutEnd` — FolioPage renders it without its number badge / waqaf marks
    (those render on the page holding the verse's end). */
export function sliceVerses<T extends { v: number; ar: string }>(
  verses: T[],
  pg: Pagination,
  page: number,
): (T & { cutEnd?: boolean })[] {
  const s0 = pg.starts[page] ?? { verse: 1, char: 0 };
  const s1 = page + 1 < pg.starts.length ? pg.starts[page + 1] : null;
  const endVerse = s1 ? (s1.char === 0 ? s1.verse - 1 : s1.verse) : verses.length;
  const out: (T & { cutEnd?: boolean })[] = [];
  for (let v = s0.verse; v <= endVerse && v <= verses.length; v++) {
    const vv = verses[v - 1];
    const cutStart = v === s0.verse ? s0.char : 0;
    const cutEnd = s1 && v === s1.verse ? s1.char : vv.ar.length;
    const cut = cutEnd < vv.ar.length;
    const ar = cutStart > 0 || cut ? vv.ar.slice(cutStart, cutEnd) : vv.ar;
    out.push(cut ? { ...vv, ar, cutEnd: true } : { ...vv, ar });
  }
  return out;
}

// ---- caches ---------------------------------------------------------------

const pagCache = new Map<string, Pagination>();

export function pagKey(surah: number, w: number, h: number, scale: number): string {
  return `${surah}@${Math.round(w)}x${Math.round(h)}#${scale}`;
}

export function getCachedPagination(key: string): Pagination | undefined {
  return pagCache.get(key);
}

export function cachePagination(key: string, pg: Pagination): void {
  if (pagCache.size > 240) pagCache.clear(); // session-scoped; keep it bounded
  pagCache.set(key, pg);
}

// ---- per-surah page counts (global "Page X of Y") --------------------------

const COUNTS_LS = 'jq-pcounts-v1';
let countsKey = '';
let counts: (number | null)[] = new Array(114).fill(null);
const listeners = new Set<() => void>();

function notify(): void {
  listeners.forEach((cb) => cb());
}

/** Start (or switch to) a frame session: loads persisted counts for this
    (width, height, scale) triple and notifies subscribers. */
export function beginCountsSession(key: string): void {
  if (key === countsKey) return;
  countsKey = key;
  counts = new Array(114).fill(null);
  try {
    const raw = localStorage.getItem(`${COUNTS_LS}:${key}`);
    if (raw) {
      const arr = JSON.parse(raw) as (number | null)[];
      if (Array.isArray(arr) && arr.length === 114) counts = arr;
    }
  } catch {
    /* storage unavailable or corrupt — start empty */
  }
  notify();
}

/** Record one surah's measured page count (1-based surah number). */
export function recordCount(surah: number, count: number): void {
  if (!countsKey || surah < 1 || surah > 114) return;
  if (counts[surah - 1] === count) return;
  counts = counts.slice();
  counts[surah - 1] = count;
  try {
    localStorage.setItem(`${COUNTS_LS}:${countsKey}`, JSON.stringify(counts));
  } catch {
    /* storage full/unavailable — counts still live in memory */
  }
  notify();
}

export function getCounts(): (number | null)[] {
  return counts;
}

export function subscribeCounts(cb: () => void): () => void {
  listeners.add(cb);
  return () => listeners.delete(cb);
}

/** Global (whole-Quran) 1-based index of page `pageIdx` (0-based) of `surah`,
    or null while an earlier surah's count is still unknown. */
export function globalPageIndex(countsArr: (number | null)[], surah: number, pageIdx: number): number | null {
  let base = 0;
  for (let s = 1; s < surah; s++) {
    const c = countsArr[s - 1];
    if (c === null || c === undefined) return null;
    base += c;
  }
  return base + pageIdx + 1;
}

/** Total pages across the whole Quran at this frame, or null until every
    surah has been measured. */
export function globalPageTotal(countsArr: (number | null)[]): number | null {
  let total = 0;
  for (let s = 1; s <= 114; s++) {
    const c = countsArr[s - 1];
    if (c === null || c === undefined) return null;
    total += c;
  }
  return total;
}
