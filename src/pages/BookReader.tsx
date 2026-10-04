import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router';
import { ChevronLeft, ChevronRight, Pause, Play, SkipBack, SkipForward, X } from 'lucide-react';
import BackBar from '@/components/BackBar';
import VersePopup from '@/components/VersePopup';
import { bookTitle, getSurahMeta, getSurahVerses, loadBundle, loadSurahTitles } from '@/lib/data';
import type { Bundle, SurahTitles, Verse } from '@/lib/data';
import { setLastRead } from '@/lib/bookmarks';
import { useSettings } from '@/lib/settings';
import {
  bismillahUrl,
  playTranslationFile,
  setIdFor,
  stopTranslation,
  translationVerseUrl,
} from '@/lib/translationAudio';
import type { PlayHandle } from '@/lib/translationAudio';

/** Horizontal space around each text column: column-width = page width − GAP,
    column-gap = GAP, so every page gets GAP/2 of air on each side. */
const GAP = 48;

/** One item in the narration sequence: the spoken Bismillah (where the print
    has one) followed by every verse of the chapter. */
interface SeqItem {
  url: Promise<string>;
  verseIdx: number | null; // index into verses[], null = Bismillah
  labelVerse: Verse; // pseudo-verse for the player card label
}

/**
 * Book mode (v114): one surah's translation as flowing prose, paginated with
 * CSS multi-columns (one viewport-width column per page). Pages are turned by
 * translating the strip — never by scrolling (RTL scrollLeft is inconsistent
 * across browsers).
 *
 * v126: narration playback — the chapter read aloud in the same translation
 * (AI text-to-speech), streamed per verse from our own bucket, cached for
 * offline. Pages turn automatically to keep the narrated verse on screen.
 */
