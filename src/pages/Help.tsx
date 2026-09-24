import { useEffect, type ReactNode } from 'react';
import { useLocation } from 'react-router';
import { Bookmark, ChevronDown, Copy, Play, Rows3, BookOpenText, Search, SkipBack, SkipForward, Volume2 } from 'lucide-react';
import BackBar from '@/components/BackBar';
import { PAUSES_NOTE } from '@/lib/khatmDua';

const GREEN = 'rgb(201, 166, 88)';

function CollapsibleCard({
  id,
  title,
  children,
  defaultOpen,
}: {
  id?: string;
  title: ReactNode;
  children: ReactNode;
  defaultOpen?: boolean;
}) {
  return (
    <details
      id={id}
      className="pauses-details"
      open={defaultOpen}
      style={{
        background: 'var(--card-bg)',
        boxShadow: 'var(--card-shadow)',
        borderRadius: 20,
        padding: 18,
      }}
    >
      <summary
        style={{
          cursor: 'pointer',
          listStyle: 'none',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <CardTitle>{title}</CardTitle>
        <span
          className="pauses-chevron"
          style={{ display: 'inline-flex', color: GREEN, transition: 'transform 0.25s ease' }}
        >
          <ChevronDown size={18} />
        </span>
      </summary>
      {children}
    </details>
  );
}

function CardTitle({ children }: { children: ReactNode }) {
  return (
    <h2
      style={{
        fontSize: 11,
        letterSpacing: '0.18em',
        textTransform: 'uppercase',
        color: GREEN,
        fontWeight: 400,
        marginBottom: 12,
      }}
    >
      {children}
    </h2>
  );
}

function Step({ n, children }: { n: number; children: ReactNode }) {
  return (
    <div className="flex items-start gap-3 py-1.5" style={{ fontSize: 14, lineHeight: 1.65 }}>
      <span className="vnum" style={{ width: 24, height: 24, fontSize: 12, marginTop: 2 }}>
        {n}
      </span>
      <span>{children}</span>
    </div>
  );
}

/** Inline circular icon, drawn with the same icons and style as the real buttons. */
function Ico({ children, label }: { children: ReactNode; label: string }) {
  return (
    <span
      role="img"
      aria-label={label}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: 24,
        height: 24,
        borderRadius: 9999,
        border: '1px solid var(--line)',
        color: 'var(--text)',
        verticalAlign: 'middle',
        margin: '0 3px',
        flexShrink: 0,
      }}
    >
      {children}
    </span>
  );
}

/** Inline chip, same look as the real toggle chips. */
function Chip({ children }: { children: ReactNode }) {
  return (
    <span
      aria-hidden="true"
      style={{
        display: 'inline-block',
        border: '1px solid var(--line)',
        borderRadius: 9999,
        padding: '1px 10px',
        fontSize: 12,
        color: 'var(--muted)',
        whiteSpace: 'nowrap',
        verticalAlign: 'middle',
        margin: '0 2px',
      }}
    >
      {children}
    </span>
  );
}

/** Superscript green verse number, same look as the .bkmark markers in Translations books. */
function SuperNum({ children }: { children: ReactNode }) {
  return (
    <span
      aria-hidden="true"
      style={{
        padding: '0 3px',
        color: 'var(--green)',
        fontFamily: "'DejaVu Serif', Georgia, serif",
        fontSize: '0.65em',
        verticalAlign: 'super',
        lineHeight: 1,
      }}
    >
      {children}
    </span>
  );
}

/** Inline gold verse badge, same look as the verse-card / mushaf markers. */
function Pill({ children }: { children: ReactNode }) {
  return (
    <span
      aria-hidden="true"
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        minWidth: 19,
        height: 19,
        border: '1px solid var(--frame)',
        borderRadius: 9999,
        color: 'var(--frame)',
        fontSize: 10.5,
        padding: '0 2px',
        verticalAlign: 'middle',
        margin: '0 3px',
      }}
    >
      {children}
    </span>
  );
}

/** One pause-sign row: large sign in the Quran font, then EN name + meaning, then UR meaning. */
function PauseRow({ sign, name_en, en, ur }: { sign: string; name_en: string; en: string; ur: string }) {
  return (
    <div
      className="flex items-center gap-4 py-2"
      style={{ borderBottom: '1px solid var(--line)', fontSize: 14 }}
    >
      <span
        style={{
          fontFamily: "'DigitalKhatt IndoPak', 'Amiri Quran', serif",
          fontSize: 30,
          lineHeight: 1.4,
          minWidth: 56,
          textAlign: 'center',
          color: 'var(--vc-ar)',
          direction: 'rtl',
        }}
      >
        {sign}
      </span>
      <span>
        <span className="block">
          <b>{name_en}</b> — {en}
        </span>
        <span
          className="block"
          style={{
            fontFamily: "'Noto Nastaliq Urdu', serif",
            direction: 'rtl',
            textAlign: 'left',
            fontSize: 13,
            lineHeight: 2.1,
            color: 'var(--vc-ur)',
          }}
        >
          {ur}
        </span>
      </span>
    </div>
  );
}

