// Data layer: loads /data/quran-bundle.json once, caches, typed accessors.

export interface SurahMeta {
  n: number;
  name_ar: string;
  name_en: string;
  name_meaning: string;
  ayahs: number;
}

export interface Verse {
  s: number;
  v: number;
  ar: string;
  en: string;
  tr: string;
  ur?: string; // optional — Jalandhari (public domain), digitised by the JustQuran project
  wq?: string; // waqaf mark(s) from the ayah-end cluster (QUL Indopak), shown above the verse badge
}

/**
 * Display form of a waqaf cluster. The IndoPak سجدة word-mark (U+08DB) is a
 * zero-width calligraphic ligature that renders tiny and off-center, so on
 * screen we show the standard ۩ sajdah symbol (U+06E9) instead — larger,
 * self-centering, universally recognised. Bundle data stays verbatim;
 * this is display-only.
 */
export function wqDisplay(wq: string): string {
  return wq.replace(/\u08db/g, '\u06e9');
}

/** Ink-center correction (CSS px at 17px) per waqaf cluster, measured from real
 *  renders: zero-width mark glyphs hang right of their anchor, so we shift them
 *  left to sit centered above the verse badge. */
const WQ_INK_OFFSET: Record<string, number> = {
    '\u06d9': -2.2,
    '\u0615': -2.0,
    '\u08d6': -1.2,
    '\u06da': -2.2,
    '\u06d8': -2.5,
    '\u0617': -1.5,
    '\u0614': -4.0,
    '\u06d9\u06db': -2.2,
    '\u0615\u06db': -1.8,
    '\u06d6\u06d9': -2.8,
    '\u06da\u06d6': -2.8,
    '\u06da\u06d9': -2.2,
    '\u08d7': -1.5,
    '\u08d6\u06da\u06db': -2.2,
    '\u0615\u0614': -4.0,
    '\u0617\u06d9': -2.2,
    '\u08d5': -2.5,
    '\u06d9\u06da\u06db': -2.2,
    '\u08de': -3.8,
    '\u06da\u06d6\u06db': -2.8,
    '\u0617\u06d6': -2.8,
    '\u06e9\u08d6': 0.2,
    '\u06d6\u06da': -2.8,
    '\u0615\u08d6': -1.8,
    '\u06e9': 0.2,
    '\u08de\u06d9': -3.8,
    '\u08d7\u06d9': -2.2,
    '\u0615\u08dd': -5.2,
    '\u08d7\u06d6': -2.8,
    '\u08de\u06da': -3.8,
    '\u08d5\u06d6': -2.8,
    '\u06e9\u0615': 0.2,
    '\u06e9\u06da': 0.2,
    '\u08d5\u06d9': -2.5,
    '\u06da\u06db': -2.2,
    '\u08d7\u06d6\u06db': -2.8,
    '\u06d9\u06da': -2.2,
    '\u06d6': -2.8,
    '\u08df': -5.0,
    '\u06d8\u0614': -3.8,
    '\u08d7\u0617': -1.5,
};

/** Horizontal nudge so a waqaf mark's ink (not its zero-width box) is centered. */
export function wqInkOffset(displayWq: string): number {
  return (WQ_INK_OFFSET[displayWq] ?? 0) * (25 / 17); // table measured at 17px; marks render at 25px
}


export interface Bundle {
  meta: {
    name: string;
    version: string;
    generated: string;
    verses: number;
    sources: Record<string, string>;
  };
  surahs: SurahMeta[];
  verses: Verse[];
}

let bundlePromise: Promise<Bundle> | null = null;

export function loadBundle(): Promise<Bundle> {
  if (!bundlePromise) {
    bundlePromise = fetch(`${import.meta.env.BASE_URL}data/quran-bundle.json`)
      .then((r) => {
        if (!r.ok) throw new Error(`Failed to load Quran bundle: ${r.status}`);
        return r.json() as Promise<Bundle>;
      })
      .catch((e) => {
        bundlePromise = null; // allow retry
        throw e;
      });
  }
  return bundlePromise;
}

export function getSurahs(b: Bundle): SurahMeta[] {
  return b.surahs;
}

export function getSurahMeta(b: Bundle, n: number): SurahMeta | undefined {
  return b.surahs.find((s) => s.n === n);
}

const surahVersesCache = new Map<number, Verse[]>();
export function getSurahVerses(b: Bundle, n: number): Verse[] {
  let arr = surahVersesCache.get(n);
  if (!arr) {
    arr = b.verses.filter((v) => v.s === n);
    surahVersesCache.set(n, arr);
  }
  return arr;
}

export function getVerse(b: Bundle, s: number, v: number): Verse | undefined {
  return getSurahVerses(b, s).find((x) => x.v === v);
}