export default function BookReader() {
  const params = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const lang: 'en' | 'ur' = params.lang === 'ur' ? 'ur' : 'en';
  const n = Math.min(114, Math.max(1, parseInt(params.n ?? '1', 10) || 1));
  const { settings, set } = useSettings();
  const [bundle, setBundle] = useState<Bundle | null>(null);
  const [titles, setTitles] = useState<SurahTitles | null>(null);
  const [popupIdx, setPopupIdx] = useState<number | null>(null);

  useEffect(() => {
    let live = true;
    loadBundle()
      .then((b) => live && setBundle(b))
      .catch(() => {});
    loadSurahTitles()
      .then((t) => live && setTitles(t))
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

  // ---- narration playback ----
  const setId = setIdFor(lang, settings.transVoice);
  const [seq, setSeq] = useState<SeqItem[]>([]);
  const [playIdx, setPlayIdx] = useState(0); // index into seq
  const [playing, setPlaying] = useState(false);
  const [paused, setPaused] = useState(false);
  const [progress, setProgress] = useState(0);
  const handleRef = useRef<PlayHandle | null>(null);
  const playIdxRef = useRef(0);
  const seqRef = useRef<SeqItem[]>([]);
  const autoRef = useRef(settings.audioAutoAdvance);
  autoRef.current = settings.audioAutoAdvance;
  const pendingAutoRef = useRef(false); // cross-surah auto-continue
  const gapTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Build the narration sequence when chapter/voice/language changes.
  useEffect(() => {
    if (!verses.length) return;
    let live = true;
    const items: SeqItem[] = [];
    const push = async () => {
      const bUrl = await bismillahUrl(setId, n);
      if (bUrl) {
        items.push({
          url: Promise.resolve(bUrl),
          verseIdx: null,
          labelVerse: { s: n, v: 0, ar: '', en: '', tr: '' },
        });
      }
      for (let i = 0; i < verses.length; i++) {
        const v = verses[i];
        items.push({ url: translationVerseUrl(setId, n, v.v), verseIdx: i, labelVerse: v });
      }
      if (live) {
        seqRef.current = items;
        setSeq(items);
      }
    };
    void push();
    return () => {
      live = false;
    };
  }, [verses, setId, n]);

  /** Turn pages until the given verse marker sits inside the visible page. */
  const ensureVerseVisible = useCallback(
    (verseIdx: number) => {
      const vp = vpRef.current;
      const strip = stripRef.current;
      const el = markRefs.current[verseIdx];
      if (!vp || !strip || !el || vw <= 0) return;
      const r = el.getBoundingClientRect();
      const vr = vp.getBoundingClientRect();
      if (r.left >= vr.left - 1 && r.right <= vr.right + 1) return; // already visible
      // stripRect.left carries the current transform, so this difference is
      // the marker's true, transform-independent position inside the strip.
      const d = r.left - strip.getBoundingClientRect().left;
      const target =
        lang === 'ur'
          ? Math.floor((strip.scrollWidth - d) / vw) // rtl: pages run right→left
          : Math.floor(d / vw);
      setPageIdx(Math.max(0, Math.min(pageCount - 1, target)));
    },
    [lang, vw, pageCount],
  );

  const playItem = useCallback(
    (idx: number) => {
      const items = seqRef.current;
      const item = items[idx];
      if (!item) return;
      playIdxRef.current = idx;
      setPlayIdx(idx);
      setPlaying(true);
      setPaused(false);
      setProgress(0);
      if (item.verseIdx !== null) ensureVerseVisible(item.verseIdx);
      const h = playTranslationFile(
        item.url,
        (reason) => {
          handleRef.current = null;
          if (reason === 'ended') {
            const next = playIdxRef.current + 1;
            if (next < seqRef.current.length) {
              if (gapTimerRef.current) clearTimeout(gapTimerRef.current);
              gapTimerRef.current = setTimeout(() => {
                gapTimerRef.current = null;
                playItem(next);
              }, 1000);
            } else {
              // Chapter finished. AUTO continues into the next chapter.
              setPlaying(false);
              setPaused(false);
              if (autoRef.current && n < 114) {
                pendingAutoRef.current = true;
                navigate(`/book/${lang}/${n + 1}`);
              }
            }
          } else {
            setPlaying(false);
            setPaused(false);
          }
        },
        (pct) => setProgress(pct),
      );
      handleRef.current = h;
    },
    [ensureVerseVisible, navigate, lang, n],
  );

  const stopPlayback = useCallback(() => {
    if (gapTimerRef.current) {
      clearTimeout(gapTimerRef.current);
      gapTimerRef.current = null;
    }
    handleRef.current?.stop();
    handleRef.current = null;
    stopTranslation();
    setPlaying(false);
    setPaused(false);
    setProgress(0);
  }, []);

  // Cross-surah auto-continue: start the new chapter once its sequence exists.
  useEffect(() => {
    if (pendingAutoRef.current && seq.length) {
      pendingAutoRef.current = false;
      playIdxRef.current = 0;
      setPlayIdx(0);
      playItem(0);
    }
  }, [seq, playItem]);

  // Leaving the chapter / changing voice stops the narration.
  useEffect(() => {
    return () => {
      if (gapTimerRef.current) {
        clearTimeout(gapTimerRef.current);
        gapTimerRef.current = null;
      }
      handleRef.current?.stop();
      handleRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [n, lang, setId]);

  const togglePlay = useCallback(() => {
    if (gapTimerRef.current) {
      clearTimeout(gapTimerRef.current);
      gapTimerRef.current = null;
    }
    if (playing && !paused) {
      handleRef.current?.audio.pause();
      setPaused(true);
    } else if (playing && paused) {
      void handleRef.current?.audio.play().catch(() => {});
      setPaused(false);
    } else {
      playItem(playIdxRef.current);
    }
  }, [playing, paused, playItem]);

  const stepItem = useCallback(
    (delta: number) => {
      if (gapTimerRef.current) {
        clearTimeout(gapTimerRef.current);
        gapTimerRef.current = null;
      }
      const items = seqRef.current;
      const next = Math.max(0, Math.min(items.length - 1, playIdxRef.current + delta));
      if (playing || paused) playItem(next);
      else {
        playIdxRef.current = next;
        setPlayIdx(next);
        const it = items[next];
        if (it?.verseIdx !== null && it) ensureVerseVisible(it.verseIdx);
      }
    },
    [playing, paused, playItem, ensureVerseVisible],
  );

  // New surah / language → back to the first page, close any popup, stop audio.
  useEffect(() => {
    setPageIdx(0);
    setPopupIdx(null);
    playIdxRef.current = 0;
    setPlayIdx(0);
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

  // ---- long-press a verse (touch) → its popup, same as tapping the number ----
  const lpTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const lpFired = useRef(false);
  const lpStart = useRef<{ x: number; y: number } | null>(null);
  const cancelLongPress = useCallback(() => {
    if (lpTimer.current !== null) {
      clearTimeout(lpTimer.current);
      lpTimer.current = null;
    }
  }, []);
  const onVerseTouchStart = (i: number) => (e: React.TouchEvent) => {
    cancelLongPress();
    lpFired.current = false;
    const t = e.touches[0];
    lpStart.current = { x: t.clientX, y: t.clientY };
    lpTimer.current = setTimeout(() => {
      lpTimer.current = null;
      lpFired.current = true;
      setPopupIdx(i);
    }, 480);
  };
  const onVerseTouchMove = (e: React.TouchEvent) => {
    const s = lpStart.current;
    if (!s) return;
    const t = e.touches[0];
    if (Math.abs(t.clientX - s.x) > 10 || Math.abs(t.clientY - s.y) > 10) cancelLongPress();
  };
  const onVerseTouchEnd = () => {
    cancelLongPress();
    lpStart.current = null;
  };

  // ---- continue-reading & verse anchoring ----
  const [currentVisibleVerse, setCurrentVisibleVerse] = useState(1);
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
          setCurrentVisibleVerse(verses[i].v);
          return;
        }
      }
      setLastRead(n, verses[0].v);
      setCurrentVisibleVerse(verses[0].v);
    }, 350); // after the 0.25s page-turn transition settles
    return () => clearTimeout(t);
  }, [pageIdx, pageCount, verses, n]);

  // Jump to anchor verse passed via hash (#v...)
  useEffect(() => {
    if (!verses.length || vw <= 0) return;
    const m = location.hash.match(/^#v(\d+)$/);
    if (!m) return;
    const targetV = parseInt(m[1], 10);
    const targetIdx = verses.findIndex((v) => v.v === targetV);
    if (targetIdx >= 0) {
      const raf = requestAnimationFrame(() => {
        ensureVerseVisible(targetIdx);
      });
      return () => cancelAnimationFrame(raf);
    }
  }, [location.hash, verses, vw, ensureVerseVisible]);

  const popupVerse = popupIdx !== null ? (verses[popupIdx] ?? null) : null;
  // ltr: strip slides left (negative). rtl: columns flow right→left, so the
  // strip slides right (positive) to reach later pages.
  const offset = pageIdx * vw * (lang === 'ur' ? 1 : -1);

  const chapterTitle = surah ? bookTitle(titles, lang, surah) : `Surah ${n}`;
  const seqItem = seq[playIdx];

  return (
    <div className="tiles" style={{ height: '100dvh', display: 'flex', flexDirection: 'column' }}>
      <BackBar
        title={`${n}. ${chapterTitle}`}
        titleStyle={
          lang === 'ur'
            ? { fontFamily: "'Noto Nastaliq Urdu', serif", direction: 'rtl' }
            : undefined
        }
        actions={
          <span className="flex items-center gap-1">
            <button
              type="button"
              className="icon-btn"
              aria-label={playing ? 'Stop narration' : 'Listen to this chapter'}
              title={playing ? 'Stop narration' : 'Listen to this chapter'}
              onClick={() => (playing ? stopPlayback() : playItem(playIdxRef.current))}
            >
              <Play size={17} fill={playing ? 'currentColor' : 'none'} />
            </button>
          </span>
        }
      />
      <div
        className="px-4 pt-2"
        style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}
      >
        <div className="chips" style={{ background: 'transparent', padding: '6px 16px', gap: 8, flexShrink: 0 }}>
          <button
            type="button"
            className="chip"
            style={{ minWidth: 72 }}
            onClick={() => {
              stopPlayback();
              set({
                showEn: false,
                showUr: false,
                showTr: false,
                verseByVerse: false,
                mushafPaged: true,
              });
              navigate(`/surah/${n}#v${currentVisibleVerse}`);
            }}
          >
            Arabic
          </button>
          <button
            type="button"
            className={lang === 'en' ? 'chip on' : 'chip'}
            style={{ minWidth: 72 }}
            onClick={() => {
              if (lang !== 'en') {
                stopPlayback();
                navigate(`/book/en/${n}#v${currentVisibleVerse}`);
              }
            }}
          >
            English
          </button>
          <button
            type="button"
            className={lang === 'ur' ? 'chip on' : 'chip'}
            style={{ minWidth: 72, fontFamily: "'Noto Nastaliq Urdu', serif" }}
            onClick={() => {
              if (lang !== 'ur') {
                stopPlayback();
                navigate(`/book/ur/${n}#v${currentVisibleVerse}`);
              }
            }}
          >
            اردو
          </button>
        </div>
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
                <span
                  key={v.v}
                  onTouchStart={onVerseTouchStart(i)}
                  onTouchMove={onVerseTouchMove}
                  onTouchEnd={onVerseTouchEnd}
                  onContextMenu={(e) => {
                    // Swallow the browser's long-press menu only when OUR
                    // long-press already opened the popup (keeps copy intact).
                    if (lpFired.current) {
                      e.preventDefault();
                      lpFired.current = false;
                    }
                  }}
                  className={
                    playing && seqItem?.verseIdx === i ? 'verse-run reciting' : undefined
                  }
                >
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

        {/* Bottom bar: media controls replace page footer while narration audio is active */}
        {playing || paused ? (
          <div
            className="audio-bottom-bar"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              background: 'var(--card-bg, rgba(255, 255, 255, 0.05))',
              border: '1px solid var(--line)',
              borderRadius: 16,
              padding: '6px 12px',
              margin: '6px 0 12px',
            }}
          >
            <button
              type="button"
              className="icon-btn"
              style={{ border: 'none', padding: 4 }}
              aria-label="Previous verse"
              onClick={() => stepItem(-1)}
            >
              <SkipBack size={16} />
            </button>
            <button
              type="button"
              className="icon-btn"
              style={{ border: 'none', padding: 4 }}
              aria-label={paused ? 'Resume' : 'Pause'}
              onClick={togglePlay}
            >
              {paused ? <Play size={17} fill="currentColor" /> : <Pause size={17} fill="currentColor" />}
            </button>
            <button
              type="button"
              className="icon-btn"
              style={{ border: 'none', padding: 4 }}
              aria-label="Next verse"
              onClick={() => stepItem(1)}
            >
              <SkipForward size={16} />
            </button>

            <div style={{ flex: 1, minWidth: 0, padding: '0 4px' }}>
              <div
                style={{
                  fontSize: 12,
                  color: 'var(--muted)',
                  fontFamily: lang === 'ur' ? "'Noto Nastaliq Urdu', serif" : "'DejaVu Serif', serif",
                  direction: lang === 'ur' ? 'rtl' : 'ltr',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {seqItem?.verseIdx === null
                  ? lang === 'ur'
                    ? 'بسم اللہ'
                    : 'Bismillah'
                  : seqItem
                  ? lang === 'ur'
                    ? `آیت ${seqItem.labelVerse.v} از ${verses.length}`
                    : `Verse ${seqItem.labelVerse.v} of ${verses.length} · Translation`
                  : '…'}
              </div>
              <div
                style={{
                  height: 3,
                  borderRadius: 2,
                  background: 'var(--line)',
                  overflow: 'hidden',
                  width: '100%',
                  marginTop: 4,
                }}
              >
                <div
                  style={{
                    height: '100%',
                    width: `${Math.min(100, Math.max(0, progress))}%`,
                    background: 'var(--green, #a7c989)',
                    transition: 'width 0.1s linear',
                  }}
                />
              </div>
            </div>

            <button
              type="button"
              className="icon-btn"
              style={{ border: 'none', padding: 4, color: 'var(--muted)' }}
              aria-label="Stop playback"
              onClick={stopPlayback}
            >
              <X size={17} />
            </button>
          </div>
        ) : (
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
        )}
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
