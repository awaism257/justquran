import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router';
import { Search as SearchIcon } from 'lucide-react';
import BackBar from '@/components/BackBar';
import {
  getSurahMeta,
  loadBundle,
  stripArabicDiacritics,
  JUZ_NAMES,
  JUZ_NAMES_AR,
  JUZ_STARTS,
} from '@/lib/data';
import type { Bundle, SurahMeta, Verse } from '@/lib/data';
import { useSettings } from '@/lib/settings';

const MAX_RESULTS = 50;

/** Normalize Latin text for fuzzy name matching: "At-Tawbah" → "attawbah". */
const normLatin = (s: string) =>
  s
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[ʿʾ'`]/g, '')
    .toLowerCase()
    .replace(/[^a-z]/g, '');

/**
 * Fold Arabic/Urdu script variants after stripping diacritics, so both
 * keyboards match the same words: التوبة (Arabic ة) = التوبہ (Urdu ہ),
 * ک=ك, ی=ى=ي. Used for surah names, juz names and verse text alike.
 */
const normArScript = (s: string) =>
  stripArabicDiacritics(s)
    .replace(/\u06C1/g, '\u0647') // ہ → ه
    .replace(/\u0629/g, '\u0647') // ة → ه
    .replace(/\u06BE/g, '\u0647') // ھ → ه
    .replace(/\u0649/g, '\u064A') // ى → ي
    .replace(/\u06CC/g, '\u064A') // ی → ي
    .replace(/\u06A9/g, '\u0643'); // ک → ك

/** Eastern Arabic-Indic digits (٠-٩ Arabic, ۰-۹ Urdu/Persian) → Latin 0-9. */
const normDigits = (s: string) =>
  s
    .replace(/[\u0660-\u0669]/g, (d) => String(d.charCodeAt(0) - 0x0660))
    .replace(/[\u06F0-\u06F9]/g, (d) => String(d.charCodeAt(0) - 0x06f0));

const jumpLabelStyle: React.CSSProperties = {
  fontSize: 12,
  letterSpacing: '0.14em',
  color: 'var(--green)',
  textTransform: 'uppercase',
};

interface Hit {
  verse: Verse;
  inAr: boolean;
  inEn: boolean;
  inUr: boolean;
  inTr: boolean;
}

/** Wrap case-insensitive matches of `q` in <mark>. */
function Highlight({ text, q }: { text: string; q: string }) {
  if (!q) return <>{text}</>;
  const lower = text.toLowerCase();
  const needle = q.toLowerCase();
  const parts: { str: string; mark: boolean }[] = [];
  let i = 0;
  while (i < text.length) {
    const idx = lower.indexOf(needle, i);
    if (idx === -1) {
      parts.push({ str: text.slice(i), mark: false });
      break;
    }
    if (idx > i) parts.push({ str: text.slice(i, idx), mark: false });
    parts.push({ str: text.slice(idx, idx + needle.length), mark: true });
    i = idx + needle.length;
  }
  return (
    <>
      {parts.map((p, k) =>
        p.mark ? (
          <mark
            key={k}
            style={{ background: 'rgba(167,201,137,.35)', color: 'inherit', borderRadius: 2 }}
          >
            {p.str}
          </mark>
        ) : (
          <span key={k}>{p.str}</span>
        ),
      )}
    </>
  );
}

export default function SearchPage() {
  const { settings } = useSettings();
  const [bundle, setBundle] = useState<Bundle | null>(null);
  const [raw, setRaw] = useState('');
  const [q, setQ] = useState('');
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    loadBundle().then(setBundle).catch(() => {});
  }, []);

  // Debounce input by 250ms
  useEffect(() => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => setQ(raw.trim()), 250);
    return () => {
      if (timer.current) clearTimeout(timer.current);
    };
  }, [raw]);

  // Direct verse jump: "2:218", "2 218", "2.218", "2/218", "2-218"
  // — also with Eastern digits: ۲:۲۵۵ → Al-Baqarah verse 255
  const verseRef = useMemo(() => {
    if (!bundle) return null;
    const m = normDigits(q).match(/^(\d{1,3})(?:\s*[:./,\-]\s*|\s+)(\d{1,3})$/);
    if (!m) return null;
    const s = parseInt(m[1], 10);
    const v = parseInt(m[2], 10);
    if (s < 1 || s > 114) return null;
    const meta = getSurahMeta(bundle, s);
    if (!meta || v < 1 || v > meta.ayahs) return null;
    return { s, v, name: meta.name_en };
  }, [bundle, q]);

  // Surah name matches: "tawb" → At-Tawbah; also meanings ("cow") and
  // Arabic/Urdu-script names — التوبة and التوبہ both match (script folding).
  const surahMatches = useMemo<SurahMeta[]>(() => {
    if (!bundle || q.length < 2 || verseRef) return [];
    const lat = normLatin(q);
    const arq = normArScript(q);
    return bundle.surahs
      .filter(
        (s) =>
          (lat.length > 0 &&
            (normLatin(s.name_en).includes(lat) ||
              normLatin(s.name_meaning).includes(lat))) ||
          (arq.length > 0 && normArScript(s.name_ar).includes(arq)),
      )
      .sort((a, b) => {
        const ap = lat && normLatin(a.name_en).startsWith(lat) ? 0 : 1;
        const bp = lat && normLatin(b.name_en).startsWith(lat) ? 0 : 1;
        return ap - bp || a.n - b.n;
      })
      .slice(0, 3);
  }, [bundle, q, verseRef]);

  // Juz matches: by number ("juz 12", "para 3", "پارہ ۳۰", "جزء ٥") or by
  // name — transliterated ("amma", "tabarak") or Arabic/Urdu script (عم، تبارک).
  const juzMatches = useMemo(() => {
    if (!bundle || verseRef) return [];
    const m = normDigits(q).match(
      /^(?:juz|juzu|para|parah|sipara|پارہ|پارا|سپارہ|جزء?|جوز)\s*[:.\-]?\s*(\d{1,2})$/i,
    );
    if (m) {
      const jn = parseInt(m[1], 10);
      return jn >= 1 && jn <= 30 ? [JUZ_STARTS[jn - 1]] : [];
    }
    if (q.length < 2) return [];
    const lat = normLatin(q);
    const arq = normArScript(q);
    if (!lat && !arq) return [];
    return JUZ_STARTS.filter(
      (js) =>
        (lat && normLatin(JUZ_NAMES[js.juz - 1] ?? '').includes(lat)) ||
        (arq && normArScript(JUZ_NAMES_AR[js.juz - 1] ?? '').includes(arq)),
    ).slice(0, 3);
  }, [bundle, q, verseRef]);

  const hasJumps = !!verseRef || surahMatches.length > 0 || juzMatches.length > 0;

  const results = useMemo<Hit[]>(() => {
    if (!bundle || q.length < 2 || verseRef) return [];
    const enNeedle = q.toLowerCase();
    const arNeedle = normArScript(q);
    const hits: Hit[] = [];
    for (const v of bundle.verses) {
      const inEn = v.en.toLowerCase().includes(enNeedle);
      const inAr = arNeedle.length > 0 && normArScript(v.ar).includes(arNeedle);
      const inUr = typeof v.ur === 'string' && v.ur.includes(q);
      const inTr = settings.showTr && v.tr.toLowerCase().includes(enNeedle);
      if (inEn || inAr || inUr || inTr) {
        hits.push({ verse: v, inAr, inEn, inUr, inTr });
        if (hits.length >= MAX_RESULTS) break;
      }
    }
    return hits;
  }, [bundle, q, settings.showTr]);

  return (
    <div>
      <BackBar title="Search" meta="offline · 6,236 verses" />
      <div className="px-4 pt-4">
        <div
          className="flex items-center gap-2 rounded-full px-4"
          style={{ border: '1px solid var(--line)', background: 'var(--card-bg)' }}
        >
          <SearchIcon size={16} style={{ color: 'var(--muted)', flexShrink: 0 }} />
          <input
            value={raw}
            onChange={(e) => setRaw(e.target.value)}
            placeholder="Search text, surah or juz — or 2:218 to jump…"
            aria-label="Search the Quran"
            autoFocus
            className="w-full bg-transparent py-2.5 outline-none"
            style={{ color: 'var(--text)', fontSize: 14, fontFamily: 'inherit' }}
          />
        </div>
        {q.length > 0 && q.length < 2 && (
          <p className="pt-3 text-center" style={{ color: 'var(--muted)', fontSize: 12.5 }}>
            Type at least 2 characters.
          </p>
        )}
        {q.length >= 2 && bundle && !hasJumps && results.length === 0 && (
          <p className="pt-3" style={{ color: 'var(--muted)', fontSize: 12 }}>
            No results for “{q}”.
          </p>
        )}
        {q.length >= 2 && bundle && results.length > 0 && (
          <p className="pt-3" style={{ color: 'var(--muted)', fontSize: 12 }}>
            {`${results.length}${results.length >= MAX_RESULTS ? '+' : ''} verse result${results.length === 1 ? '' : 's'}`}
          </p>
        )}
        {verseRef && (
          <Link
            to={`/surah/${verseRef.s}#v${verseRef.v}`}
            className="card mt-3 block"
            style={{ display: 'block' }}
          >
            <div style={jumpLabelStyle}>Go to verse</div>
            <div style={{ marginTop: 4, fontSize: 15, color: 'var(--text)' }}>
              {verseRef.name} — verse {verseRef.v}
            </div>
          </Link>
        )}
        {surahMatches.map((s) => (
          <Link
            key={`surah-${s.n}`}
            to={`/surah/${s.n}`}
            className="card mt-3 block"
            style={{ display: 'block' }}
          >
            <div style={jumpLabelStyle}>Go to surah</div>
            <div style={{ marginTop: 4, fontSize: 15, color: 'var(--text)' }}>
              {s.n}. {s.name_en} — {s.name_meaning}
            </div>
          </Link>
        ))}
        {juzMatches.map((js) => (
          <Link
            key={`juz-${js.juz}`}
            to={`/surah/${js.s}#v${js.v}`}
            className="card mt-3 block"
            style={{ display: 'block' }}
          >
            <div style={jumpLabelStyle}>Go to juz</div>
            <div style={{ marginTop: 4, fontSize: 15, color: 'var(--text)' }}>
              Juz {js.juz} · {JUZ_NAMES_AR[js.juz - 1]} · {JUZ_NAMES[js.juz - 1]} — starts at{' '}
              {getSurahMeta(bundle!, js.s)?.name_en} {js.s}:{js.v}
            </div>
          </Link>
        ))}
      </div>
      <div className="flex flex-col gap-3 px-4 py-4">
        {results.map(({ verse: v, inEn, inUr, inTr }) => {
          const meta = bundle ? getSurahMeta(bundle, v.s) : undefined;
          return (
            <Link
              key={`${v.s}:${v.v}`}
              to={`/surah/${v.s}#v${v.v}`}
              className="card block"
              style={{ display: 'block' }}
            >
              <div
                style={{
                  fontSize: 12,
                  letterSpacing: '0.14em',
                  color: 'var(--green)',
                  textTransform: 'uppercase',
                }}
              >
                {meta?.name_en ?? `Surah ${v.s}`} {v.s}:{v.v}
              </div>
              <div
                style={{
                  fontFamily: "'DigitalKhatt IndoPak', 'Amiri Quran', serif",
                  fontSize: 'calc(20px * var(--fs-ar, 1))',
                  lineHeight: 2.05,
                  textAlign: 'right',
                  direction: 'rtl',
                  color: 'var(--vc-ar)',
                  marginTop: 6,
                }}
              >
                {v.ar}
              </div>
              {inUr && v.ur ? (
                <div className="vc-ur">
                  <Highlight text={v.ur} q={q} />
                </div>
              ) : null}
              <div className="vc-en" style={{ fontSize: 'calc(14px * var(--fs-en, 1))' }}>
                {inEn ? <Highlight text={v.en} q={q} /> : v.en}
              </div>
              {inTr && (
                <div className="vc-tr">
                  <Highlight text={v.tr} q={q} />
                </div>
              )}
            </Link>
          );
        })}
        {!bundle && (
          <p className="py-10 text-center" style={{ color: 'var(--muted)', fontSize: 13 }}>
            Loading…
          </p>
        )}
      </div>
    </div>
  );
}
