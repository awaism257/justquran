import { useLayoutEffect, useRef, useState, type CSSProperties } from 'react';
import { getSurahMeta } from '@/lib/data';
import type { Bundle } from '@/lib/data';
import { pageVerses } from '@/lib/pages';
import FolioPage from '@/components/FolioPage';

/** Absolute bottom of the fit scale (~0.10× the 100% base size; when the
    Arabic slider is raised above 100% the bottom relaxes by the same factor —
    the guarantee is about the RENDERED size, 23px × slider × scale).
    0.45×base is the preferred READABILITY floor (the densest mushaf page,
    ~16 lines, fits a normal phone frame well before it), but "never clip" is
    a HARD guarantee: a page that genuinely cannot fit at the readability
    floor (tiny landscape / SE-height frames) keeps shrinking past it until
    it fits, bottoming out here — 0.10 is reached only on ≤568px-tall frames
    showing a 3-band page (page 604 on a 320×568 portrait screen lands near
    ~0.12): tiny, but no verse is ever hidden. */
const HARD_SHRINK = 0.10;
/** Sparse pages GROW to fill the frame, up to 1.45× the 100% base size. The
    cap is on the ABSOLUTE rendered size, so a raised Arabic slider lowers it
    by the same factor (cap = 1.45·100/fontScale) — beyond 100% the slider
    shifts the whole [0.45, 1.45] rendered-size window down, never past it. */
const MAX_GROW = 1.45;
/** Stage 2 fill: once the font scale settles, leftover vertical slack becomes
    extra leading (up to this factor) instead of dead space. Line-height does
    not change line breaks (width is unchanged), so one extra measure suffices. */
const MAX_LH = 1.45;
/** Measure → rescale → re-measure passes before accepting the result
    (shrinking re-wraps lines, so each pass re-reads the real height). The
    quadratic model usually lands in 2–3; the extra headroom is for bracket
    bisection when a rewrap step-wall forces it. */
