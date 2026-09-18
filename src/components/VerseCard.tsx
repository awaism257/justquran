import { useEffect, useRef, useState } from 'react';
import { Bookmark, Check, Copy, Volume2 } from 'lucide-react';
import { wqDisplay, wqInkOffset, loadBundle, getSurahMeta, JUZ_NAMES } from '@/lib/data';
import type { Verse } from '@/lib/data';
import { playVerse, stopAudio } from '@/lib/audio';
import { isBookmarked, toggleBookmark } from '@/lib/bookmarks';
import { shareVerse } from '@/lib/share';
import { useSettings } from '@/lib/settings';

/** Fired on window when a verse finished naturally and auto-advance wants s:v next. */
export const AUTO_ADVANCE_EVENT = 'jq:verse-auto-advance';

interface VerseCardProps {
  verse: Verse;
  showEn: boolean;
  showUr: boolean;
  showTr: boolean;
  onAnyPlay?: () => void; // let a parent stop other players
  active?: boolean; // currently recited during whole-surah playback
  /** Al-Fatiha 1:1 only — this verse IS the Bismillah, so its card carries
   *  the bismillah-band styling (surah name on top, solid gold border). */
  bandName?: string;
  /** Juz number when this verse is the first ayah of a juz — the card then
   *  carries a "Juz begins here" divider chip and a soft highlight. */
  juzStart?: number;
}

/**
 * Gold-trimmed verse card: number circle LEFT, speaker RIGHT.
 * Body order: Arabic → English → Urdu → Transliteration.
 * Long-press toggles bookmark.
 */
