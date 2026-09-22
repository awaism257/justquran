import { Fragment, useEffect, useRef, useState, type MouseEvent, type TouchEvent } from 'react';
import { wqDisplay, wqInkOffset, wqSortDisplay, loadBundle, getSurahVerses, juzStartAt } from '@/lib/data';
import type { Verse } from '@/lib/data';
import BismillahBand from '@/components/BismillahBand';
import VersePopup from '@/components/VersePopup';
import { AUTO_ADVANCE_EVENT } from '@/components/VerseCard';

interface FolioPageProps {
  verses: Verse[];
  /** dark-theme folio (`.night`) vs cream paper */
  night: boolean;
  /** verse currently being recited (whole-surah playback), highlighted inline */
  activeV?: number | null;
  /** Surah Al-Fatiha — the opening of the Qur'an gets a solid gold frame */
  fatiha?: boolean;
  /** Paged mushaf: the frame fills a fixed-height box (set by PagedFolio) */
  paged?: boolean;
  /** Paged mushaf only: surah number → display name for every surah that
      BEGINS on this page (its verse 1 is in `verses`). When set, the page's
      verses render grouped per surah and the bismillah band appears INLINE
      before a group whose first verse is s:1 (never for surah 9 — PagedFolio
      omits it). Undefined → vertical single-surah folio, unchanged. */
  surahBands?: Record<number, string>;
  /** Paged mushaf: surah whose band is currently reciting (Bismillah prelude) */
  recitingBand?: number | null;
}

/**
 * Continuous mushaf-folio: flowing Arabic with inline tappable verse
 * markers. Shown only when verse-by-verse is off and all translations off.
 */
