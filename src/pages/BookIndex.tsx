import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { BookOpen, ChevronRight } from 'lucide-react';
import BackBar from '@/components/BackBar';
import { bookTitle, loadBundle, loadSurahTitles } from '@/lib/data';
import type { Bundle, SurahMeta, SurahTitles } from '@/lib/data';
import type { BookLang } from '@/components/NarrationDownloads';

const LS_KEY = 'jq-book-lang';

/** Last picked book language sticks (localStorage) until the user changes it. */
function loadBookLang(): BookLang {
  try {
    return localStorage.getItem(LS_KEY) === 'ur' ? 'ur' : 'en';
  } catch {
    return 'en';
  }
}

/** Surah list row — same markup/classes as SurahIndex, linking into the book.
 *  The main title is the translation source's own chapter heading:
 *  Talal Itani's titles for English, Jalandhari's print headings for Urdu. */
function BookSurahRow({
  s,
  lang,
  titles,
}: {
  s: SurahMeta;
  lang: BookLang;
  titles: SurahTitles | null;
}) {
  const title = bookTitle(titles, lang, s);
  return (
    <Link to={`/book/${lang}/${s.n}`} className="card">
      <span className="vnum">{s.n}</span>
      <span className="min-w-0">
        <span
          className="block"
          style={
            lang === 'ur'
              ? { fontFamily: "'Noto Nastaliq Urdu', serif", fontSize: 19, direction: 'rtl', textAlign: 'left' }
              : { fontSize: 16 }
          }
        >
          {title}
        </span>
        <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
          {lang === 'ur' ? `${s.name_en} · ${s.ayahs} verses` : `${s.name_en} · ${s.ayahs} verses`}
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

/** Book mode (v114): read a translation as flowing pages, like a book. */
export default function BookIndex() {
  const [bundle, setBundle] = useState<Bundle | null>(null);
  const [titles, setTitles] = useState<SurahTitles | null>(null);
  // v117: persist the pick — it must survive leaving/returning to this list
  // (previously useState('en') snapped back to English after reading a surah).
  const [lang, setLangState] = useState<BookLang>(loadBookLang);
  const setLang = (l: BookLang) => {
    setLangState(l);
    try {
      localStorage.setItem(LS_KEY, l);
    } catch {
      /* private mode */
    }
  };

  useEffect(() => {
    loadBundle().then(setBundle).catch(() => {});
    loadSurahTitles().then(setTitles).catch(() => {});
  }, []);

  return (
    <div className="tiles" style={{ minHeight: '100dvh' }}>
      <BackBar title="Translations" meta={lang === 'en' ? 'English translation' : 'Urdu translation'} />
      <div className="chips" style={{ paddingTop: 12 }}>
        <button className={lang === 'en' ? 'chip on' : 'chip'} onClick={() => setLang('en')}>
          English
        </button>
        <button className={lang === 'ur' ? 'chip on' : 'chip'} onClick={() => setLang('ur')}>
          اردو
        </button>
      </div>
      <div className="flex flex-col gap-3 px-4 py-4">
        <button
          type="button"
          className="card"
          onClick={() => setLang('en')}
          style={{
            width: '100%',
            font: 'inherit',
            textAlign: 'left',
            border: lang === 'en' ? '1px solid var(--green)' : '1px solid transparent',
          }}
        >
          <span style={{ color: 'var(--green)', flexShrink: 0 }}>
            <BookOpen size={20} />
          </span>
          <span>
            <span className="block" style={{ fontSize: 17 }}>English</span>
            <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
              ClearQuran translation
            </span>
          </span>
        </button>
        <button
          type="button"
          className="card"
          onClick={() => setLang('ur')}
          style={{
            width: '100%',
            font: 'inherit',
            textAlign: 'left',
            border: lang === 'ur' ? '1px solid var(--green)' : '1px solid transparent',
          }}
        >
          <span style={{ color: 'var(--green)', flexShrink: 0 }}>
            <BookOpen size={20} />
          </span>
          <span>
            <span className="block" style={{ fontSize: 17 }}>اردو</span>
            <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
              جلندھری ترجمہ
            </span>
          </span>
        </button>

        {bundle &&
          bundle.surahs.map((s) => <BookSurahRow key={s.n} s={s} lang={lang} titles={titles} />)}
        {!bundle && (
          <p className="py-10 text-center" style={{ color: 'var(--muted)', fontSize: 13 }}>
            Loading…
          </p>
        )}
      </div>
    </div>
  );
}
