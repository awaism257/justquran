import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { ChevronRight } from 'lucide-react';
import BackBar from '@/components/BackBar';
import { JUZ_NAMES, JUZ_NAMES_AR, JUZ_STARTS, getSurahMeta, loadBundle } from '@/lib/data';
import type { Bundle } from '@/lib/data';

export default function JuzIndex() {
  const [bundle, setBundle] = useState<Bundle | null>(null);

  useEffect(() => {
    loadBundle().then(setBundle).catch(() => {});
  }, []);

  return (
    <div className="tiles" style={{ minHeight: '100dvh' }}>
      <BackBar title="Juz" meta="30 ajzāʾ" />
      <div className="flex flex-col gap-3 px-4 py-4">
        {JUZ_STARTS.map((j) => {
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