const MAX_PASSES = 8;

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
 * Scale-to-fit (v115) + mushaf-style FILL (v116): after layout, the content's
 * real stack height (last in-flow child's rect bottom — scrollHeight clamps
 * to the frame height and cannot see slack) is compared with the fixed inner
 * frame's clientHeight and --paged-shrink is set continuously (no steps): each pass solves the
 * h(s) = F + k·s² model (page height is roughly quadratic in the font scale —
 * fewer AND shorter lines), clamped to [hardFloor, cap] where
 * hardFloor = 0.10·100/fontScale ("never clip" is a hard guarantee; 0.45×base
 * is only a readability preference that tiny frames may go below) and
 * cap = 1.45·100/fontScale, re-measuring after
 * every pass until stable (max MAX_PASSES, with a measured fit/overflow
 * bracket as convergence backstop against line-rewrap step-walls). SPARSE
 * pages grow above 1 so every
 * page fills its frame; once the scale settles, any slack left (content still
 * shorter than the frame at the cap) is filled by stretching leading via
 * --paged-lh = clamp(avail/need, 1, 1.45) — even vertical rhythm like a
 * printed mushaf, and nothing ever clips. A surah that BEGINS on this page
 * gets its bismillah band inline at that point (never for surah 9); there is
 * no always-on band above the page.
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
  // Stage-2 fill: extra leading factor (CSS var --paged-lh), 1 = natural.
  const [lh, setLh] = useState(1);
  const lhRef = useRef(1);
  lhRef.current = lh;

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
    // See HARD_SHRINK: the fit may shrink to 0.10×base (slider-adjusted) if a
    // frame genuinely cannot hold the page at the 0.45 readability floor.
    const hardFloor = Math.min(HARD_SHRINK, (HARD_SHRINK * 100) / Math.max(100, fontScale));
    // Growth cap (MAX_GROW): sparse pages fill the frame up to 1.45× the 100%
    // base — again an ABSOLUTE rendered-size bound, so a raised slider lowers
    // it (at slider 160 the cap is ~0.91, i.e. the slider did its growing).
    const cap = (MAX_GROW * 100) / Math.max(100, fontScale);
    let raf = 0;
    let passes = 0;
    // Measurement history (scale, needed height) — page height is roughly
    // quadratic in the font scale (fewer AND shorter lines as it shrinks), so
    // after two measurements a h(s) = F + k·s² model solves the fitting
    // scale in one step instead of hunting.
    const hist: [number, number][] = [];
    // Bracket of KNOWN outcomes: lo = largest measured scale that fits (with
    // its measured height), hi = smallest measured scale that overflows.
    // h(s) is really a STEP function near the crossing — one rewrapped line
    // jumps the height by a whole line — so the smooth model can ping-pong
    // across the boundary; the bracket guarantees the search still lands on
    // the largest fitting scale and never rests on an overflowing one.
    let lo = 0;
    let loNeed = 0;
    let hi = Infinity;
    /** Stage 2 — line-height stretch: the font scale has settled; fill any
        remaining slack with leading (only ever ≥1, so it can never clip).
        Widths and line breaks are unaffected, so no further fit pass runs. */
    const finish = (availH: number, needH: number) => {
      const target = needH > 0 ? Math.min(MAX_LH, Math.max(1, availH / needH)) : 1;
      if (Math.abs(target - lhRef.current) > 0.004) setLh(target);
    };
    /** Fall back to the largest measured fit instead of resting on an
        overflowing scale (its height is known, so lh is computed directly). */
    const settle = (availH: number): boolean => {
      if (lo > 0 && shrinkRef.current > lo) {
        setShrink(lo);
        finish(availH, loNeed);
        return true;
      }
      return false;
    };
    const fit = () => {
      raf = 0;
      const inner = content.querySelector<HTMLElement>('.folio-inner');
      if (!inner) return;
      const avail = inner.clientHeight; // fixed frame's inside height
      // Real content stack height. scrollHeight CANNOT be used: it clamps to
      // clientHeight whenever content is shorter than the frame, hiding the
      // slack the fill stages need to see (and polluting the h(s) model with
      // bogus points). The last in-flow child's rect bottom, measured from
      // the padding-box top, matches scrollHeight when content overflows.
      const last = inner.lastElementChild;
      if (!last) return;
      const need =
        last.getBoundingClientRect().bottom -
        (inner.getBoundingClientRect().top +
          (parseFloat(getComputedStyle(inner).borderTopWidth) || 0));
      if (avail <= 0) return;
      const cur = shrinkRef.current;
      const fits = need <= avail + 1;
      if (fits) {
        if (cur >= lo) {
          lo = cur;
          loNeed = need;
        }
      } else {
        hi = Math.min(hi, cur);
      }
      if (passes >= MAX_PASSES) {
        if (!settle(avail)) finish(avail, need);
        return;
      }
      if (fits && cur >= cap - 0.001) {
        finish(avail, need); // at the growth cap — as large as allowed
        return;
      }
      if (!fits && cur <= hardFloor + 0.001) {
        finish(avail, need); // absolute bottom — even this frame cannot fit more
        return;
      }
      if (hi - lo <= 0.004) {
        // Bracket closed: sit on the largest fitting scale.
        if (!fits && settle(avail)) return;
        finish(avail, fits ? need : loNeed);
        return;
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
      next = Math.min(cap, Math.max(hardFloor, next));
      // A model step outside the measured bracket (rewrap steps confuse it)
      // is replaced by bisection, which always converges; with no upper
      // bound yet, the plain √(avail/need) step keeps its direction.
      if (next <= lo || next >= hi) {
        next = hi < Infinity ? (lo + hi) / 2 : cur * Math.sqrt(avail / need);
      }
      if (fits) {
        if (next <= cur + 0.002) {
          finish(avail, need); // converged — no useful growth left
          return;
        }
      } else if (next >= cur - 0.002) {
        if (lo > 0) {
          // Stuck against a rewrap step-wall with a known fit below: settle
          // on the largest measured fit rather than clipping.
          settle(avail);
          return;
        }
        // Nothing has fit yet — keep shrinking with real (monotone) steps;
        // hi decreases every pass, so this must reach a fitting scale.
        next = Math.max(hardFloor, Math.min(cur * Math.sqrt(avail / need), cur * 0.985));
      }
      passes++;
      setShrink(next);
      raf = requestAnimationFrame(fit); // re-measure after the re-wrap
    };
    /** Fresh constraint set: restart the fit from full size (and natural
        leading — the fit always measures with --paged-lh at 1). */
    const restart = () => {
      passes = 0;
      hist.length = 0;
      lo = 0;
      loNeed = 0;
      hi = Infinity;
      const start = Math.min(1, cap);
      if (shrinkRef.current !== start) setShrink(start);
      if (lhRef.current !== 1) setLh(1);
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
        style={
          { height: '100%', minHeight: 0, '--paged-shrink': shrink, '--paged-lh': lh } as CSSProperties
        }
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
