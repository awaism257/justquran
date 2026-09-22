// Mushaf page map: loads /data/pages.json once, caches, typed accessors.
// 604 fixed pages, printed Medina mushaf pagination (KFGQPC). Page N contains
// the verses from starts[N-1] up to (excluding) starts[N]; page 604 runs to
// 114:6. Boundary metadata only (zonetecde/mushaf-layout, ISC).

import { getSurahVerses } from '@/lib/data';
import type { Bundle, Verse } from '@/lib/data';

export interface PageMap {
  version: number;
  total: number; // 604
  /** starts[i] = [surah, verse] where page i+1 (1-based) begins. */
  starts: [number, number][];
}

let cached: PageMap | null = null;
let mapPromise: Promise<PageMap> | null = null;

export function loadPageMap(): Promise<PageMap> {
  if (cached) return Promise.resolve(cached);
  if (!mapPromise) {
    mapPromise = fetch(`${import.meta.env.BASE_URL}data/pages.json`)
      .then((r) => {
        if (!r.ok) throw new Error(`Failed to load mushaf page map: ${r.status}`);
        return r.json() as Promise<PageMap>;
      })
      .then((m) => {
        cached = m;
        return m;
      })
      .catch((e) => {
        mapPromise = null; // allow retry
        throw e;
      });
  }
  return mapPromise;
}

function map(): PageMap {
  if (!cached) throw new Error('Page map not loaded — await loadPageMap() first');
  return cached;
}

export function pageCount(): number {
  return map().total;
}

/** [surah, verse] at which `page` (1-based) begins. */
export function pageStart(page: number): [number, number] {
  const m = map();
  const p = Math.min(m.total, Math.max(1, page));
  return m.starts[p - 1];
}

/** 1-based page containing the verse (s, v). */
export function pageOfVerse(s: number, v: number): number {
  const starts = map().starts;
  let lo = 0;
  let hi = starts.length - 1;
  let ans = 0;
  while (lo <= hi) {
    const mid = (lo + hi) >> 1;
    const [ms, mv] = starts[mid];
    if (ms < s || (ms === s && mv <= v)) {
      ans = mid;
      lo = mid + 1;
    } else {
      hi = mid - 1;
    }
  }
  return ans + 1;
}

/** Verses of `page` (1-based) in reading order, crossing surah boundaries. */
export function pageVerses(page: number, b: Bundle): Verse[] {
  const [s0, v0] = pageStart(page);
  const [s1, v1] = page < pageCount() ? pageStart(page + 1) : [115, 1];
  const out: Verse[] = [];
  for (let s = s0; s <= Math.min(s1, 114); s++) {
    for (const v of getSurahVerses(b, s)) {
      if (s === s0 && v.v < v0) continue;
      if (s === s1 && v.v >= v1) continue;
      out.push(v);
    }
  }
  return out;
}
