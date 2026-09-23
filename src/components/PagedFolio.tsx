import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { getSurahMeta, getSurahVerses } from '@/lib/data';
import type { Bundle } from '@/lib/data';
import {
  beginCountsSession,
  cachePagination,
  getCachedPagination,
  getCounts,
  measureFlow,
  paginate,
  pagKey,
  recordCount,
  sliceVerses,
} from '@/lib/mushafPages';
import type { Pagination } from '@/lib/mushafPages';
import FolioPage from '@/components/FolioPage';

interface PagedFolioProps {
  bundle: Bundle;
  /** current surah (1..114) — every surah starts on a new page */
  surah: number;
  /** 0-based page within this surah */
  page: number;
  /** dark-theme folio (`.night`) vs cream paper */
  night: boolean;
  /** verse currently being recited (whole-surah playback), highlighted inline */
  activeV?: number | null;
  /** Arabic font slider (%) — pages are laid out at exactly this scale */
  fontScale?: number;
  /** surah whose bismillah band is reciting right now (prelude), else null */
  recitingBand?: number | null;
  /** pagination (re)computed — the parent re-anchors its page index by verse */
  onPaginate?: (pg: Pagination) => void;
}

interface FrameSize {
  /** content-box width/height of the ornate frame's inner box (px, rounded) */
  w: number;
  h: number;
  /** outer width of the .folio box — probes copy it so line breaks match */
  folioW: number;
}

/**
 * Dynamic paged mushaf. The whole surah is typeset ONCE, at the reader's
 * chosen Arabic font scale, in a hidden measurer at the frame's exact width;
 * the rendered line boxes are walked to find page boundaries at LINE breaks
 * (each page = as many whole lines as fit the frame's content height). The
 * visible page is then rendered as a SLICE of the surah (char offsets at
 * word boundaries, so Arabic shaping is untouched) containing exactly its
 * own lines — nothing can ever clip, by construction. No fit solver, no
 * font scaling, no line-height stretch.
 *
 * The ornate frame, themes, bismillah band (top of page 1; never surah 9;
 * surah 1 keeps its gold band and in-text 1:1) and waqf marks render exactly
 * as the vertical folio does. Re-measured on surah / font-slider / frame
 * size change and once webfonts settle; results are cached per
 * (surah, width, height, scale). Measured page counts feed the global
 * "Page X of Y" label: unvisited surahs are measured lazily, one per idle
 * slice, in the same hidden probe at the same frame width.
 */
