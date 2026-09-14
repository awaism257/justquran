import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { ChevronRight, History, X } from 'lucide-react';
import BackBar from '@/components/BackBar';
import { getBookmarks, getLastRead, removeBookmark } from '@/lib/bookmarks';
import type { LastRead, VerseRef } from '@/lib/bookmarks';
import { getSurahMeta, getVerse, loadBundle } from '@/lib/data';
import type { Bundle } from '@/lib/data';

function firstWords(text: string, n: number): string {
  const words = text.split(/\s+/).filter(Boolean);
  if (words.length <= n) return text;
  return words.slice(0, n).join(' ') + '…';
}

export default function BookmarksPage() {
  const navigate = useNavigate();
  const [bundle, setBundle] = useState<Bundle | null>(null);
  const [marks, setMarks] = useState<VerseRef[]>([]);
  const [last, setLast] = useState<LastRead | null>(null);


  const refresh = useCallback(() => {
    setMarks(getBookmarks().slice().reverse()); // newest first
    setLast(getLastRead());
  }, []);

  useEffect(() => {
    loadBundle().then(setBundle).catch(() => {});
    refresh();
    const onChange = () => refresh();
    window.addEventListener('jq-bookmarks-changed', onChange);
    return () => window.removeEventListener('jq-bookmarks-changed', onChange);
  }, [refresh]);

  const remove = (s: number, v: number) => {
    removeBookmark(s, v);
    refresh();
  };

  const lastMeta = last && bundle ? getSurahMeta(bundle, last.s) : undefined;

  return (
    <div className="tiles" style={{ minHeight: '100dvh' }}>
      <BackBar title="Bookmarks" meta={marks.length ? `${marks.length} saved` : undefined} />
      <div className="flex flex-col gap-3 px-4 py-4">
        {last && (
          <Link to={`/surah/${last.s}#v${last.v}`} className="card">
            <span style={{ color: 'var(--green)', flexShrink: 0 }}>
              <History size={20} />
            </span>
            <span className="min-w-0">
              <span className="block" style={{ fontSize: 16 }}>
                Last read
              </span>
              <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
                {lastMeta ? `${lastMeta.name_en} ` : ''}{last.s}:{last.v} ·{' '}
                {new Date(last.at).toLocaleDateString()}
              </span>
            </span>
            <ChevronRight className="chev" size={18} />
          </Link>
        )}

        {marks.map((b) => {
          const meta = bundle ? getSurahMeta(bundle, b.s) : undefined;
          const verse = bundle ? getVerse(bundle, b.s, b.v) : undefined;
          let timer: ReturnType<typeof setTimeout> | null = null;
          return (
            <div
              key={`${b.s}:${b.v}`}
              className="card"
              role="link"
              tabIndex={0}
              onClick={() => navigate(`/surah/${b.s}#v${b.v}`)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') navigate(`/surah/${b.s}#v${b.v}`);
              }}
              onTouchStart={() => {
                timer = setTimeout(() => remove(b.s, b.v), 600);
              }}
              onTouchEnd={() => {
                if (timer) clearTimeout(timer);
              }}
              onTouchMove={() => {
                if (timer) clearTimeout(timer);
              }}
              onMouseDown={() => {
                timer = setTimeout(() => remove(b.s, b.v), 600);
              }}
              onMouseUp={() => {
                if (timer) clearTimeout(timer);
              }}
              onMouseLeave={() => {
                if (timer) clearTimeout(timer);
              }}
            >
              <span className="vnum">{b.v}</span>
              <span className="min-w-0 flex-1">
                <span className="block" style={{ fontSize: 15 }}>
                  {meta?.name_en ?? `Surah ${b.s}`} {b.s}:{b.v}
                </span>
                {verse && (
                  <span
                    className="block"
                    style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 2 }}
                  >
                    {firstWords(verse.en, 8)}
                  </span>
                )}
              </span>
              <button
                aria-label="Remove bookmark"
                className="icon-btn shrink-0"
                style={{ width: 32, height: 32, border: 'none', color: 'var(--muted)' }}
                onClick={(e) => {
                  e.stopPropagation();
                  remove(b.s, b.v);
                }}
              >
                <X size={16} />
              </button>
            </div>
          );
        })}

        {marks.length === 0 && (
          <p className="py-10 text-center" style={{ color: 'var(--muted)', fontSize: 13 }}>
            No bookmarks yet — long-press a verse while reading to save it here.
          </p>
        )}

        <p className="pt-2 pb-6 text-center" style={{ color: 'var(--muted)', fontSize: 11.5 }}>
          Stored locally · no account · no sync
        </p>
      </div>
    </div>
  );
}
