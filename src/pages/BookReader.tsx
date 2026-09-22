import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useParams } from 'react-router';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import BackBar from '@/components/BackBar';
import VersePopup from '@/components/VersePopup';
import { getSurahMeta, getSurahVerses, loadBundle } from '@/lib/data';
import type { Bundle, Verse } from '@/lib/data';
import { setLastRead } from '@/lib/bookmarks';
import { useSettings } from '@/lib/settings';

/** Horizontal space around each text column: column-width = page width − GAP,
    column-gap = GAP, so every page gets GAP/2 of air on each side. */
const GAP = 48;

/**
 * Book mode (v114): one surah's translation as flowing prose, paginated with
 * CSS multi-columns (one viewport-width column per page). Pages are turned by
 * translating the strip — never by scrolling (RTL scrollLeft is inconsistent
 * across browsers).
 */
export default function BookReader() {
  const params = useParams();
  const lang: 'en' | 'ur' = params.lang === 'ur' ? 'ur' : 'en';
  const n = Math.min(114, Math.max(1, parseInt(params.n ?? '1', 10) || 1));
  const { settings } = useSettings();
  const [bundle, setBundle] = useState<Bundle | null>(null);
  const [popupIdx, setPopupIdx] = useState<number | null>(null);

  useEffect(() => {
    let live = true;
    loadBundle()
      .then((b) => live && setBundle(b))
      .catch(() => {});
    return () => {
      live = false;
    };
  }, []);

  const surah = bundle ? getSurahMeta(bundle, n) : undefined;
  const verses: Verse[] = useMemo(
    () => (bundle ? getSurahVerses(bundle, n) : []),
    [bundle, n],
  );

  // Bismillah translation heads every surah except Al-Fatihah (its 1:1 IS the
  // Bismillah) and At-Tawbah. Wording taken verbatim from the bundle's own 1:1
  // translation so it stays the translator's exact text.
  const bismillah = useMemo(() => {
    if (!bundle || n === 1 || n === 9) return null;
    const first = getSurahVerses(bundle, 1)[0];
    return (lang === 'en' ? first?.en : first?.ur) || null;
  }, [bundle, n, lang]);

  // ---- pagination (CSS multi-columns + translateX) ----
  const vpRef = useRef<HTMLDivElement>(null); // the visible page (clips the strip)
  const stripRef = useRef<HTMLDivElement>(null); // the columnised prose strip
  const markRefs = useRef<(HTMLButtonElement | null)[]>([]); // verse markers, in order
  const [vw, setVw] = useState(0); // page width = viewport clientWidth
  const [pageCount, setPageCount] = useState(1);
  const [pageIdx, setPageIdx] = useState(0); // 0-based

  const fontScale = lang === 'en' ? settings.fonts.en : settings.fonts.ur;

  useLayoutEffect(() => {
    const measure = () => {
      const vp = vpRef.current;
      if (!vp) return;
      const w = vp.clientWidth;
      setVw(w);
      const strip = stripRef.current;
      if (strip && w > 0) {
        // strip content = N columns of (w − GAP) + (N−1) gaps → N·w − GAP
        setPageCount(Math.max(1, Math.round((strip.scrollWidth + GAP) / w)));
      }
    };
    measure();
    window.addEventListener('resize', measure);
    // Webfonts (Nastaliq especially) change the text metrics once loaded.
    void document.fonts?.ready.then(measure);
    return () => window.removeEventListener('resize', measure);
  }, [lang, n, verses, fontScale]);

  // Clamp when the page count shrinks (font size down, wider window…).
  useEffect(() => {
    setPageIdx((i) => Math.min(i, pageCount - 1));
  }, [pageCount]);

  // New surah / language → back to the first page, close any popup.
  useEffect(() => {
    setPageIdx(0);
    setPopupIdx(null);
  }, [n, lang]);

  const nextPage = useCallback(
    () => setPageIdx((i) => Math.min(pageCount - 1, i + 1)),
    [pageCount],
  );
  const prevPage = useCallback(() => setPageIdx((i) => Math.max(0, i - 1)), []);

  // ---- turning input: swipe (same thresholds as the Reading page) ----
  const touchStart = useRef<{ x: number; y: number; t: number } | null>(null);
  const onTouchStartSwipe = useCallback((e: React.TouchEvent) => {
    const t = e.touches[0];
    touchStart.current = { x: t.clientX, y: t.clientY, t: Date.now() };
  }, []);
  const onTouchEndSwipe = useCallback(
    (e: React.TouchEvent) => {
      const s = touchStart.current;
      touchStart.current = null;
      if (!s) return;
      const t = e.changedTouches[0];
      const dx = t.clientX - s.x;
      const dy = t.clientY - s.y;
      const dt = Date.now() - s.t;
      if (dt > 900 || Math.abs(dx) < 72 || Math.abs(dx) < Math.abs(dy) * 1.6) return;
      // English (ltr): drag left = next. Urdu (rtl, mushaf convention): drag right = next.
      const forward = lang === 'ur' ? dx > 0 : dx < 0;
      if (forward) nextPage();
      else prevPage();
    },
    [lang, nextPage, prevPage],
  );

  // ---- continue-reading: first verse on the current page ----
  useEffect(() => {
    if (!verses.length) return;
    const t = setTimeout(() => {
      const vp = vpRef.current;
      if (!vp) return;
      const r = vp.getBoundingClientRect();
      for (let i = 0; i < markRefs.current.length; i++) {
        const el = markRefs.current[i];
        if (!el) continue;
        const b = el.getBoundingClientRect();
        if (b.left >= r.left - 1 && b.right <= r.right + 1) {
          setLastRead(n, verses[i].v);
          return;
        }
      }
      setLastRead(n, verses[0].v);
    }, 350); // after the 0.25s page-turn transition settles
    return () => clearTimeout(t);
  }, [pageIdx, pageCount, verses, n]);

  const popupVerse = popupIdx !== null ? (verses[popupIdx] ?? null) : null;
  // ltr: strip slides left (negative). rtl: columns flow right→left, so the
  // strip slides right (positive) to reach later pages.
  const offset = pageIdx * vw * (lang === 'ur' ? 1 : -1);

  return (
    <div className="tiles" style={{ height: '100dvh', display: 'flex', flexDirection: 'column' }}>
      <BackBar
        title={surah ? `${n}. ${surah.name_en}` : `Surah ${n}`}
        meta={`Page ${pageIdx + 1} of ${pageCount} · ${lang === 'en' ? 'English translation' : 'Urdu translation'}`}
      />
      <div
        className="px-4 pt-2"
        style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
      >
        <div
          ref={vpRef}
          className="book-page"
          style={{ flex: 1, minHeight: 0 }}
          onTouchStart={onTouchStartSwipe}
          onTouchEnd={onTouchEndSwipe}
        >
          <div
            ref={stripRef}
            dir={lang === 'ur' ? 'rtl' : 'ltr'}
            style={{
              height: '100%',
              boxSizing: 'border-box',
              margin: '0 24px',
              padding: '26px 0',
              columnWidth: vw > 0 ? vw - GAP : undefined,
              columnGap: GAP,
              columnFill: 'auto',
              transform: vw > 0 ? `translateX(${offset}px)` : undefined,
              transition: 'transform 0.25s ease',
              visibility: vw > 0 ? 'visible' : 'hidden',
            }}
          >
            {!bundle && (
              <p className="text-center" style={{ color: 'var(--muted)', fontSize: 13 }}>
                Loading…
              </p>
            )}
            {bismillah && (
              <p className={`book-bism ${lang === 'en' ? 'book-en' : 'book-ur'}`}>{bismillah}</p>
            )}
            <p className={lang === 'en' ? 'book-en' : 'book-ur'} style={{ margin: 0 }}>
              {verses.map((v, i) => (
                <span key={v.v}>
                  {lang === 'en' ? v.en : (v.ur ?? '')}{' '}
                  <button
                    type="button"
                    className="bkmark"
                    ref={(el) => {
                      markRefs.current[i] = el;
                    }}
                    aria-label={`Verse ${v.v}`}
                    onClick={() => setPopupIdx(i)}
                  >
                    {v.v}
                  </button>{' '}
                </span>
              ))}
            </p>
          </div>

          {/* edge-tap page-turn zones (faint chevron, no press state) */}
          <button
            type="button"
            className="book-edge"
            style={{ left: 0 }}
            aria-label={lang === 'ur' ? 'Next page' : 'Previous page'}
            onClick={lang === 'ur' ? nextPage : prevPage}
          >
            <ChevronLeft size={14} />
          </button>
          <button
            type="button"
            className="book-edge"
            style={{ right: 0 }}
            aria-label={lang === 'ur' ? 'Previous page' : 'Next page'}
            onClick={lang === 'ur' ? prevPage : nextPage}
          >
            <ChevronRight size={14} />
          </button>
        </div>

        {/* Footer pager: English › = next; Urdu ‹ = next (mushaf convention). */}
        <nav className="pager" style={{ padding: '10px 0 16px' }}>
          {(lang === 'ur' ? pageIdx < pageCount - 1 : pageIdx > 0) ? (
            <button
              type="button"
              className="pager-link"
              onClick={lang === 'ur' ? nextPage : prevPage}
            >
              {lang === 'ur' ? '‹ Next' : '‹ Prev'}
            </button>
          ) : (
            <span className="pager-link disabled">{lang === 'ur' ? '‹ Next' : '‹ Prev'}</span>
          )}
          <span className="count">
            Page {pageIdx + 1} of {pageCount}
          </span>
          {(lang === 'ur' ? pageIdx > 0 : pageIdx < pageCount - 1) ? (
            <button
              type="button"
              className="pager-link"
              onClick={lang === 'ur' ? prevPage : nextPage}
            >
              {lang === 'ur' ? 'Prev ›' : 'Next ›'}
            </button>
          ) : (
            <span className="pager-link disabled">{lang === 'ur' ? 'Prev ›' : 'Next ›'}</span>
          )}
        </nav>
      </div>

      {popupVerse && popupIdx !== null && (
        <VersePopup
          verse={popupVerse}
          hasPrev={popupIdx > 0}
          hasNext={popupIdx < verses.length - 1}
          onPrev={() => setPopupIdx((i) => (i !== null && i > 0 ? i - 1 : i))}
          onNext={() => setPopupIdx((i) => (i !== null && i < verses.length - 1 ? i + 1 : i))}
          onClose={() => setPopupIdx(null)}
        />
      )}
    </div>
  );
}
