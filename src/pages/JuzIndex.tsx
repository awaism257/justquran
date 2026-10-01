import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router';
import { ChevronRight, Search } from 'lucide-react';
import BackBar from '@/components/BackBar';
import { JUZ_NAMES, JUZ_NAMES_AR, JUZ_STARTS, getSurahMeta, loadBundle } from '@/lib/data';
import type { Bundle } from '@/lib/data';

export default function JuzIndex() {
  const [bundle, setBundle] = useState<Bundle | null>(null);
  const [q, setQ] = useState('');

  useEffect(() => {
    loadBundle().then(setBundle).catch(() => {});
  }, []);

  const filteredJuz = useMemo(() => {
    const query = q.trim().toLowerCase();
    if (!query) return JUZ_STARTS;
    return JUZ_STARTS.filter((j) => {
      const num = String(j.juz);
      const enName = (JUZ_NAMES[j.juz - 1] ?? '').toLowerCase();
      const arName = JUZ_NAMES_AR[j.juz - 1] ?? '';
      return num === query || enName.includes(query) || arName.includes(q.trim());
    });
  }, [q]);

  return (
    <div className="tiles" style={{ minHeight: '100dvh' }}>
      <BackBar title="Juz" meta="30 ajzāʾ" />
      <div className="px-4 pt-4">
        <div
          className="flex items-center gap-2 rounded-full px-4"
          style={{ border: '1px solid var(--line)', background: 'var(--card-bg)' }}
        >
          <Search size={16} style={{ color: 'var(--muted)', flexShrink: 0 }} />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Filter by name or number…"
            className="w-full bg-transparent py-2.5 text-sm outline-none"
            style={{ color: 'var(--fg)' }}
          />
          {q && (
            <button
              onClick={() => setQ('')}
              className="text-xs"
              style={{ color: 'var(--muted)' }}
              aria-label="Clear filter"
            >
              Clear
            </button>
          )}
        </div>
      </div>
      <div className="flex flex-col gap-3 px-4 py-4">
        {filteredJuz.map((j) => {
          const next = JUZ_STARTS.find((x) => x.juz === j.juz + 1);
          // End of juz = verse before next juz start (or end of Quran for juz 30)
          let endS = 114;
          let endV = 6;
          if (next) {
            if (next.v === 1) {
              endS = next.s - 1;
              endV = bundle ? (getSurahMeta(bundle, endS)?.ayahs ?? 0) : 0;
            } else {
              endS = next.s;
              endV = next.v - 1;
            }
          }
          return (
            <Link key={j.juz} to={`/surah/${j.s}`} className="card">
              <span className="vnum">{j.juz}</span>
              <span className="min-w-0 flex-1">
                <span className="flex items-center gap-3">
                  <span style={{ fontSize: 16 }}>Juz {j.juz}</span>
                  <span
                    className="ml-auto shrink-0"
                    style={{ fontFamily: "'DigitalKhatt IndoPak', 'Amiri Quran', serif", fontSize: 20, direction: 'rtl', lineHeight: 1.2 }}
                  >
                    {JUZ_NAMES_AR[j.juz - 1]}
                  </span>
                </span>
                <span
                  className="block"
                  style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
                >
                  {JUZ_NAMES[j.juz - 1]} · {j.s}:{j.v} → {endS}:{endV}
                </span>
              </span>
              <ChevronRight className="chev" size={18} />
            </Link>
          );
        })}
      </div>
    </div>
  );
}
