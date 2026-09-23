import { useCallback, useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router';
import BackBar from '@/components/BackBar';
import BismillahBand from '@/components/BismillahBand';
import BismillahCard from '@/components/BismillahCard';
import FolioPage from '@/components/FolioPage';
import PagedFolio from '@/components/PagedFolio';
import VerseCard, { AUTO_ADVANCE_EVENT } from '@/components/VerseCard';
import { getSurahMeta, getSurahVerses, juzOf, juzStartAt, loadBundle } from '@/lib/data';
import type { Bundle, Verse } from '@/lib/data';
import {
  getCounts,
  globalPageIndex,
  globalPageTotal,
  subscribeCounts,
} from '@/lib/mushafPages';
import type { Pagination } from '@/lib/mushafPages';
import { setLastRead } from '@/lib/bookmarks';
import { playVerse, stopAudio, getPlayingVerse, getLastPlayedVerse } from '@/lib/audio';
import type { PlayHandle } from '@/lib/audio';
import SurahPlayer from '@/components/SurahPlayer';
import { useSettings } from '@/lib/settings';
import { BookOpenText, ChevronLeft, ChevronRight, Play, Rows3, Square } from 'lucide-react';

/** Cross-surah auto-advance: verse to auto-play once the target surah loads. */
let pendingAutoPlay: { s: number; v: number } | null = null;
/** Surah-chain AUTO: surah number to start playing (from verse 1) once loaded. */
let pendingSurahChain: number | null = null;

export default function Reading() {
  const params = useParams();
  const n = Math.min(114, Math.max(1, parseInt(params.n ?? '1', 10) || 1));
  const { settings, resolvedTheme, toggle, set } = useSettings();
  const [bundle, setBundle] = useState<Bundle | null>(null);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const touchStart = useRef<{ x: number; y: number; t: number } | null>(null);
  /** Mirrors pagedMode for callbacks whose effect deps don't include it. */
  const pagedRef = useRef(false);

  // ---- Whole-surah playback (sequential per-verse, auto-scroll) ----
  const [surahPlayV, setSurahPlayV] = useState<number | null>(null);
  const [surahPlaying, setSurahPlaying] = useState(false); // false while paused
  const [audioError, setAudioError] = useState(false); // last chain died on a load failure
  // AUTO toggle state, mirrored into a ref so the playback chain can read it
  const autoAdvanceRef = useRef(settings.audioAutoAdvance);
  autoAdvanceRef.current = settings.audioAutoAdvance;
  const [surahProgress, setSurahProgress] = useState(0); // % within current verse
  const surahGen = useRef(0);
  const surahHandle = useRef<PlayHandle | null>(null);
  // AUTO chip tapped off mid-chain: let the current verse finish, then stop
  // (checked in the verse-end callbacks before stepping to the next verse).
  const chainCancelRef = useRef(false);
  /** Stop the running chain after the current verse (audio keeps playing to its end). */
  const cancelChainAfterVerse = useCallback(() => {
    chainCancelRef.current = true;
  }, []);
  // Auto-scroll follows the recitation verse-by-verse — but the user's own
  // scrolling always wins: any manual wheel/touch pauses the follow, and it
  // resumes when the user uses the player controls or taps the playing verse.
  const followRef = useRef(true);

  /** Smooth-scroll `el` to the centre of the VISIBLE reading area — the
      viewport minus the fixed player's height (~185px). Manual scrollTo
      because scrollIntoView counts the cards' scroll-margin-top, which
      pushes the centred position ~75px too low. */
  const scrollToCentre = useCallback((el: Element | null | undefined) => {
    if (!el) return;
    const r = el.getBoundingClientRect();
    const visibleH = window.innerHeight - 185;
    const y = window.scrollY + r.top + r.height / 2 - visibleH / 2;
    window.scrollTo({ top: Math.max(0, y), behavior: 'smooth' });
  }, []);

  const stopSurahPlay = useCallback(() => {
    surahGen.current++;
    surahHandle.current = null;
    setSurahPlayV(null);
    setSurahPlaying(false);
    setSurahProgress(0);
    stopAudio();
  }, []);

  useEffect(() => {
    let live = true;
    loadBundle()
      .then((b) => live && setBundle(b))
      .catch((e) => live && setError(String(e)));
    return () => {
      live = false;
    };
  }, []);

  // ---- Paged mushaf: dynamic line-sliced pages at the chosen font scale ----
  /** Reading position: page = 0-based page within the CURRENT surah (each
      surah starts a new page); anchor = verse owning the first line of that
      page. Kept in ONE state so page turns and re-anchors stay atomic. */
  const [pos, setPos] = useState({ page: 0, anchor: 1 });
  const pageIdx = pos.page;
  /** Current surah's pagination, reported by PagedFolio after measuring. */
  const [pages, setPages] = useState<Pagination | null>(null);
  /** Verse to land on once the (new surah's) pagination arrives; -1 = last page. */
  const [jumpTarget, setJumpTarget] = useState<number | null>(null);
  /** Surah the entry-jump effect last computed for (fresh navigation → top). */
  const entrySurahRef = useRef(0);
  /** Per-surah page counts across the Quran (live + lazily probed). */
  const counts = useSyncExternalStore(subscribeCounts, getCounts);
  const handlePaginate = useCallback((pg: Pagination) => setPages(pg), []);

  const surah = bundle ? getSurahMeta(bundle, n) : undefined;
  const verses: Verse[] = useMemo(
    () => (bundle ? getSurahVerses(bundle, n) : []),
    [bundle, n],
  );
  // Display verse for the Bismillah audio prelude: Al-Fatiha 1:1's text,
  // translation and transliteration, renumbered v=0 on the CURRENT surah so
  // the player bar labels it "Bismillah" and skips bookmark/share actions.
  const bismillahVerse: Verse | null = useMemo(
    () => (bundle ? { ...getSurahVerses(bundle, 1)[0], s: n, v: 0 } : null),
    [bundle, n],
  );

  // stop audio when leaving / changing surah.
  // (last-read verse is recorded by the scroll effect below, from the
  //  verse actually on screen — not hardcoded to 1.)
  useEffect(() => {
    return () => stopSurahPlay();
  }, [n, stopSurahPlay]);

  // User-scroll-wins: any manual wheel / touch-drag pauses auto-follow.
  // It re-engages via the player controls or by tapping the playing verse.
  useEffect(() => {
    const pauseFollow = () => {
      followRef.current = false;
    };
    window.addEventListener('wheel', pauseFollow, { passive: true });
    window.addEventListener('touchmove', pauseFollow, { passive: true });
    return () => {
      window.removeEventListener('wheel', pauseFollow);
      window.removeEventListener('touchmove', pauseFollow);
    };
  }, []);

  const stopAll = useCallback(() => stopSurahPlay(), [stopSurahPlay]);

  // Cross-surah auto-advance (single-verse playback): when a surah's last
  // verse ends with the setting on, the playing card asks for (n+1):1. Its
  // card doesn't exist on this page, so navigate there and auto-play verse 1
  // once loaded. (114:6 ends the chain — no event is fired for surah 115.)
  useEffect(() => {
    const onAdvance = (e: Event) => {
      const d = (e as CustomEvent<{ s: number; v: number }>).detail;
      if (!d || d.s === n || d.s < 1 || d.s > 114) return;
      // VersePopup (folio mode) handles cross-surah advance itself.
      if (document.querySelector('.popup-overlay')) return;
      pendingAutoPlay = d;
      navigate(`/surah/${d.s}`);
    };
    window.addEventListener(AUTO_ADVANCE_EVENT, onAdvance);
    return () => window.removeEventListener(AUTO_ADVANCE_EVENT, onAdvance);
  }, [n, navigate]);

  // After a cross-surah auto-advance navigation, hand verse 1 to its card.
  useEffect(() => {
    if (!pendingAutoPlay || pendingAutoPlay.s !== n || verses.length === 0) return;
    const target = pendingAutoPlay;
    pendingAutoPlay = null;
    const raf = requestAnimationFrame(() =>
      requestAnimationFrame(() =>
        window.dispatchEvent(new CustomEvent(AUTO_ADVANCE_EVENT, { detail: target })),
      ),
    );
    return () => cancelAnimationFrame(raf);
  }, [bundle, n, verses]);

  /** Start (or restart) the surah playback chain from verse `from`. */
  const startSurahFrom = useCallback(
    (from: number, skipBismillah = false) => {
      if (!surah) return;
      followRef.current = true; // (re)starting playback re-engages auto-follow
      chainCancelRef.current = false; // (re)starting also re-arms continuation
      setAudioError(false);
      const gen = ++surahGen.current;
      /** Audio failed to load (offline, or the host is unreachable): stop the
          chain and say so — never advance, or the page races to the surah's
          end playing nothing. */
      const failChain = () => {
        surahHandle.current = null;
        setSurahPlayV(null);
        setSurahPlaying(false);
        setSurahProgress(0);
        setAudioError(true);
      };
      const step = (v: number) => {
        if (surahGen.current !== gen) return;
        if (v > surah.ayahs) {
          surahHandle.current = null;
          setSurahPlayV(null);
          setSurahPlaying(false);
          setSurahProgress(0);
          // AUTO on → continue straight into the next surah (114 ends the chain)
          if (autoAdvanceRef.current && n < 114) {
            pendingSurahChain = n + 1;
            navigate(`/surah/${n + 1}`);
          }
          return;
        }
        setSurahPlayV(v);
        setSurahPlaying(true);
        setSurahProgress(0);
        if (followRef.current) scrollToCentre(document.getElementById(`v${v}`));
        surahHandle.current = playVerse(
          n,
          v,
          (reason) => {
            if (surahGen.current !== gen) return;
            if (reason === 'error') {
              failChain();
              return;
            }
            // AUTO turned off mid-chain → finish this verse, then stop.
            if (chainCancelRef.current) {
              chainCancelRef.current = false;
              surahHandle.current = null;
              setSurahPlayV(null);
              setSurahPlaying(false);
              setSurahProgress(0);
              return;
            }
            step(v + 1);
          },
          (pct) => {
            if (surahGen.current === gen) setSurahProgress(pct);
          },
        );
      };
      // Every surah opens with the Bismillah — except Al-Fatiha (where it IS
      // verse 1) and At-Tawbah (no Bismillah by convention). When starting
      // from the top of any other surah, play Al-Hussary's 1:1 as a prelude.
      if (from === 1 && !skipBismillah && n !== 1 && n !== 9) {
        setSurahPlayV(0); // sentinel: Bismillah playing (non-null → toggle stops it)
        setSurahPlaying(true);
        setSurahProgress(0);
        // bring the bismillah band into view, mirroring per-verse auto-scroll
        if (followRef.current) scrollToCentre(document.querySelector('.bism'));
        surahHandle.current = playVerse(
          1,
          1,
          (reason) => {
            if (surahGen.current !== gen) return;
            if (reason === 'error') {
              failChain();
              return;
            }
            if (chainCancelRef.current) {
              chainCancelRef.current = false;
              surahHandle.current = null;
              setSurahPlayV(null);
              setSurahPlaying(false);
              setSurahProgress(0);
              return;
            }
            step(1);
          },
          (pct) => {
            if (surahGen.current === gen) setSurahProgress(pct);
          },
        );
        return;
      }
      step(from);
    },
    [surah, n, scrollToCentre, navigate],
  );

  // AUTO chain hand-off: arriving here from the previous surah's end →
  // start playing this surah from verse 1 (with its Bismillah prelude).
  useEffect(() => {
    if (pendingSurahChain !== n || !surah || verses.length === 0) return;
    pendingSurahChain = null;
    const raf = requestAnimationFrame(() => startSurahFrom(1));
    return () => cancelAnimationFrame(raf);
  }, [surah, n, verses, startSurahFrom]);

  /** Play/stop toggle for the chips row: play from verse 1, or stop. */
  const toggleSurahPlay = useCallback(() => {
    if (surahPlayV !== null) {
      stopSurahPlay();
      return;
    }
    startSurahFrom(1);
  }, [surahPlayV, stopSurahPlay, startSurahFrom]);

  /** Pause/resume the current verse without breaking the chain. */
  const toggleSurahPause = useCallback(() => {
    const h = surahHandle.current;
    if (!h) return;
    if (surahPlaying) {
      h.audio.pause();
      setSurahPlaying(false);
    } else {
      void h.audio.play().catch(() => {});
      setSurahPlaying(true);
      // resuming from the player re-engages auto-follow
      followRef.current = true;
      if (surahPlayV !== null && surahPlayV > 0)
        scrollToCentre(document.getElementById(`v${surahPlayV}`));
    }
  }, [surahPlaying, surahPlayV, scrollToCentre]);

  /** Skip to an adjacent verse (restarts the chain there). */
  const skipSurahVerse = useCallback(
    (delta: number) => {
      if (surahPlayV === null || !surah) return;
      const target = Math.min(surah.ayahs, Math.max(1, surahPlayV + delta));
      // Skipping forward out of the Bismillah prelude → straight to verse 1.
      startSurahFrom(target, surahPlayV === 0 && delta > 0);
    },
    [surahPlayV, surah, startSurahFrom],
  );

  // Continuous folio only when verse-by-verse off AND all translations off.
  const folioMode =
    !settings.verseByVerse && !settings.showEn && !settings.showUr && !settings.showTr;

  // Paged mushaf: the folio is sliced into dynamic pages (whole lines at the
  // chosen font scale) instead of one scrolling page.
  const pagedMode = folioMode && settings.mushafPaged;
  pagedRef.current = pagedMode;
  /** This surah's measured pagination (null until PagedFolio reports). */
  const surahPages = pages && pages.surah === n ? pages : null;
  const surahPageCount = surahPages ? surahPages.count : 1;

  // ---- Scroll behaviour ----
  // 1) #v218 hash (search/bookmark/continue-reading verse jump) → scroll to that verse.
  // 2) ?end=1 (swipe into previous surah) → bottom of surah.
  // 3) otherwise (surah list, juz list, pager — no explicit target) → top of the surah.
  const location = useLocation();

  // Header juz label must track the verse ACTUALLY at the top of the screen:
  // a surah can span a juz boundary (e.g. Az-Zumar crosses into Juz 24 at
  // v32), so computing it once from the surah's first verse goes stale as
  // soon as the reader scrolls past the boundary.
  const [topVerse, setTopVerse] = useState(1);
  useEffect(() => {
    setTopVerse(1);
    if (!bundle) return;
    let raf = 0;
    const update = () => {
      raf = 0;
      // Paged mushaf: nothing scrolls (the window is a transform), so there
      // is no meaningful "verse at scroll top" — leave topVerse alone.
      if (pagedRef.current) return;
      let cur = 1;
      document.querySelectorAll<HTMLElement>('[id^="v"]').forEach((el) => {
        const m = el.id.match(/^v(\d+)$/);
        if (m && el.getBoundingClientRect().top <= 140) cur = Math.max(cur, parseInt(m[1], 10));
      });
      setTopVerse(cur);
    };
    const onScroll = () => {
      if (!raf) raf = requestAnimationFrame(update);
    };
    window.addEventListener('scroll', onScroll, { passive: true });
    // Recompute once webfonts settle — the IndoPak metrics shift every
    // verse's offset, so a pre-font measurement lands on the wrong verse.
    void document.fonts?.ready.then(onScroll);
    onScroll();
    return () => {
      window.removeEventListener('scroll', onScroll);
      if (raf) cancelAnimationFrame(raf);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bundle, n]);

  // record the verse being read (drives Home's "Continue reading")
  useEffect(() => {
    if (!bundle) return;
    const mountAt = Date.now();
    const verseOnScreen = (): number => {
      let cur = 1;
      document.querySelectorAll<HTMLElement>('[id^="v"]').forEach((el) => {
        const m = el.id.match(/^v(\d+)$/);
        if (m && el.getBoundingClientRect().top <= 140) cur = Math.max(cur, parseInt(m[1], 10));
      });
      return cur;
    };
    let t: ReturnType<typeof setTimeout> | null = null;
    const save = () => {
      if (t) clearTimeout(t);
      t = setTimeout(() => {
        // Paged mushaf: a fixed page never scrolls — the paged-mode effect
        // records the page's first verse instead.
        if (pagedRef.current) return;
        // skip the first moments after mount, before the scroll position
        // has settled at its target (top / hash verse / end)
        if (Date.now() - mountAt > 500) setLastRead(n, verseOnScreen());
      }, 300);
    };
    window.addEventListener('scroll', save, { passive: true });
    save();
    return () => {
      window.removeEventListener('scroll', save);
      if (t) clearTimeout(t);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bundle, n]);

  useEffect(() => {
    if (!bundle || !surah) return;
    const m = location.hash.match(/^#v(\d+)$/);
    const end = searchParams.get('end') === '1';
    const raf = requestAnimationFrame(() =>
      requestAnimationFrame(() => {
        if (m) {
          // Scroll so the verse START sits just below the sticky header.
          // Manual offset (not scrollIntoView): in folio mode the anchor is
          // an inline span, where CSS scroll-margin is unreliable.
          const scrollToVerseStart = () => {
            const el = document.getElementById(`v${m[1]}`);
            if (!el) return;
            const y = el.getBoundingClientRect().top + window.scrollY - 150;
            window.scrollTo({ top: Math.max(0, y), behavior: 'auto' });
          };
          scrollToVerseStart();
          // Re-align once webfonts finish loading: the IndoPak font is
          // taller than the fallback, so the first scroll (done with
          // fallback metrics) lands off-target after the reflow.
          void document.fonts?.ready.then(scrollToVerseStart);
          return;
        }
        if (end) {
          window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'auto' });
          return;
        }
        // No explicit target (opened from the surah/juz list or the pager):
        // always start at the top — never inherit the previous page's scroll
        // position or resume a stale per-surah position.
        window.scrollTo({ top: 0, behavior: 'auto' });
      }),
    );
    return () => cancelAnimationFrame(raf);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bundle, n, location.hash, searchParams]);

  /** Turn to page `p` of the current surah, anchoring on its first verse. */
  const turnTo = useCallback(
    (p: number) => {
      setPos({ page: p, anchor: surahPages?.anchors[p] ?? 1 });
    },
    [surahPages],
  );

  /** Turn one page forward/backward; past a surah's last page comes the next
      surah's first page (and back across the boundary lands at ?end=1). A
      manual turn pauses audio follow, like manual scrolling does. */
  const nextPage = useCallback(() => {
    followRef.current = false;
    if (pageIdx < surahPageCount - 1) turnTo(pageIdx + 1);
    else if (n < 114) navigate(`/surah/${n + 1}`);
  }, [pageIdx, surahPageCount, n, navigate, turnTo]);
  const prevPage = useCallback(() => {
    followRef.current = false;
    if (pageIdx > 0) turnTo(pageIdx - 1);
    else if (n > 1) navigate(`/surah/${n - 1}?end=1`);
  }, [pageIdx, n, navigate, turnTo]);

  // Horizontal swipe navigation, mushaf convention (the page turn moves
  // LEFT → RIGHT to go forward): drag right (dx > 0) → next surah (start);
  // drag left (dx < 0) → previous surah at its LAST page (?end=1).
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
      if (pagedMode) {
        // Paged mushaf: the swipe turns PAGES (drag right = next page).
        if (dx > 0) nextPage();
        else prevPage();
        return;
      }
      if (dx < 0 && n > 1) navigate(`/surah/${n - 1}?end=1`);
      else if (dx > 0 && n < 114) navigate(`/surah/${n + 1}`);
    },
    [n, navigate, pagedMode, nextPage, prevPage],
  );

  const arabicOnly = !settings.showEn && !settings.showUr && !settings.showTr;
  // v113: the exact selection is remembered in the persisted settings
  // (preMushaf) and restored precisely on the way back — no more "everything
  // switches back on". Nothing remembered (never entered mushaf this way)
  // still gets all translations on — the helpful first-exit default.
  const toggleArabicOnly = () => {
    if (arabicOnly) {
      const pm = settings.preMushaf;
      if (pm) {
        set({ showEn: pm.en, showUr: pm.ur, showTr: pm.tr, verseByVerse: pm.verseByVerse });
      } else {
        set({ showEn: true, showUr: true, showTr: true, verseByVerse: true });
      }
    } else {
      set({
        preMushaf: {
          en: settings.showEn,
          ur: settings.showUr,
          tr: settings.showTr,
          verseByVerse: settings.verseByVerse,
        },
        showEn: false,
        showUr: false,
        showTr: false,
        verseByVerse: false,
      });
    }
  };

  // Continuous folio only when verse-by-verse off AND all translations off.
  // (folioMode / pagedMode are declared above, before the scroll effects.)

  // Paged entry anchors: ?v= / #v → that verse's page; ?end=1 → the surah's
  // last page; toggling paged on mid-read → the verse currently at the top;
  // a fresh surah navigation → verse 1. The target is a VERSE — it is mapped
  // to a page once the surah's pagination arrives (see the next effect).
  useEffect(() => {
    if (!pagedMode || !surah) return;
    const m = location.hash.match(/^#v(\d+)$/);
    const freshNav = entrySurahRef.current !== n;
    if (m) {
      setJumpTarget(Math.min(surah.ayahs, Math.max(1, parseInt(m[1], 10))));
    } else if (searchParams.get('end') === '1') {
      setJumpTarget(-1); // last page
    } else if (freshNav) {
      setJumpTarget(1);
    } else {
      setJumpTarget(Math.min(surah.ayahs, Math.max(1, topVerse)));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagedMode, n, location.hash, searchParams, surah]);

  // Tracks the surah of the last navigation in ANY mode (declared after the
  // entry effect so the entry effect still sees the previous surah). This is
  // what lets "toggling paged on mid-read" be told apart from a fresh surah
  // navigation even though the entry effect only runs in paged mode.
  useEffect(() => {
    entrySurahRef.current = n;
  }, [n]);

  // Position anchoring by verse. Whenever a (re)pagination for this surah
  // arrives: a pending jump lands on the page CONTAINING the target verse;
  // otherwise the page holding the anchor verse (the verse that owned the
  // first line of the page on screen) is found again — never a raw index.
  useEffect(() => {
    if (!surahPages) return;
    if (jumpTarget !== null) {
      const p =
        jumpTarget === -1
          ? surahPages.count - 1
          : (surahPages.pageOfVerse[Math.max(1, jumpTarget)] ?? 0);
      setPos({ page: p, anchor: surahPages.anchors[p] ?? 1 });
      setJumpTarget(null);
      return;
    }
    setPos((cur) => {
      let p = 0;
      for (let i = 0; i < surahPages.anchors.length; i++) {
        if (surahPages.anchors[i] <= cur.anchor) p = i;
        else break;
      }
      const a = surahPages.anchors[p] ?? 1;
      return cur.page === p && cur.anchor === a ? cur : { page: p, anchor: a };
    });
  }, [surahPages, jumpTarget]);

  // Audio follow in paged mode: the playing verse not on the current page →
  // turn to its page (mirrors the vertical folio's auto-scroll follow).
  useEffect(() => {
    if (!pagedMode || !surahPages || surahPlayV === null || surahPlayV === 0) return;
    if (!followRef.current) return;
    const p = surahPages.pageOfVerse[surahPlayV] ?? 0;
    setPos((cur) =>
      cur.page === p ? cur : { page: p, anchor: surahPages.anchors[p] ?? cur.anchor },
    );
  }, [pagedMode, surahPages, surahPlayV]);

  // Continue-reading in paged mode: record the first verse of the current
  // page (the scroll-based recorder above is skipped — a page never scrolls).
  useEffect(() => {
    if (!pagedMode || !surahPages) return;
    const v = surahPages.anchors[Math.min(pageIdx, surahPages.anchors.length - 1)] ?? 1;
    const t = setTimeout(() => setLastRead(n, v), 400);
    return () => clearTimeout(t);
  }, [pagedMode, surahPages, pageIdx, n]);

  // Layout switch mid-playback: folio and card layouts have completely
  // different heights, so the old scroll position points at a random verse
  // after a toggle. When follow is engaged, re-centre the verse that is
  // playing RIGHT NOW once the new layout has rendered (double rAF waits
  // for the reflow). Also covers translation toggles / font-slider changes.
  const layoutSig = [
    folioMode,
    settings.verseByVerse,
    settings.showEn,
    settings.showUr,
    settings.showTr,
    settings.fonts.ar,
    settings.fonts.en,
    settings.fonts.ur,
    settings.fonts.tr,
  ].join('|');
  const layoutSigRef = useRef(layoutSig);
  useEffect(() => {
    if (layoutSigRef.current === layoutSig) return; // not a layout change
    layoutSigRef.current = layoutSig;
    // Whole-surah playback: re-centre the reciting verse.
    // Single-verse playback (card speaker / folio popup): the card or popup
    // unmounts on a folio↔card switch, so re-scroll the NEW layout to the
    // verse that is (or was just) playing instead of landing at a random spot.
    let target: number | null = null;
    if (surahPlayV !== null && surahPlayV > 0 && followRef.current) {
      target = surahPlayV;
    } else {
      const pv = getPlayingVerse() ?? getLastPlayedVerse();
      if (pv && pv.s === n) target = pv.v;
    }
    if (target === null) return;
    const v = target;
    requestAnimationFrame(() =>
      requestAnimationFrame(() => scrollToCentre(document.getElementById(`v${v}`))),
    );
  }, [layoutSig, surahPlayV, scrollToCentre, n]);

  // Theme-tied folio paper: dark theme → .night, unless "always cream" override.
  const night = resolvedTheme === 'dark' && !settings.alwaysCreamPage;

  // Juz label: tracks the verse at the scroll top in flowing layouts; in
  // paged mode it tracks the verse at the top of the page on screen.
  const metaVerse =
    pagedMode && surahPages
      ? (surahPages.anchors[Math.min(pageIdx, surahPages.anchors.length - 1)] ?? 1)
      : topVerse;
  const meta = surah ? `${surah.ayahs} verses · Juz ${juzOf(n, metaVerse)}` : undefined;

  // Global "Page X of Y" across the whole Quran at this font scale/frame —
  // X once every earlier surah's page count is known (the current surah is
  // measured live, earlier ones lazily), Y once all 114 are; surah-local
  // numbers until then.
  const globalX = surahPages ? globalPageIndex(counts, n, pageIdx) : null;
  const globalY = globalPageTotal(counts);
  const pageLabel = surahPages
    ? globalX !== null
      ? globalY !== null
        ? `Page ${globalX} of ${globalY}`
        : `Page ${globalX}`
      : `Page ${pageIdx + 1} of ${surahPageCount}`
    : 'Page …';

  // Card-mode layer chips: the last visible text layer can never be switched
  // off (toggling any other layer on first re-enables it).
  const layersOn = [settings.showAr, settings.showEn, settings.showUr, settings.showTr].filter(
    Boolean,
  ).length;
  const lastLayer = (on: boolean) => on && layersOn === 1;

  return (
    <div
      className={n === 1 ? 'tiles fatiha-bg' : 'tiles'}
      style={
        pagedMode
          ? // Paged mushaf: the whole screen is a fixed page — nothing scrolls.
            { height: '100dvh', display: 'flex', flexDirection: 'column' }
          : { minHeight: '100dvh' }
      }
    >
      <BackBar
        home
        title={surah ? `${n}. ${surah.name_en}` : `Surah ${n}`}
        meta={meta}
        actions={
          <>
            {/* Folio (mushaf) view has no chips row — the surah-chain play /
                stop lives here in the top bar instead. Same behaviour as the
                card mode's "▶ Play surah" chip. */}
            {folioMode && (
              <button
                className="icon-btn"
                aria-label={surahPlayV !== null ? 'Stop recitation' : 'Play surah'}
                title={surahPlayV !== null ? 'Stop recitation' : 'Play surah'}
                onClick={toggleSurahPlay}
                style={
                  surahPlayV !== null
                    ? { color: 'var(--green)', borderColor: 'var(--green)', flexShrink: 0 }
                    : { flexShrink: 0 }
                }
              >
                {surahPlayV !== null ? <Square size={16} /> : <Play size={18} />}
              </button>
            )}
            <button
              className="icon-btn"
              aria-label={arabicOnly ? 'Show verses with translations' : 'Read Arabic only (continuous)'}
              title={arabicOnly ? 'Verse-by-verse view' : 'Arabic-only continuous view'}
              onClick={toggleArabicOnly}
              style={
                arabicOnly
                  ? { color: 'var(--green)', borderColor: 'var(--green)', flexShrink: 0 }
                  : { flexShrink: 0 }
              }
            >
              {arabicOnly ? <Rows3 size={18} /> : <BookOpenText size={18} />}
            </button>
          </>
        }
      />

      {/* Chips row (card mode only — the folio views have no chips; playback
          there lives in the top bar). "Arabic" shows/hides the Arabic line on
          the verse cards; entering the mushaf view happens only via the
          top-bar layout icon. A chip is disabled while it is the LAST visible
          text layer. */}
      {!folioMode && (
        <div className="chips">
          <button
            className={surahPlayV !== null ? 'chip on' : 'chip'}
            onClick={toggleSurahPlay}
          >
            {surahPlayV !== null
              ? surahPlayV === 0
                ? '■ Stop (Bismillah)'
                : `■ Stop (${surahPlayV})`
              : '▶ Play surah'}
          </button>
          <button
            className={settings.showAr ? 'chip on' : 'chip'}
            disabled={lastLayer(settings.showAr)}
            onClick={() => toggle('showAr')}
          >
            Arabic
          </button>
          <button
            className={settings.showEn ? 'chip on' : 'chip'}
            disabled={lastLayer(settings.showEn)}
            onClick={() => toggle('showEn')}
          >
            English
          </button>
          <button
            className={settings.showUr ? 'chip on' : 'chip'}
            disabled={lastLayer(settings.showUr)}
            onClick={() => toggle('showUr')}
          >
            اردو
          </button>
          <button
            className={settings.showTr ? 'chip on' : 'chip'}
            disabled={lastLayer(settings.showTr)}
            onClick={() => toggle('showTr')}
          >
            Translit
          </button>
        </div>
      )}

      {audioError && (
        <div className="audio-error" role="alert">
          Couldn't load the recitation audio. Check your internet connection,
          then tap <b>▶ Play</b> to try again.
        </div>
      )}

      <main
        className="px-4 pt-2"
        style={
          pagedMode
            ? {
                flex: 1,
                minHeight: 0,
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                position: 'relative',
                paddingBottom: surahPlayV !== null ? 175 : 8,
              }
            : surahPlayV !== null
              ? { paddingBottom: 175 }
              : undefined
        }
        onTouchStart={onTouchStartSwipe}
        onTouchEnd={onTouchEndSwipe}
        onClickCapture={(e) => {
          // tapping the currently-playing verse re-centres it and resumes follow
          if (surahPlayV === null || surahPlayV === 0) return;
          if (!(e.target as HTMLElement).closest?.(`#v${surahPlayV}`)) return;
          followRef.current = true;
          scrollToCentre(document.getElementById(`v${surahPlayV}`));
        }}
      >
        {error && (
          <p className="text-center py-10" style={{ color: 'var(--muted)' }}>
            Could not load Quran data.
          </p>
        )}
        {!bundle && !error && (
          <p className="text-center py-10" style={{ color: 'var(--muted)' }}>
            Loading…
          </p>
        )}

        {bundle && surah && (
          <>
            {/* Vertical folio keeps the plain band above the page (in paged
                mode the band appears only where a surah actually begins,
                inline inside the page — see PagedFolio); in card mode the
                Bismillah (with its translation) IS the first card. */}
            {folioMode && !pagedMode && n !== 9 && (
              <BismillahBand surahName={surah.name_en} reciting={surahPlayV === 0 || (n === 1 && surahPlayV === 1)} gold={n === 1} />
            )}

            {folioMode ? (
              pagedMode && bundle ? (
                <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
                  <PagedFolio
                    bundle={bundle}
                    surah={n}
                    page={pageIdx}
                    night={night}
                    activeV={surahPlayV}
                    fontScale={settings.fonts.ar}
                    recitingBand={
                      surahPlayV === 0 ? n : n === 1 && surahPlayV === 1 ? 1 : null
                    }
                    onPaginate={handlePaginate}
                  />
                  {/* edge-tap page-turn zones (mushaf convention: left = forward) */}
                  <button
                    type="button"
                    className="book-edge"
                    style={{ left: 0 }}
                    aria-label="Next page"
                    onClick={nextPage}
                  >
                    <ChevronLeft size={14} />
                  </button>
                  <button
                    type="button"
                    className="book-edge"
                    style={{ right: 0 }}
                    aria-label="Previous page"
                    onClick={prevPage}
                  >
                    <ChevronRight size={14} />
                  </button>
                </div>
              ) : (
                <FolioPage verses={verses} night={night} activeV={surahPlayV} fatiha={n === 1} />
              )
            ) : (
              <div className="flex flex-col gap-3">
                {n !== 1 && n !== 9 && bismillahVerse && (
                  <BismillahCard
                    surahName={surah.name_en}
                    verse={bismillahVerse}
                    showEn={settings.showEn}
                    showUr={settings.showUr}
                    showTr={settings.showTr}
                    reciting={surahPlayV === 0}
                  />
                )}
                {verses.map((v) => (
                  <VerseCard
                    key={v.v}
                    verse={v}
                    showEn={settings.showEn}
                    showUr={settings.showUr}
                    showTr={settings.showTr}
                    onAnyPlay={stopAll}
                    // Ring only while the chain is ACTUALLY playing: when the
                    // user pauses, surahPlayV stays set and would otherwise
                    // leave a stale gold ring on a non-playing card.
                    active={surahPlaying && surahPlayV === v.v}
                    bandName={n === 1 && v.v === 1 ? surah.name_en : undefined}
                    juzStart={juzStartAt(n, v.v) ?? undefined}
                  />
                ))}
              </div>
            )}

            {/* End of the Quran — after the last verse of An-Nas (114:6),
                offer the dua upon completing the Quran. */}
            {n === 114 && (
              <Link
                to="/khatm"
                className="card"
                style={{ marginTop: 6, border: '1px solid var(--green)', textDecoration: 'none' }}
              >
                <span style={{ color: 'var(--green)', flexShrink: 0 }}>
                  <BookOpenText size={18} />
                </span>
                <span>
                  <span className="block" style={{ fontSize: 15 }}>
                    You have reached the end of the Quran
                  </span>
                  <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
                    The dua upon completing the Quran →
                  </span>
                </span>
              </Link>
            )}

            {/* Bottom pager. Paged mushaf: ‹ next page | Page X of Y | prev ›
                (mushaf convention: the left arrow goes FORWARD; X/Y are global
                across the Quran once every surah's page count is known).
                Otherwise: ← next surah | N verses | prev surah → */}
            {pagedMode ? (
              <nav className="pager">
                {pageIdx < surahPageCount - 1 || n < 114 ? (
                  <button type="button" className="pager-link" onClick={nextPage}>
                    ‹ Next
                  </button>
                ) : (
                  <span className="pager-link disabled">‹ Next</span>
                )}
                <span className="count">{pageLabel}</span>
                {pageIdx > 0 || n > 1 ? (
                  <button type="button" className="pager-link" onClick={prevPage}>
                    Previous ›
                  </button>
                ) : (
                  <span className="pager-link disabled">Previous ›</span>
                )}
              </nav>
            ) : (
            <nav className="pager">
              {n < 114 ? (
                <Link to={`/surah/${n + 1}`}>← {getSurahMeta(bundle, n + 1)?.name_en ?? `Surah ${n + 1}`}</Link>
              ) : (
                <span className="pager-link disabled">← Next</span>
              )}
              <span className="count">{surah.ayahs} verses</span>
              {n > 1 ? (
                <Link to={`/surah/${n - 1}`}>{getSurahMeta(bundle, n - 1)?.name_en ?? `Surah ${n - 1}`} →</Link>
              ) : (
                <span className="pager-link disabled">Previous →</span>
              )}
            </nav>
            )}
          </>
        )}
      </main>

      {/* Bottom player card while the whole surah is reciting.
          During the Bismillah prelude (surahPlayV === 0) it shows the
          Bismillah's own text, translation and transliteration. */}
      {surahPlayV !== null && verses.length > 0 && bismillahVerse && (
        <SurahPlayer
          verse={surahPlayV === 0 ? bismillahVerse : verses[surahPlayV - 1]}
          playing={surahPlaying}
          progress={surahProgress}
          hasPrev={surahPlayV > 1}
          hasNext={surahPlayV < verses.length}
          auto={settings.audioAutoAdvance || surahPlayV !== null}
          onToggleAuto={() => {
            // Filled = "audio will continue automatically". Tapping it off
            // stops ALL continuation: the setting AND the running chain
            // (the current verse plays out, then playback stops).
            if (settings.audioAutoAdvance) toggle('audioAutoAdvance');
            cancelChainAfterVerse();
          }}
          onTogglePlay={toggleSurahPause}
          onPrev={() => skipSurahVerse(-1)}
          onNext={() => skipSurahVerse(1)}
          onStop={stopSurahPlay}
        />
      )}
    </div>
  );
}
