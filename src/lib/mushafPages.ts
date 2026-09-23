// Dynamic mushaf pagination: page boundaries are COMPUTED at the reader's
// chosen Arabic font scale, not taken from a fixed printed-page map. A page
// is a whole number of VERSES, sliced out of the surah's flow at line-break
// character offsets (word boundaries, so Arabic shaping is preserved), so
// nothing can ever clip and a page turn never lands mid-verse — except a
// verse taller than a page, which is line-sliced alone (only then).
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
  /** width of this line's owning verse that sat on EARLIER (shared) lines —
      nonzero only on a verse's first owned line when the verse's head began
      mid-line behind the previous verse's badge. A page opening here renders
      the verse from char 0, so those words re-flow into it. */
  lead: number;
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
  let pendingLead = 0; // verse-head width already spent on shared earlier lines
  let ornPending = 0; // juz ornament width to charge if the head shares its line
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
        pendingLead = 0;
        // A juz boundary ornament ("۞ ") precedes the head and moves with it.
        ornPending = 0;
        const prev = el.previousElementSibling;
        if (prev && prev.classList.contains('juz-orn')) {
          ornPending = prev.getBoundingClientRect().width + 5; // + the glue space
        }
      }
      const nodeVerse = verse;
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
          if (nodeVerse !== last.verse) {
            // a later verse's head sharing this line: its width re-flows onto
            // the next page if a page break lands at this verse boundary
            pendingLead += r.width + ornPending;
            ornPending = 0;
          }
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
          lines.push({ top, bottom, verse: nodeVerse, char, lead: pendingLead });
          pendingLead = 0;
          ornPending = 0;
        }
      }
    }
  });
  range.detach();
  return lines;
}

/** Greedy VERSE-boundary pagination: a page holds as many whole verses as
    fit and may only END where a verse completes — its last rendered element
    is a verse's number badge, so a page turn never lands mid-verse (user
    decision: "the last verse must be a complete verse"). A verse's last line
    is detected by OWNERSHIP: the next measured line belongs to a higher
    verse number (the badge is glued to the verse's last word by .vkeep, so
    the last line always carries verse text — a mid-line verse start does NOT
    make the next line char === 0). Page 0's capacity is measured from the
    flow root top (surah band included); later pages from their first line's
    top. When the opening verse's head began mid-line behind the previous
    verse's badge, the rendered page starts it from char 0 and those lead
    words re-flow into the page: justified greedy re-breaking can add up to
    one line regardless of trailing slack, so a full line is charged whenever
    the opening verse has a lead (lead > 0). Sole exception: a verse taller
    than one page even with the charge is line-sliced alone (e.g. 2:282 at a
    huge scale). */
export function paginate(
  surah: number,
  lines: MeasuredLine[],
  availH: number,
  firstVerse: number,
  lastVerse: number,
): Pagination {
  const starts: Slice[] = [{ verse: firstVerse, char: 0 }];
  const anchors: number[] = [firstVerse];
  const cap = availH - 6; // safety: badge protrusion + sub-pixel rounding
  if (lines.length > 0 && cap > 0) {
    anchors[0] = Math.max(firstVerse, lines[0].verse);
    const lineH =
      lines.length > 1 ? lines[1].top - lines[0].top : lines[0].bottom - lines[0].top;
    // Page-end candidates: the flow's last line, or a line after which the
    // next line is owned by a NEW verse — i.e. this line ends with a badge.
    const ends: number[] = [];
    for (let k = 0; k < lines.length; k++) {
      if (k === lines.length - 1 || lines[k + 1].verse > lines[k].verse) ends.push(k);
    }
    let a = 0; // first line of the current page
    let ei = 0;
    while (a < lines.length) {
      while (ends[ei] < a) ei++;
      const firstEnd = ends[ei]; // line where the page's opening verse completes
      const top = a === 0 ? 0 : lines[a].top; // page 0 carries the band
      // Re-flow charge: when the page opens with a verse whose head began
      // mid-line behind the previous badge (lead > 0), the rendered page
      // starts it from char 0; justified greedy re-breaking can add up to one
      // line regardless of the trailing slack, so charge a full line.
      const extraH = lines[a].lead > 0 ? lineH : 0;
      let next: number;
      if (lines[firstEnd].bottom + extraH - top > cap + 0.5) {
        // Oversize verse (taller than a page even alone): line-slice it
        // across sub-pages, then resume verse-boundary paging after it.
        // Sub-pages open at measured line breaks, so they carry no lead —
        // only the first sub-page inherits the opening verse's re-flow.
        let line = a;
        while (line <= firstEnd) {
          const ltop = lines[line].top - (line === a ? extraH : 0);
          let last = line;
          while (last + 1 <= firstEnd && lines[last + 1].bottom - ltop <= cap + 0.5) last++;
          line = last + 1;
          if (line <= firstEnd) {
            starts.push({ verse: lines[line].verse, char: lines[line].char });
            anchors.push(lines[line].verse);
          }
        }
        next = firstEnd + 1;
      } else {
        // Greedy: the largest run of whole verses that fits.
        let k = firstEnd;
        while (
          ei + 1 < ends.length &&
          lines[ends[ei + 1]].bottom + extraH - top <= cap + 0.5
        ) {
          ei++;
          k = ends[ei];
        }
        next = k + 1;
      }
      a = next;
      if (a < lines.length) {
        // Verse-boundary opening: the page renders the verse FROM ITS START
        // (char 0), so sliceVerses never splits a verse across a page turn.
        starts.push({ verse: lines[a].verse, char: 0 });
        anchors.push(Math.max(firstVerse, lines[a].verse));
      }
    }
  }
  // verse → page (page containing the verse's FIRST rendered char): walk the
  // page slices; first assignment wins so a verse split by the oversize
  // fallback still maps to the page where it STARTS, and a tiny verse that
  // never owns a measured line maps to the page whose slice contains it.
  const count = starts.length;
  const pageOfVerse: number[] = new Array(lastVerse + 1).fill(-1);
  for (let i = 0; i < count; i++) {
    const s1 = i + 1 < count ? starts[i + 1] : null;
    const endV = s1 ? (s1.char === 0 ? s1.verse - 1 : s1.verse) : lastVerse;
    for (let v = starts[i].verse; v <= endV && v <= lastVerse; v++) {
      if (pageOfVerse[v] < 0) pageOfVerse[v] = i;
    }
  }
  for (let v = 1; v <= lastVerse; v++) if (pageOfVerse[v] < 0) pageOfVerse[v] = count - 1;
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
