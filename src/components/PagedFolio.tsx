import { useLayoutEffect, useRef, useState, type CSSProperties } from 'react';
import { getSurahMeta } from '@/lib/data';
import type { Bundle } from '@/lib/data';
import { pageVerses } from '@/lib/pages';
import FolioPage from '@/components/FolioPage';

/** Hard floor for the fit scale at the default Arabic size: even the densest
    mushaf page (~16 lines) fits the fixed frame well before 0.45. When the
    Arabic font slider is raised above 100%, the floor relaxes by the same
    factor — the guarantee is about the RENDERED size (23px × slider × scale),
    so a larger slider may scale further down; nothing is ever clipped. */
const MIN_SHRINK = 0.45;
/** Measure → rescale → re-measure passes before accepting the result
    (shrinking re-wraps lines, so each pass re-reads the real height). */
const MAX_PASSES = 6;

interface PagedFolioProps {
  bundle: Bundle;
  /** 1-based mushaf page (1..604) */
  page: number;
  /** dark-theme folio (`.night`) vs cream paper */
  night: boolean;
  /** verse currently being recited (whole-surah playback), highlighted inline */
  activeV?: number | null;
  /** Arabic font slider (%) — the fit re-runs from full size when it changes */
  fontScale?: number;
  /** surah whose bismillah band is reciting right now (prelude), else null */
  recitingBand?: number | null;
}

/**
 * Fixed-page mushaf (v114): one of the 604 printed-Medina pages, rendered
 * with FolioPage's exact frame/ornament/verse-marker markup inside a
 * non-scrolling box. The frame lives on the FIXED-height page box
 * (`.folio.paged`), so its border/ornament — including the bottom edge — is
 * always fully visible; only the text inside is scaled.
 *
 * Scale-to-fit (v115): after layout, the content's scrollHeight is compared
 * with the fixed inner frame's clientHeight and --paged-shrink is set
 * continuously (no steps) to make it fit: each pass multiplies the current
 * scale by √(clientHeight/scrollHeight) — page height is roughly quadratic
 * in the font scale (fewer AND shorter lines) — clamped to [MIN_SHRINK, 1],
 * re-measuring after every pass until stable (max MAX_PASSES). A surah that
 * BEGINS on this page gets its bismillah band inline at that point (never
 * for surah 9); there is no always-on band above the page.
 */
export default function PagedFolio({
  bundle,
  page,
  night,
  activeV = null,
  fontScale = 100,
  recitingBand = null,
}: PagedFolioProps) {
  const verses = pageVerses(page, bundle);
  const wrapRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);
  const [shrink, setShrink] = useState(1);
  const shrinkRef = useRef(1);
  shrinkRef.current = shrink;

  // Surahs that BEGIN on this page (their verse 1 is in it) — FolioPage
  // renders the band inline before each such group. Surah 9 (At-Tawbah) has
  // no bismillah by convention; Al-Fatiha's band is gold (1:1 IS the
  // bismillah, skipped from the text exactly like the vertical folio).
  const bands: Record<number, string> = {};
  for (const v of verses) {
    if (v.v === 1 && v.s !== 9 && bands[v.s] === undefined) {
      bands[v.s] = getSurahMeta(bundle, v.s)?.name_en ?? `Surah ${v.s}`;
    }
  }

  // Continuous scale-to-fit. Re-runs (from full size) when the page, the
  // data, the Arabic font slider, or the page box itself changes.
  useLayoutEffect(() => {
    const wrap = wrapRef.current;
    const content = contentRef.current;
    if (!wrap || !content) return;
    // See MIN_SHRINK: the 0.45 floor holds at 100% Arabic size; a raised
    // slider relaxes it by the same factor (rendered size is what matters).
    const floor = Math.min(MIN_SHRINK, (MIN_SHRINK * 100) / Math.max(100, fontScale));
    let raf = 0;
    let passes = 0;
    // Measurement history (scale, needed height) — page height is roughly
    // quadratic in the font scale (fewer AND shorter lines as it shrinks), so
    // after two measurements a h(s) = F + k·s² model solves the fitting
    // scale in one step instead of hunting.
    const hist: [number, number][] = [];
    const fit = () => {
      raf = 0;
      if (passes >= MAX_PASSES) return;
      const inner = content.querySelector<HTMLElement>('.folio-inner');
      if (!inner) return;
      const avail = inner.clientHeight; // fixed frame's inside height
      const need = inner.scrollHeight; // content height at the current scale
      if (avail <= 0) return;
      const cur = shrinkRef.current;
      if (need <= avail + 1) {
        // Fits. Grow back only when a previous pass clearly overshot
        // (>4% slack), so the text stays as large as the page allows.
        if (need >= avail * 0.96 || cur >= 1) return;
      } else if (cur <= floor + 0.001) {
        return; // floor reached — densest pages still fit (see MIN_SHRINK)
      }
      const prev = hist[hist.length - 1];
      if (!prev || prev[0] !== cur) hist.push([cur, need]);
      let next: number;
      const [a, b] = hist.length >= 2 ? [hist[hist.length - 2], hist[hist.length - 1]] : [null, null];
      if (a && b && a[0] !== b[0] && a[1] !== b[1]) {
        const k = (a[1] - b[1]) / (a[0] ** 2 - b[0] ** 2);
        const fixed = a[1] - k * a[0] ** 2;
        next = k > 0 && avail > fixed ? Math.sqrt((avail - fixed) / k) : cur * Math.sqrt(avail / need);
      } else {
        next = cur * Math.sqrt(avail / need);
      }
      next = Math.min(1, Math.max(floor, next));
      if (Math.abs(next - cur) < 0.002) return; // converged
      passes++;
      setShrink(next);
      raf = requestAnimationFrame(fit); // re-measure after the re-wrap
    };
    /** Fresh constraint set: restart the fit from full size. */
    const restart = () => {
      passes = 0;
      hist.length = 0;
      if (shrinkRef.current !== 1) setShrink(1);
      if (raf) cancelAnimationFrame(raf);
      raf = requestAnimationFrame(fit);
    };
    restart();
    // Viewport / safe-area / player-bar changes resize the fixed page box.
    const ro = new ResizeObserver(restart);
    ro.observe(wrap);
    // The IndoPak webfont swaps in after first paint with different metrics.
    void document.fonts?.ready.then(restart);
    return () => {
      ro.disconnect();
      if (raf) cancelAnimationFrame(raf);
    };
  }, [page, bundle, fontScale]);

  return (
    <div ref={wrapRef} style={{ height: '100%', minHeight: 0, overflow: 'hidden' }}>
      <div
        ref={contentRef}
        style={{ height: '100%', minHeight: 0, '--paged-shrink': shrink } as CSSProperties}
      >
        {/* Page 1 is Al-Fatiha alone, so the gold frame / Bismillah-skip rules
            of the continuous folio apply exactly as in surah view. */}
        <FolioPage
          verses={verses}
          night={night}
          activeV={activeV}
          fatiha={page === 1}
          paged
          surahBands={bands}
          recitingBand={recitingBand}
        />
      </div>
    </div>
  );
}