export default function PagedFolio({
  bundle,
  surah,
  page,
  night,
  activeV = null,
  fontScale = 100,
  recitingBand = null,
  onPaginate,
}: PagedFolioProps) {
  const wrapRef = useRef<HTMLDivElement>(null);
  const measureRef = useRef<HTMLDivElement>(null);
  const probeRef = useRef<HTMLDivElement>(null);
  const [size, setSize] = useState<FrameSize | null>(null);
  const [pg, setPg] = useState<Pagination | null>(null);
  const [fontBump, setFontBump] = useState(0);
  /** when true, the hidden full-surah measurer is mounted for (re)pagination */
  const [measuring, setMeasuring] = useState(true);
  /** background page-count probing: next surah to measure offscreen */
  const [probe, setProbe] = useState<number | null>(null);
  const probeFailed = useRef<Set<number>>(new Set());

  const verses = getSurahVerses(bundle, surah);
  const bandName = getSurahMeta(bundle, surah)?.name_en ?? `Surah ${surah}`;

  // Track the frame's content box: viewport / safe-area / player-bar changes
  // resize the fixed page box, and a new size means a new pagination.
  useLayoutEffect(() => {
    const wrap = wrapRef.current;
    if (!wrap) return;
    const read = () => {
      const folio = wrap.querySelector<HTMLElement>(':scope > .folio');
      const inner = wrap.querySelector<HTMLElement>(':scope > .folio .folio-inner');
      if (!folio || !inner) return;
      const cs = getComputedStyle(inner);
      const w = Math.round(inner.clientWidth - parseFloat(cs.paddingLeft) - parseFloat(cs.paddingRight));
      const h = Math.round(inner.clientHeight - parseFloat(cs.paddingTop) - parseFloat(cs.paddingBottom));
      const folioW = Math.round(folio.offsetWidth);
      if (w <= 0 || h <= 0) return;
      setSize((cur) =>
        cur && Math.abs(cur.w - w) < 2 && Math.abs(cur.h - h) < 2 && cur.folioW === folioW
          ? cur
          : { w, h, folioW },
      );
    };
    read();
    const ro = new ResizeObserver(read);
    ro.observe(wrap);
    return () => ro.disconnect();
  }, [bundle, surah]);

  // The IndoPak webfont swaps in after first paint with different metrics —
  // re-measure once it settles (line breaks shift).
  useEffect(() => {
    let live = true;
    void document.fonts?.ready.then(() => {
      if (live) setFontBump((b) => b + 1);
    });
    return () => {
      live = false;
    };
  }, []);

  // (Re)measure trigger: new surah / frame / scale / fonts → serve from the
  // cache if we have it, else mount the hidden full-surah measurer.
  useLayoutEffect(() => {
    if (!size) return;
    beginCountsSession(`${size.w}x${size.h}#${fontScale}`);
    // While the IndoPak webfont is still loading, text rects are unusable
    // (font-block period: zero-size boxes) and would be stale once the font
    // swaps in — wait for the fonts-ready bump.
    if (document.fonts?.status === 'loading') return;
    const key = pagKey(surah, size.w, size.h, fontScale);
    const cached = getCachedPagination(key);
    if (cached) {
      recordCount(surah, cached.count);
      setPg(cached);
      onPaginate?.(cached);
      setMeasuring(false);
      return;
    }
    setMeasuring(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [size, surah, fontScale, bundle, fontBump]);

  // The hidden measurer is mounted: walk its line boxes and paginate. Waits
  // for webfonts (font-block period yields zero-size text rects — fontBump
  // in the deps re-fires this effect once they settle).
  useLayoutEffect(() => {
    if (!measuring || !size) return;
    if (document.fonts?.status === 'loading') return;
    const inner = measureRef.current?.querySelector<HTMLElement>('.folio-inner');
    if (!inner) return;
    const lines = measureFlow(inner, true);
    if (!lines.length && verses.length) return; // layout not ready yet
    const next = paginate(surah, lines, size.h, 1, verses.length);
    (window as unknown as { __pg?: unknown }).__pg = { surah, h: size.h, lines, next };
    cachePagination(pagKey(surah, size.w, size.h, fontScale), next);
    recordCount(surah, next.count);
    setPg(next);
    onPaginate?.(next);
    setMeasuring(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [measuring, size, surah, bundle, fontBump]);

  // Background probing: measure the page counts of surahs not yet seen, one
  // per idle slice, so the global "Page X of Y" label fills in without jank.
  useEffect(() => {
    if (!size || measuring || probe !== null) return;
    const counts = getCounts();
    let next = -1;
    for (let s = 1; s <= 114; s++) {
      if ((counts[s - 1] === null || counts[s - 1] === undefined) && !probeFailed.current.has(s)) {
        next = s;
        break;
      }
    }
    if (next === -1) return;
    const ric: (cb: () => void) => number =
      typeof window.requestIdleCallback === 'function'
        ? (cb) => window.requestIdleCallback(() => cb(), { timeout: 400 })
        : (cb) => window.setTimeout(cb, 32);
    const cancelRic: (id: number) => void =
      typeof window.cancelIdleCallback === 'function'
        ? (id) => window.cancelIdleCallback(id)
        : (id) => window.clearTimeout(id);
    const id = ric(() => setProbe(next));
    return () => cancelRic(id);
  }, [size, measuring, probe, pg, fontScale]);

  // Measure the mounted probe surah (line count only — no char slicing
  // needed), record its page count, unmount it.
  useLayoutEffect(() => {
    if (probe === null || !size) return;
    const inner = probeRef.current?.querySelector<HTMLElement>('.folio-inner');
    const pv = getSurahVerses(bundle, probe);
    const lines = inner ? measureFlow(inner, false) : [];
    if (!lines.length) {
      probeFailed.current.add(probe);
      setProbe(null);
      return;
    }
    recordCount(probe, paginate(probe, lines, size.h, 1, pv.length).count);
    setProbe(null);
  }, [probe, size, bundle]);

  const surahPg = pg && pg.surah === surah ? pg : null;
  const cur = Math.max(0, Math.min(page, (surahPg?.count ?? 1) - 1));
  // While the first measurement runs, show the surah's top (page 0 shape:
  // band + first verses) rather than a blank frame.
  const pageVerses = surahPg ? sliceVerses(verses, surahPg, cur) : verses.slice(0, 12);
  const onFirstPage = surahPg ? cur === 0 : true;

  return (
    <div
      ref={wrapRef}
      className={measuring && !surahPg ? 'paged-measuring' : undefined}
      style={{ height: '100%', minHeight: 0, overflow: 'hidden' }}
    >
      <FolioPage
        key={`${surah}:${cur}`}
        verses={pageVerses}
        night={night}
        activeV={activeV}
        fatiha={surah === 1 && onFirstPage}
        paged
        surahBands={onFirstPage && surah !== 9 ? { [surah]: bandName } : {}}
        recitingBand={recitingBand}
      />
      {/* Hidden full-surah measurer: identical markup and frame width to the
          visible slice, so measured line breaks match the rendered pages. */}
      {measuring && size !== null && (
        <div ref={measureRef} className="paged-probe" style={{ width: size.folioW }} aria-hidden="true">
          <FolioPage
            verses={verses}
            night={night}
            paged
            fatiha={surah === 1}
            surahBands={surah === 9 ? {} : { [surah]: bandName }}
          />
        </div>
      )}
      {/* Offscreen page-count probe for the global "Page X of Y" label. */}
      {probe !== null && size !== null && !measuring && (
        <div ref={probeRef} className="paged-probe" style={{ width: size.folioW }} aria-hidden="true">
          <FolioPage
            verses={getSurahVerses(bundle, probe)}
            night={night}
            paged
            fatiha={probe === 1}
            surahBands={
              probe === 9
                ? {}
                : { [probe]: getSurahMeta(bundle, probe)?.name_en ?? `Surah ${probe}` }
            }
          />
        </div>
      )}
    </div>
  );
}
