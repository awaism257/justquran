import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { BookMarked, BookOpen, ChevronRight, CircleHelp, HandHeart, History, Library, Rows3, Search, Settings } from 'lucide-react';
import { getLastRead } from '@/lib/bookmarks';
import type { LastRead } from '@/lib/bookmarks';
import { loadBundle } from '@/lib/data';
import type { Bundle } from '@/lib/data';
import { isAndroidApp } from '@/lib/androidApp';

function BigCard({
  to,
  icon,
  title,
  sub,
}: {
  to: string;
  icon: ReactNode;
  title: string;
  sub: string;
}) {
  return (
    <Link to={to} className="card">
      <span style={{ color: 'var(--green)', flexShrink: 0 }}>{icon}</span>
      <span>
        <span className="block" style={{ fontSize: 17 }}>{title}</span>
        <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
          {sub}
        </span>
      </span>
      <ChevronRight size={18} className="chev" />
    </Link>
  );
}

export default function Home() {
  const [last, setLast] = useState<LastRead | null>(null);
  const [bundle, setBundle] = useState<Bundle | null>(null);

  useEffect(() => {
    setLast(getLastRead());
    loadBundle().then(setBundle).catch(() => {});
  }, []);

  const lastSurah = last ? bundle?.surahs.find((s) => s.n === last.s) : undefined;

  return (
    <div className="tiles flex flex-col" style={{ minHeight: '100dvh' }}>
      <header className="mhead flex items-start justify-between">
        <div>
          <h1 className="mhead-title">JustQuran</h1>
          <p className="mhead-sub">Arabic · اردو · English · Transliteration</p>
        </div>
        <nav className="mhead-icons">
          <Link className="icon-btn" to="/search" aria-label="Search">
            <Search size={17} />
          </Link>
          <Link className="icon-btn" to="/bookmarks" aria-label="Bookmarks">
            <BookMarked size={17} />
          </Link>
          <Link className="icon-btn" to="/settings" aria-label="Settings">
            <Settings size={17} />
          </Link>
        </nav>
      </header>
      <div className="greendiv" />

      <main className="px-4 pt-4 pb-8 flex-1 flex flex-col gap-3 justify-center">
        {last && (
          <Link to={`/surah/${last.s}#v${last.v}`} className="card">
            <span style={{ color: 'var(--green)', flexShrink: 0 }}>
              <History size={20} />
            </span>
            <span>
              <span className="block" style={{ fontSize: 17 }}>Continue reading</span>
              <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
                {lastSurah
                  ? `${lastSurah.name_en} · verse ${last.v}`
                  : `Surah ${last.s} · verse ${last.v}`}
              </span>
            </span>
            <ChevronRight size={18} className="chev" />
          </Link>
        )}
        <BigCard to="/surahs" icon={<BookOpen size={20} />} title="Surahs" sub="All 114 surahs" />
        <BigCard to="/juz" icon={<Rows3 size={20} />} title="Juz" sub="30 parts of the Quran" />
        {/* No Bookmarks card here — the header (top-right) already links to it. */}
        <BigCard
          to="/khatm"
          icon={<HandHeart size={20} />}
          title="Completing the Quran"
          sub="Khatm dua · دعائے ختمِ قرآن"
        />
        <BigCard
          to="/book"
          icon={<Library size={20} />}
          title="Translations"
          sub="Read translations as flowing pages"
        />
        <BigCard
          to="/help"
          icon={<CircleHelp size={20} />}
          title="How to use"
          sub="Quick guide — reading, search, bookmarks"
        />

        <p className="text-center" style={{ fontSize: 11.5, color: 'var(--muted)', marginTop: 22 }}>
          Fully offline · no ads · no tracking
        </p>
        {/* Required credit for the Netlify Open Source plan — keep on the main
            page of the website; hidden inside the Android app (no website there) */}
        {!isAndroidApp && (
        <p
          className="text-center"
          style={{ fontSize: 11.5, color: 'var(--muted)', margin: '24px 0 8px' }}
        >
          This site is powered by{' '}
          <a
            href="https://www.netlify.com"
            target="_blank"
            rel="noopener noreferrer"
            style={{ color: 'var(--green)', textDecoration: 'none' }}
          >
            Netlify
          </a>
        </p>
        )}
      </main>
    </div>
  );
}
