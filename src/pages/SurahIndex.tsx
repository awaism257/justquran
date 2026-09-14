import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router';
import { ChevronRight, Search } from 'lucide-react';
import BackBar from '@/components/BackBar';
import { loadBundle } from '@/lib/data';
import type { Bundle, SurahMeta } from '@/lib/data';

function SurahRow({ s }: { s: SurahMeta }) {
  return (
    <Link to={`/surah/${s.n}`} className="card">
      <span className="vnum">{s.n}</span>
      <span className="min-w-0">
        <span className="block" style={{ fontSize: 16 }}>{s.name_en}</span>
        <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
          {s.name_meaning} · {s.ayahs} verses
        </span>
      </span>
      <span
        className="ml-auto shrink-0"
        style={{ fontFamily: "'DigitalKhatt IndoPak', 'Amiri Quran', serif", fontSize: 20, direction: 'rtl' }}
      >
        {s.name_ar}
      </span>
      <ChevronRight className="chev" size={18} />
    </Link>
  );
}

export default function SurahIndex() {
  const [bundle, setBundle] = useState<Bundle | null>(null);
  const [q, setQ] = useState('');

  useEffect(() => {
    loadBundle().then(setBundle).catch(() => {});
  }, []);

  const list = useMemo(() => {
    if (!bundle) return [];
    const query = q.trim().toLowerCase();
    if (!query) return bundle.surahs;
    return bundle.surahs.filter(
      (s) =>
        String(s.n) === query ||
        s.name_en.toLowerCase().includes(query) ||
        s.name_meaning.toLowerCase().includes(query) ||
        s.name_ar.includes(q.trim()),
    );
  }, [bundle, q]);

  return (
    <div className="tiles" style={{ minHeight: '100dvh' }}>
      <BackBar title="Surahs" meta="114 surahs" />
      <div className="px-4 pt-4">
        <div
          className="flex items-center gap-2 rounded-full px-4"
          style={{ border: '1px solid var(--line)', background: 'var(--card-bg)' }}
        >
          <Search size={16} style={{ color: 'var(--muted)', flexShrink: 0 }} />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Filter by name, meaning or number…"
            aria-label="Filter surahs"
            className="w-full bg-transparent py-2.5 outline-none"
            style={{ color: 'var(--text)', fontSize: 14, fontFamily: 'inherit' }}
          />
        </div>
      </div>
      <div className="flex flex-col gap-3 px-4 py-4">
        {list.map((s) => (
          <SurahRow key={s.n} s={s} />
        ))}
        {bundle && list.length === 0 && (
          <p className="py-10 text-center" style={{ color: 'var(--muted)', fontSize: 13 }}>
            No surah matches “{q}”.
          </p>
        )}
        {!bundle && (
          <p className="py-10 text-center" style={{ color: 'var(--muted)', fontSize: 13 }}>
            Loading…
          </p>
        )}
      </div>
    </div>
  );
}
