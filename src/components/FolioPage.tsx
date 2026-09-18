import { Fragment, useEffect, useRef, useState, type MouseEvent, type TouchEvent } from 'react';
import { wqDisplay, wqInkOffset, loadBundle, getSurahVerses, juzStartAt } from '@/lib/data';
import type { Verse } from '@/lib/data';
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
}

/**
 * Continuous mushaf-folio: flowing Arabic with inline tappable verse
 * markers. Shown only when verse-by-verse is off and all translations off.
 */
export default function FolioPage({ verses, night, activeV = null, fatiha = false }: FolioPageProps) {
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

  return (
    <div className={['folio', night && 'night', fatiha && 'fatiha'].filter(Boolean).join(' ')}>
      <div className="folio-inner">
        <p className="folio-ar">
          {verses.map((v, i) => {
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
                    <span
                      className="wqmark"
                      aria-hidden="true"
                      style={{ transform: `translateX(calc(-50% + ${wqInkOffset(wqDisplay(v.wq))}px))` }}
                    >
                      {wqDisplay(v.wq)
                        .split('')
                        .map((c, ci) => (
                          <span key={ci} className="wqch">
                            {c}
                          </span>
                        ))}
                    </span>
                  )}
                </span>
              </span>{' '}
            </Fragment>
            );
          })}
        </p>
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