export default function FolioPage({ verses, night, activeV = null, fatiha = false, paged = false, surahBands, recitingBand = null }: FolioPageProps) {
  const [openIdx, setOpenIdx] = useState<number | null>(null);
  // Verses the popup navigates within — normally this surah's, but
  // auto-advance across a surah boundary swaps in the next surah's verses.
  const [popupVerses, setPopupVerses] = useState<Verse[]>(verses);
  useEffect(() => setPopupVerses(verses), [verses]);
  const verse = openIdx !== null ? (popupVerses[openIdx] ?? null) : null;

  // Cross-surah auto-advance from the popup: load the next surah's verses and
  // open the popup at the requested verse (playback restarts via VersePopup's
  // verse-change effect).
  useEffect(() => {
    const onAdvance = (e: Event) => {
      const d = (e as CustomEvent<{ s: number; v: number }>).detail;
      if (!d) return;
      void loadBundle()
        .then((b) => {
          const vs = getSurahVerses(b, d.s);
          if (!vs.length) return;
          setPopupVerses(vs);
          setOpenIdx(Math.min(d.v, vs.length) - 1);
        })
        .catch(() => {});
    };
    window.addEventListener(AUTO_ADVANCE_EVENT, onAdvance);
    return () => window.removeEventListener(AUTO_ADVANCE_EVENT, onAdvance);
  }, []);

  // Long-press (or right-click / context menu) on any verse opens its popup.
  const pressTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pressPos = useRef<{ x: number; y: number } | null>(null);
  const PRESS_DELAY = 500;
  const PRESS_TOLERANCE = 10; // px of finger drift allowed before the press is cancelled
  const startPress = (i: number, e?: TouchEvent) => {
    cancelPress();
    const t = e?.touches?.[0];
    pressPos.current = t ? { x: t.clientX, y: t.clientY } : null;
    pressTimer.current = setTimeout(() => {
      pressTimer.current = null;
      setOpenIdx(i);
    }, PRESS_DELAY);
  };
  const movePress = (e: TouchEvent) => {
    // Only cancel on real movement (scrolling) — tiny finger jitter during a
    // stationary long-press must not kill the timer.
    if (!pressTimer.current || !pressPos.current) return;
    const t = e.touches[0];
    if (!t) return;
    if (
      Math.abs(t.clientX - pressPos.current.x) > PRESS_TOLERANCE ||
      Math.abs(t.clientY - pressPos.current.y) > PRESS_TOLERANCE
    ) {
      cancelPress();
    }
  };
  const cancelPress = () => {
    if (pressTimer.current) {
      clearTimeout(pressTimer.current);
      pressTimer.current = null;
    }
    pressPos.current = null;
  };

  /** One verse run (text + glued number badge + waqaf marks). `i` is the
      index into `verses` — the popup navigates by it. */
  const renderVerse = (v: Verse, i: number) => {
            // Al-Fatiha 1:1 IS the Bismillah — already shown in the gold band
            // above, so the folio skips it and opens at verse 2 (no duplication).
            if (fatiha && v.v === 1) return null;
            // Keep the verse's last word glued to its number badge: if the pair
            // doesn't fit on the current line they wrap together, so a badge never
            // orphans at the start of a line looking like it belongs to nothing.
            const ar = v.ar.trim();
            const sp = ar.lastIndexOf(' ');
            const head = sp > 0 ? ar.slice(0, sp + 1) : '';
            const tail = sp > 0 ? ar.slice(sp + 1) : ar;
            // First ayah of a juz → mushaf-style ۞ ornament before it, and a
            // soft gold wash over the whole verse so the boundary is visible.
            const jz = juzStartAt(v.s, v.v);
            const runCls =
              (activeV === v.v ? 'verse-run reciting' : 'verse-run') + (jz !== null ? ' juz-start' : '');
            const press = {
              onTouchStart: (e: TouchEvent) => startPress(i, e),
              onTouchEnd: cancelPress,
              onTouchCancel: cancelPress,
              onTouchMove: movePress,
              onContextMenu: (e: MouseEvent) => {
                e.preventDefault();
                setOpenIdx(i);
              },
            };
            return (
            <Fragment key={v.v}>
              {jz !== null && (
                <>
                  <span className="juz-orn" title={`Juz ${jz} begins here`}>
                    ۞
                  </span>{' '}
                </>
              )}
              <span id={`v${v.v}`} className={runCls} {...press}>
                {head}
              </span>
              <span className="vkeep">
                <span className={runCls} {...press}>
                  {tail}
                </span>
                <span className="wqwrap">
                  <button
                    className="vmark"
                    aria-label={`Verse ${v.v}`}
                    {...press}
                    onClick={() => setOpenIdx(i)}
                  >
                    {v.v}
                  </button>
                  {v.wq && (
                    <span className="wqmark" aria-hidden="true">
                      {wqSortDisplay(wqDisplay(v.wq))
                        .split('')
                        .map((c, ci, arr) => {
                          // 3-mark clusters: highest priority alone on top;
                          // the other two side by side below (RTL: #2 right,
                          // #3 left). 1–2 marks: plain vertical stack, 14px pitch.
                          const n = arr.length;
                          let bottom = (n - 1 - ci) * 14;
                          let dx = 0;
                          if (n === 3) {
                            if (ci === 0) bottom = 14;
                            else { bottom = 0; dx = ci === 1 ? 8 : -8; }
                          }
                          return (
                            <span
                              key={ci}
                              className="wqch"
                              style={{
                                bottom: `${bottom}px`,
                                transform: `translateX(calc(-50% + ${wqInkOffset(c) + dx}px))`,
                              }}
                            >
                              {c}
                            </span>
                          );
                        })}
                    </span>
                  )}
                </span>
              </span>{' '}
            </Fragment>
    );
  };

  // Paged mushaf only: split the page's verses into per-surah runs so a surah
  // that BEGINS on this page gets its bismillah band inline at exactly that
  // point (printed-mushaf convention). The vertical folio keeps its single
  // flowing paragraph — its band lives above the page (Reading.tsx).
  const groups: Verse[][] = [];
  if (surahBands) {
    for (const v of verses) {
      const g = groups[groups.length - 1];
      if (g && g[0].s === v.s) g.push(v);
      else groups.push([v]);
    }
  }
  let flatIdx = 0; // index into `verses`, for the popup (grouped rendering)

  return (
    <div className={['folio', night && 'night', fatiha && 'fatiha', paged && 'paged'].filter(Boolean).join(' ')}>
      <div className="folio-inner">
        {surahBands ? (
          groups.map((g) => {
            const s = g[0].s;
            const bandName = g[0].v === 1 ? surahBands[s] : undefined;
            return (
              <Fragment key={s}>
                {bandName !== undefined && (
                  <BismillahBand
                    surahName={bandName}
                    gold={s === 1}
                    reciting={recitingBand === s}
                  />
                )}
                <p className="folio-ar">{g.map((v) => renderVerse(v, flatIdx++))}</p>
              </Fragment>
            );
          })
        ) : (
          <p className="folio-ar">{verses.map((v, i) => renderVerse(v, i))}</p>
        )}
      </div>
      {verse && openIdx !== null && (
        <VersePopup
          verse={verse}
          hasPrev={openIdx > 0}
          hasNext={openIdx < popupVerses.length - 1}
          onPrev={() => setOpenIdx((i) => (i !== null && i > 0 ? i - 1 : i))}
          onNext={() => setOpenIdx((i) => (i !== null && i < popupVerses.length - 1 ? i + 1 : i))}
          onClose={() => {
            setOpenIdx(null);
            setPopupVerses(verses);
          }}
        />
      )}
    </div>
  );
}