export default function Help() {
  const { hash } = useLocation();

  // React Router doesn't scroll to a hash by itself — honor e.g. /help#pauses.
  // Also auto-open the matching <details> card so its content is visible.
  useEffect(() => {
    if (hash) {
      const el = document.getElementById(hash.slice(1));
      if (el) {
        if (el.tagName.toLowerCase() === 'details') {
          (el as HTMLDetailsElement).open = true;
        }
        el.scrollIntoView({ behavior: 'smooth' });
      }
    }
  }, [hash]);

  return (
    <div className="tiles" style={{ minHeight: '100dvh' }}>
      <BackBar title="How to use" />
      <div className="mx-auto flex max-w-[560px] flex-col gap-4 px-4 py-4 pb-10">
        <CollapsibleCard defaultOpen title="Two ways to read">
          <Step n={1}>
            <b>Verse-by-verse</b> (the default): every verse in its own card with translation and
            transliteration.
          </Step>
          <Step n={2}>
            <b>Arabic only</b> (mushaf style): tap the book icon
            <Ico label="Arabic only"><BookOpenText size={13} /></Ico>
            in the top bar of any surah for continuous Arabic text. <b>Settings → Mushaf page
            style</b> can turn the mushaf into pages that are laid out at your chosen font size —
            the number of pages adapts so verses are never cut off.
          </Step>
          <Step n={3}>
            The icon changes to
            <Ico label="Verse by verse"><Rows3 size={13} /></Ico>
            and turns green — tap it again to switch back. The app remembers your choice. In the
            mushaf view there are no chips below the bar: a
            <Ico label="Play surah"><Play size={13} /></Ico>
            button next to the icon plays (or stops) the whole surah.
          </Step>
          <Step n={4}>
            Reading without the Arabic? <b>Settings → Arabic text in card view</b> hides it, so you
            can read English only, Urdu only, or transliteration only. (Mushaf view is always
            Arabic.)
          </Step>
        </CollapsibleCard>

        <CollapsibleCard title="Card View">
          <Step n={1}>
            In the card view, the top of any surah has chips: <Chip>Arabic</Chip> <Chip>English</Chip>
            <Chip>اردو</Chip> <Chip>Translit</Chip>
          </Step>
          <Step n={2}>
            Tap a chip to show or hide that line while reading — <Chip>Arabic</Chip> shows or hides
            the Arabic line on the verse cards. The last visible line can't be switched off.
          </Step>
          <Step n={3}>
            The mushaf (Arabic-only) view has no chips — use the
            <Ico label="Play surah"><Play size={13} /></Ico>
            play button and the layout icon in the top bar instead.
          </Step>
          <Step n={4}>The Urdu translation is the classic Jalandhari translation (public domain).</Step>
        </CollapsibleCard>

        <CollapsibleCard title="Text size &amp; theme">
          <Step n={1}>
            Settings has sliders for Arabic, English, Urdu and transliteration text size.
          </Step>
          <Step n={2}>Theme can be light, dark, or follow your phone.</Step>
          <Step n={3}>Everything works fully offline; there are no ads, accounts or tracking.</Step>
        </CollapsibleCard>

        <CollapsibleCard title="Audio">
          <Step n={1}>
            <Chip>▶ Play surah</Chip> at the top of a surah plays it from the beginning, starting
            with Bismillah (except surah 9 — in Al-Fatiha the Bismillah is verse 1 itself).
          </Step>
          <Step n={2}>
            The <Ico label="Listen"><Volume2 size={13} /></Ico> button on a verse card plays just
            that verse.
          </Step>
          <Step n={3}>
            To listen without internet, first download the surah: Settings → Recitation.
          </Step>
          <Step n={4}>
            To keep listening after a verse ends, open the player bar (tap any verse's{' '}
            <Ico label="Listen"><Volume2 size={13} /></Ico> button) and tap the <b>Auto</b> toggle —
            playback will continue verse by verse. The same option is in Settings → Recitation
            ("Continue to next verse automatically").
          </Step>
        </CollapsibleCard>

        <CollapsibleCard title="Search">
          <Step n={1}>
            Tap the magnifier <Ico label="Search"><Search size={13} /></Ico> at the top of the home
            page.
          </Step>
          <Step n={2}>Type in English, Arabic or Urdu letters — all three work.</Step>
          <Step n={3}>
            Find a surah by name (e.g. “Tawbah” or التوبہ) or a juz by name or number (“juz 30”,
            “para 30”, پارہ ۳۰).
          </Step>
          <Step n={4}>Jump straight to a verse by typing its number, e.g. 2:255.</Step>
        </CollapsibleCard>

        <CollapsibleCard title="Bookmarks">
          <Step n={1}>
            Tap <Ico label="Bookmark"><Bookmark size={13} /></Ico> on any verse card or verse
            details card.
          </Step>
          <Step n={2}>
            Open <b>Bookmarks</b> from the home page to see saved verses; tap one to go back to it.
          </Step>
          <Step n={3}>
            <b>Continue reading</b> on the home page returns you to where you stopped.
          </Step>
        </CollapsibleCard>

        <CollapsibleCard title="Verse details card">
          <Step n={1}>
            While reading, tap the small gold number badge <Pill>15</Pill> after a verse — or press
            and hold on the verse itself.
          </Step>
          <Step n={2}>A card opens showing the translation and transliteration.</Step>
          <Step n={3}>
            Use
            <Ico label="Previous verse"><SkipBack size={13} /></Ico>
            <Ico label="Next verse"><SkipForward size={13} /></Ico>
            to move to the previous or next verse.
          </Step>
          <Step n={4}>
            Buttons:
            <Ico label="Bookmark"><Bookmark size={13} /></Ico> bookmark ·
            <Ico label="Copy or share"><Copy size={13} /></Ico> copy or share ·
            <Ico label="Listen"><Play size={13} /></Ico> listen to this verse.
          </Step>
        </CollapsibleCard>

        <CollapsibleCard title="Translations reader">
          <Step n={1}>
            <b>Home → Translations</b>, then pick English or اردو.
          </Step>
          <Step n={2}>
            Translations flow like a book — swipe, tap the page edges, or use the arrows to turn a
            page.
          </Step>
          <Step n={3}>
            Tap a verse-number marker <SuperNum>15</SuperNum> for its Arabic, audio, bookmark and share.
          </Step>
        </CollapsibleCard>

        <CollapsibleCard id="pauses" title={PAUSES_NOTE.heading_en}>
          <p style={{ fontSize: 14, lineHeight: 1.65 }}>{PAUSES_NOTE.en}</p>
          <p
            style={{
              fontFamily: "'Noto Nastaliq Urdu', serif",
              direction: 'rtl',
              fontSize: 13,
              lineHeight: 2.3,
              marginTop: 8,
              color: 'var(--vc-ur)',
            }}
          >
            {PAUSES_NOTE.ur}
          </p>
          <div style={{ marginTop: 8 }}>
            {PAUSES_NOTE.signs.map((s) => (
              <PauseRow key={s.sign} sign={s.sign} name_en={s.name_en} en={s.en} ur={s.ur} />
            ))}
          </div>
        </CollapsibleCard>

        <CollapsibleCard title="Install the app">
          <Step n={1}>
            <b>Android:</b> tap the <b>⋮ menu</b> in Chrome → <b>Add to Home screen</b> → <b>Install</b>.
            (You may also see an install sheet with a one-tap button.)
          </Step>
          <Step n={2}>
            <b>iPhone / iPad:</b> in Safari tap <b>Share</b> (square with an arrow up) → scroll down →
            <b> Add to Home Screen</b> → <b>Add</b>.
          </Step>
          <Step n={3}>
            Once installed it opens like a real app and works fully offline. You can reopen the
            install sheet any time from Settings → <b>Install app</b>.
          </Step>
        </CollapsibleCard>

        <CollapsibleCard title="Troubleshooting">
          <Step n={1}>
            If the app ever looks broken, shows an old version, or gets stuck: open{' '}
            <b>Settings → Diagnostics</b> and tap the <b>Repair offline files &amp; reload</b>{' '}
            button. The app will reload itself fresh; your bookmarks and settings are kept.
          </Step>
          <Step n={2}>
            If that does not fix it, remove the app from your home screen and add it again from
            the browser — you will get the latest version.
          </Step>
          <Step n={3}>
            Still stuck? Note the build number shown in Settings → Diagnostics (or on the About
            screen) and let the developer know what you saw.
          </Step>
        </CollapsibleCard>
      </div>
    </div>
  );
}