// ---- Juz start table (surah:verse of each juz start) — from design.md ----
export interface JuzStart {
  juz: number;
  s: number;
  v: number;
}

export const JUZ_STARTS: JuzStart[] = [
  { juz: 1, s: 1, v: 1 }, { juz: 2, s: 2, v: 142 }, { juz: 3, s: 2, v: 253 },
  { juz: 4, s: 3, v: 93 }, { juz: 5, s: 4, v: 24 }, { juz: 6, s: 4, v: 148 },
  { juz: 7, s: 5, v: 82 }, { juz: 8, s: 6, v: 111 }, { juz: 9, s: 7, v: 88 },
  { juz: 10, s: 8, v: 41 }, { juz: 11, s: 9, v: 93 }, { juz: 12, s: 11, v: 6 },
  { juz: 13, s: 12, v: 53 }, { juz: 14, s: 15, v: 1 }, { juz: 15, s: 17, v: 1 },
  { juz: 16, s: 18, v: 75 }, { juz: 17, s: 21, v: 1 }, { juz: 18, s: 23, v: 1 },
  { juz: 19, s: 25, v: 21 }, { juz: 20, s: 27, v: 56 }, { juz: 21, s: 29, v: 46 },
  { juz: 22, s: 33, v: 31 }, { juz: 23, s: 36, v: 28 }, { juz: 24, s: 39, v: 32 },
  { juz: 25, s: 41, v: 47 }, { juz: 26, s: 46, v: 1 }, { juz: 27, s: 51, v: 31 },
  { juz: 28, s: 58, v: 1 }, { juz: 29, s: 67, v: 1 }, { juz: 30, s: 78, v: 1 },
];

export const JUZ_NAMES: string[] = [
  'Alif Lām Mīm', 'Sayaqūlu', 'Tilka ar-Rusulu', 'Lan Tānālū', 'Wal-Muḥṣanāt',
  'Lā Yuḥibbu-llāhu', 'Wa Idhā Samiʿū', 'Wa Law Annanā', 'Qāla al-Malaʾ',
  'Wa-Aʿlamū', 'Yaʿtadhirūna', 'Wa Mā min Dābbah', 'Wa Mā Ubarriʾu',
  'Rubamā', 'Subḥāna lladhī', 'Qāla a-Lam', 'Iqtaraba', 'Qad Aflaḥa',
  'Wa-Qāla lladhīna', 'Amman Khalaqa', 'Utlu Mā Ūḥiya', 'Wa-Man Yaqnut',
  'Wa-Mā Liya', 'Fa-Man Aẓlamu', 'Ilayhi Yuraddu', 'Ḥā Mīm',
  'Qāla Fa-Mā Khaṭbukum', 'Qad Samiʿa llāhu', 'Tabāraka lladhī', 'ʿAmma',
];

/** Arabic-script juz names (same order as JUZ_NAMES) — for Arabic/Urdu search. */
export const JUZ_NAMES_AR: string[] = [
  'الم', 'سيقول', 'تلك الرسل', 'لن تنالوا', 'والمحصنات',
  'لا يحب الله', 'وإذا سمعوا', 'ولو أننا', 'قال الملأ',
  'واعلموا', 'يعتذرون', 'وما من دابة', 'وما أبرئ',
  'ربما', 'سبحان الذي', 'قال ألم', 'اقترب', 'قد أفلح',
  'وقال الذين', 'أمن خلق', 'اتل ما أوحي', 'ومن يقنت',
  'وما لي', 'فمن أظلم', 'إليه يرد', 'حم',
  'قال فما خطبكم', 'قد سمع الله', 'تبارك الذي', 'عم',
];

/** Juz number containing a given surah:verse (1..30). */
export function juzOf(s: number, v: number): number {
  let j = 1;
  for (const js of JUZ_STARTS) {
    if (js.s < s || (js.s === s && js.v <= v)) j = js.juz;
    else break;
  }
  return j;
}

const JUZ_START_MAP = new Map<string, number>(JUZ_STARTS.map((j) => [`${j.s}:${j.v}`, j.juz]));

/** Juz number when (s,v) is the FIRST verse of a juz, else null — drives the
    "juz begins here" marker on the opening ayah (mushaf-style ۞). */
export function juzStartAt(s: number, v: number): number | null {
  return JUZ_START_MAP.get(`${s}:${v}`) ?? null;
}

/** Strip Arabic diacritics/tatweel for offline search matching. */
export function stripArabicDiacritics(t: string): string {
  return t
    .replace(/[\u064B-\u065F\u0670\u06D6-\u06ED\u0640\u034F\u0614-\u061A\u089C\u08D5-\u08E2]/g, '')
    .replace(/[أإآٱ]/g, 'ا')
    .replace(/ة/g, 'ه')
    .replace(/ى/g, 'ي');
}
