import { useEffect, useState } from 'react';
import { Bookmark, Check, Copy, ListMusic, Pause, Play, SkipBack, SkipForward, X } from 'lucide-react';
import type { Verse } from '@/lib/data';
import { shareVerse } from '@/lib/share';
import { isBookmarked, toggleBookmark } from '@/lib/bookmarks';

interface SurahPlayerProps {
  verse: Verse; // currently reciting verse
  playing: boolean; // false while paused
  progress: number; // 0-100 within the current verse
  hasPrev: boolean;
  hasNext: boolean;
  /** AUTO: when this surah ends, continue straight into the next one. */
  auto: boolean;
  onToggleAuto: () => void;
  onTogglePlay: () => void;
  onPrev: () => void;
  onNext: () => void;
  onStop: () => void;
}

/**
 * Bottom player card shown while a whole surah is playing. Compact by design:
 * label + bookmark/copy + progress + prev / play-pause / next controls only.
 * The verse text itself is read on the page (card or folio), which auto-scrolls
 * with the recitation — so the user's own text-size settings are respected.
 */
export default function SurahPlayer({
  verse,
  playing,
  progress,
  hasPrev,
  hasNext,
  auto,
  onToggleAuto,
  onTogglePlay,
  onPrev,
  onNext,
  onStop,
}: SurahPlayerProps) {
  const [copied, setCopied] = useState(false);
  const [bookmarked, setBookmarked] = useState(() => isBookmarked(verse.s, verse.v));

  // keep bookmark state in sync (verse changes as the recitation advances)
  useEffect(() => {
    const sync = () => setBookmarked(isBookmarked(verse.s, verse.v));
    sync();
    window.addEventListener('jq-bookmarks-changed', sync);
    return () => window.removeEventListener('jq-bookmarks-changed', sync);
  }, [verse.s, verse.v]);

  // reset the "copied" tick when the recitation moves to another verse
  useEffect(() => setCopied(false), [verse.s, verse.v]);

  return (
    <div className="surah-player" onContextMenu={(e) => e.preventDefault()}>
      <div className="flex items-center justify-between">
        <span className="popup-label">
          {verse.v === 0 ? 'Bismillah' : `Surah ${verse.s} : ${verse.v}`}
        </span>
        <span className="flex items-center gap-2">
          {verse.v !== 0 && (
            <>
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
            </>
          )}
          <button className="icon-btn" aria-label="Stop" onClick={onStop} style={{ border: 'none' }}>
            <X size={17} />
          </button>
        </span>
      </div>
      <div className="popup-bar">
        <div style={{ width: `${progress}%` }} />
      </div>
      <div className="popup-controls">
        {/* Transport stays centred; the AUTO toggle is pinned to the far
            right, same pattern as the single-verse popup. */}
        <div className="popup-transport">
          <button
            aria-label="Previous verse"
            disabled={!hasPrev}
            style={!hasPrev ? { opacity: 0.35 } : undefined}
            onClick={onPrev}
          >
            <SkipBack size={16} />
          </button>
          <button
            className="play"
            aria-label={playing ? 'Pause' : 'Play'}
            onClick={onTogglePlay}
          >
            {playing ? <Pause size={18} /> : <Play size={18} />}
          </button>
          <button
            aria-label="Next verse"
            disabled={!hasNext}
            style={!hasNext ? { opacity: 0.35 } : undefined}
            onClick={onNext}
          >
            <SkipForward size={16} />
          </button>
        </div>
        <button
          className="popup-auto"
          aria-label="Auto-play next surah"
          aria-pressed={auto}
          title="Continue to next surah automatically"
          onClick={onToggleAuto}
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 1,
            color: auto ? 'var(--green)' : 'var(--muted)',
          }}
        >
          <ListMusic size={15} />
          <span style={{ fontSize: 8, letterSpacing: '0.08em', textTransform: 'uppercase' }}>Auto</span>
        </button>
      </div>
    </div>
  );
}