export default function VerseCard({ verse, showEn, showUr, showTr, onAnyPlay, active, bandName, juzStart }: VerseCardProps) {
  const [playing, setPlaying] = useState(false);
  const [copied, setCopied] = useState(false);
  const [bookmarked, setBookmarked] = useState(() => isBookmarked(verse.s, verse.v));
  const stopRef = useRef<(() => void) | null>(null);
  const pressTimer = useRef<number | null>(null);
  const longPressed = useRef(false);
  const { settings, toggle } = useSettings();
  const autoRef = useRef(settings.audioAutoAdvance);
  autoRef.current = settings.audioAutoAdvance;

  useEffect(() => {
    const sync = () => setBookmarked(isBookmarked(verse.s, verse.v));
    window.addEventListener('jq-bookmarks-changed', sync);
    return () => {
      window.removeEventListener('jq-bookmarks-changed', sync);
      stopRef.current?.();
    };
  }, [verse.s, verse.v]);

  const startPlay = () => {
    onAnyPlay?.();
    stopAudio();
    setPlaying(true);
    const h = playVerse(verse.s, verse.v, (reason) => {
      stopRef.current = null;
      setPlaying(false);
      // Natural end + auto-advance on → hand off to the next verse. The next
      // verse's card (same surah) picks this up; Reading handles surah changes.
      if (reason === 'ended' && autoRef.current) {
        void loadBundle()
          .then((b) => {
            const meta = getSurahMeta(b, verse.s);
            if (!meta) return;
            if (verse.v < meta.ayahs) {
              window.dispatchEvent(
                new CustomEvent(AUTO_ADVANCE_EVENT, { detail: { s: verse.s, v: verse.v + 1 } }),
              );
            } else if (verse.s < 114) {
              window.dispatchEvent(
                new CustomEvent(AUTO_ADVANCE_EVENT, { detail: { s: verse.s + 1, v: 1 } }),
              );
            }
          })
          .catch(() => {});
      }
    });
    stopRef.current = h.stop;
  };

  // Auto-advance hand-off: if this card is the requested next verse, start
  // playing it and bring it into view so the gold speaker follows playback.
  useEffect(() => {
    const onAdvance = (e: Event) => {
      const d = (e as CustomEvent<{ s: number; v: number }>).detail;
      if (!d || d.s !== verse.s || d.v !== verse.v || !autoRef.current) return;
      if (stopRef.current) return; // already playing
      startPlay();
      document.getElementById(`v${verse.v}`)?.scrollIntoView({ block: 'center', behavior: 'smooth' });
    };
    window.addEventListener(AUTO_ADVANCE_EVENT, onAdvance);
    return () => window.removeEventListener(AUTO_ADVANCE_EVENT, onAdvance);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [verse.s, verse.v]);

  const togglePlay = () => {
    if (playing) {
      stopRef.current?.();
      stopRef.current = null;
      setPlaying(false);
      return;
    }
    startPlay();
  };

  const startPress = () => {
    longPressed.current = false;
    pressTimer.current = window.setTimeout(() => {
      longPressed.current = true;
      setBookmarked(toggleBookmark(verse.s, verse.v));
    }, 500);
  };
  const endPress = () => {
    if (pressTimer.current) {
      window.clearTimeout(pressTimer.current);
      pressTimer.current = null;
    }
  };

  return (
    <article
      id={`v${verse.v}`}
      className={['vcard relative', playing && 'now-playing', active && 'reciting', bandName && 'band', juzStart && 'juz-start'].filter(Boolean).join(' ')}
      onMouseDown={startPress}
      onMouseUp={endPress}
      onMouseLeave={endPress}
      onTouchStart={startPress}
      onTouchEnd={endPress}
      onTouchMove={endPress}
      onContextMenu={(e) => e.preventDefault()}
    >
      {juzStart && (
        <div className="juz-chip" aria-label={`Juz ${juzStart} begins at this verse`}>
          ۞ Juz {juzStart} · {JUZ_NAMES[juzStart - 1]}
        </div>
      )}
      <div className="vcard-head">
        <span className="wqwrap">
          <span className="vnum">{verse.v}</span>
          {verse.wq && (
            <span
              className="wqmark"
              aria-hidden="true"
              style={{ transform: `translateX(calc(-50% + ${wqInkOffset(wqDisplay(verse.wq))}px))` }}
            >
              {wqDisplay(verse.wq)
                .split('')
                .map((c, ci) => (
                  <span key={ci} className="wqch">
                    {c}
                  </span>
                ))}
            </span>
          )}
        </span>
        <span className="flex items-center gap-2">
          <button
            className="spk"
            aria-label={bookmarked ? 'Remove bookmark' : 'Bookmark verse'}
            onClick={() => setBookmarked(toggleBookmark(verse.s, verse.v))}
            onMouseDown={(e) => e.stopPropagation()}
            onTouchStart={(e) => e.stopPropagation()}
          >
            <Bookmark
              size={15}
              style={bookmarked ? { color: 'var(--frame2)' } : undefined}
              fill={bookmarked ? 'currentColor' : 'none'}
            />
          </button>
          <button
            className="spk"
            aria-label={copied ? 'Copied' : 'Copy or share verse'}
            onClick={async () => {
              const r = await shareVerse(verse);
              if (r !== 'failed') {
                setCopied(true);
                setTimeout(() => setCopied(false), 1500);
              }
            }}
            onMouseDown={(e) => e.stopPropagation()}
            onTouchStart={(e) => e.stopPropagation()}
          >
            {copied ? <Check size={15} style={{ color: 'var(--green, #a7c989)' }} /> : <Copy size={15} />}
          </button>
          {playing && (
            <button
              className="spk auto-chip"
              aria-label="Auto-play next verse"
              aria-pressed={settings.audioAutoAdvance}
              title="Continue to next verse automatically"
              onClick={() => toggle('audioAutoAdvance')}
              onMouseDown={(e) => e.stopPropagation()}
              onTouchStart={(e) => e.stopPropagation()}
              style={
                settings.audioAutoAdvance
                  ? {
                      /* ON: solid fill matching the playing speaker button */
                      background: 'var(--green)',
                      borderColor: 'var(--green)',
                      color: 'var(--bg)',
                    }
                  : { color: 'var(--muted)' }
              }
            >
              Auto
            </button>
          )}
          <button
            className={playing ? 'spk playing' : 'spk'}
            aria-label={playing ? 'Stop recitation' : 'Play recitation'}
            onClick={togglePlay}
            onMouseDown={(e) => e.stopPropagation()}
            onTouchStart={(e) => e.stopPropagation()}
          >
            <Volume2 size={15} />
          </button>
        </span>
      </div>
      {bandName && <div className="bism-name vcard-bandname">{bandName}</div>}
      <p className="vc-ar">{verse.ar}</p>
      {showTr && <p className="vc-tr">{verse.tr}</p>}
      {showEn && <p className="vc-en">{verse.en}</p>}
      {showUr && verse.ur ? <p className="vc-ur">{verse.ur}</p> : null}
    </article>
  );
}
