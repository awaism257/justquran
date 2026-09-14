import { useEffect, useRef, useState } from 'react';
import { Bookmark, Check, Copy, ListMusic, Pause, Play, SkipBack, SkipForward, X } from 'lucide-react';
import type { Verse } from '@/lib/data';
import { loadBundle } from '@/lib/data';
import { playVerse } from '@/lib/audio';
import { shareVerse } from '@/lib/share';
import { isBookmarked, toggleBookmark } from '@/lib/bookmarks';
import { useSettings } from '@/lib/settings';
import { AUTO_ADVANCE_EVENT } from '@/components/VerseCard';

interface VersePopupProps {
  verse: Verse; // current verse
  hasPrev: boolean;
  hasNext: boolean;
  onPrev: () => void;
  onNext: () => void;
  onClose: () => void;
}

/** Popup card opened by tapping a folio verse marker. */
export default function VersePopup({ verse, hasPrev, hasNext, onPrev, onNext, onClose }: VersePopupProps) {
  const [playing, setPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [copied, setCopied] = useState(false);
  const [bookmarked, setBookmarked] = useState(() => isBookmarked(verse.s, verse.v));
  const stopRef = useRef<(() => void) | null>(null);
  const { settings, toggle } = useSettings();
  const autoRef = useRef(settings.audioAutoAdvance);
  autoRef.current = settings.audioAutoAdvance;

  // keep bookmark state in sync (other cards, prev/next navigation)
  useEffect(() => {
    const sync = () => setBookmarked(isBookmarked(verse.s, verse.v));
    sync();
    window.addEventListener('jq-bookmarks-changed', sync);
    return () => window.removeEventListener('jq-bookmarks-changed', sync);
  }, [verse.s, verse.v]);

  const stop = () => {
    stopRef.current?.();
    stopRef.current = null;
    setPlaying(false);
  };

  const play = () => {
    stop();
    setPlaying(true);
    setProgress(0);
    const h = playVerse(verse.s, verse.v, (reason) => {
      stopRef.current = null;
      setPlaying(false);
      // Natural end + auto-advance on → continue with the next verse. Within
      // the surah we just move the popup; at the surah boundary FolioPage
      // swaps in the next surah's verses (114:6 = stop, no wrap).
      if (reason === 'ended' && autoRef.current) {
        if (hasNext) {
          wasPlaying.current = true;
          onNext();
        } else if (verse.s < 114) {
          void loadBundle()
            .then(() => {
              if (!autoRef.current) return;
              wasPlaying.current = true;
              window.dispatchEvent(
                new CustomEvent(AUTO_ADVANCE_EVENT, { detail: { s: verse.s + 1, v: 1 } }),
              );
            })
            .catch(() => {});
        }
      }
    }, (pct) => setProgress(pct));
    stopRef.current = h.stop;
  };

  // reset the "copied" tick when moving to another verse
  useEffect(() => setCopied(false), [verse.s, verse.v]);

  // restart audio when verse changes via prev/next, if currently playing
  const wasPlaying = useRef(false);
  useEffect(() => {
    if (wasPlaying.current) play();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [verse.s, verse.v]);

  useEffect(() => () => stopRef.current?.(), []);

  return (
    <div className="popup-overlay" onClick={onClose}>
      <div
        className="popup"
        onClick={(e) => e.stopPropagation()}
        onContextMenu={(e) => e.preventDefault()}
      >
        <div className="flex items-center justify-between">
          <span className="popup-label">
            Surah {verse.s} : {verse.v}
          </span>
          <span className="flex items-center gap-2">
            <button
              className="icon-btn"
              aria-label={bookmarked ? 'Remove bookmark' : 'Bookmark verse'}
              style={{ border: 'none' }}
              onClick={() => setBookmarked(toggleBookmark(verse.s, verse.v))}
            >
              <Bookmark
                size={16}
                style={bookmarked ? { color: '#a7c989' } : undefined}
                fill={bookmarked ? 'currentColor' : 'none'}
              />
            </button>
            <button
              className="icon-btn"
              aria-label={copied ? 'Copied' : 'Copy or share verse'}
              style={{ border: 'none' }}
              onClick={async () => {
                const r = await shareVerse(verse);
                if (r !== 'failed') {
                  setCopied(true);
                  setTimeout(() => setCopied(false), 1500);
                }
              }}
            >
              {copied ? <Check size={16} style={{ color: '#a7c989' }} /> : <Copy size={16} />}
            </button>
            <button className="icon-btn" aria-label="Close" onClick={onClose} style={{ border: 'none' }}>
              <X size={17} />
            </button>
          </span>
        </div>
        {verse.ur ? <p className="popup-ur">{verse.ur}</p> : null}
        <p className="popup-en">{verse.en}</p>
        <div className="popup-bar">
          <div style={{ width: `${progress}%` }} />
        </div>
        <div className="popup-controls">
          {/* Transport group is centred in the popup; the AUTO toggle is
              pinned to the far right so it can't pull the group off-centre. */}
          <div className="popup-transport">
            <button
              aria-label="Previous verse"
              disabled={!hasPrev}
              style={!hasPrev ? { opacity: 0.35 } : undefined}
              onClick={() => {
                wasPlaying.current = playing;
                onPrev();
              }}
            >
              <SkipBack size={16} />
            </button>
            <button
              className="play"
              aria-label={playing ? 'Pause' : 'Play'}
              onClick={() => {
                if (playing) stop();
                else play();
              }}
            >
              {playing ? <Pause size={18} /> : <Play size={18} />}
            </button>
            <button
              aria-label="Next verse"
              disabled={!hasNext}
              style={!hasNext ? { opacity: 0.35 } : undefined}
              onClick={() => {
                wasPlaying.current = playing;
                onNext();
              }}
            >
              <SkipForward size={16} />
            </button>
          </div>
          <button
            className="popup-auto"
            aria-label="Auto-play next verse"
            aria-pressed={settings.audioAutoAdvance}
            title="Continue to next verse automatically"
            onClick={() => toggle('audioAutoAdvance')}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 1,
              color: settings.audioAutoAdvance ? 'var(--green)' : 'var(--muted)',
            }}
          >
            <ListMusic size={15} />
            <span style={{ fontSize: 8, letterSpacing: '0.08em', textTransform: 'uppercase' }}>Auto</span>
          </button>
        </div>
      </div>
    </div>
  );
}
