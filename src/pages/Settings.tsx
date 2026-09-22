import { useState } from 'react';
import { ChevronRight } from 'lucide-react';
import { Link } from 'react-router';
import BackBar from '@/components/BackBar';
import { useSettings } from '@/lib/settings';
import type { FontScales, ThemeChoice } from '@/lib/settings';
import { isAndroidApp } from '@/lib/androidApp';
import { BUILD_TAG } from '@/lib/build';

const GREEN = 'rgb(201, 166, 88)';

/* ---------- controls ---------- */

function SliderRow({
  label,
  value,
  onChange,
  min = 70,
  max = 160,
}: {
  label: string;
  value: number;
  onChange: (pct: number) => void;
  min?: number;
  max?: number;
}) {
  const pct = ((value - min) / (max - min)) * 100;
  return (
    <div className="py-3">
      <div className="flex items-baseline justify-between">
        <span style={{ fontSize: 14 }}>{label}</span>
        <span style={{ fontSize: 12, color: 'var(--muted)' }}>{value}%</span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={1}
        value={value}
        aria-label={label}
        onChange={(e) => onChange(Number(e.target.value))}
        className="jq-slider mt-2 w-full appearance-none"
        style={{
          height: 4,
          borderRadius: 9999,
          outline: 'none',
          cursor: 'pointer',
          background: `linear-gradient(to right, ${GREEN} ${pct}%, var(--line) ${pct}%)`,
          accentColor: GREEN,
        }}
      />
      <style>{`
        input[type='range'].jq-slider::-webkit-slider-thumb {
          -webkit-appearance: none;
          appearance: none;
          width: 20px;
          height: 20px;
          border-radius: 9999px;
          background: ${GREEN};
          border: none;
          cursor: pointer;
        }
        input[type='range'].jq-slider::-moz-range-thumb {
          width: 20px;
          height: 20px;
          border-radius: 9999px;
          background: ${GREEN};
          border: none;
          cursor: pointer;
        }
      `}</style>
    </div>
  );
}

function Toggle({
  on,
  onClick,
  label,
  disabled = false,
}: {
  on: boolean;
  onClick: () => void;
  label: string;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={on}
      aria-label={label}
      aria-disabled={disabled}
      onClick={disabled ? undefined : onClick}
      className="relative shrink-0 cursor-pointer"
      style={{
        width: 46,
        height: 26,
        borderRadius: 9999,
        border: 'none',
        background: on ? GREEN : 'var(--line)',
        transition: 'background 0.15s ease',
        padding: 0,
        opacity: disabled ? 0.4 : 1,
        cursor: disabled ? 'default' : 'pointer',
      }}
    >
      <span
        style={{
          position: 'absolute',
          top: 3,
          left: on ? 23 : 3,
          width: 20,
          height: 20,
          borderRadius: 9999,
          background: on ? 'rgb(16,22,19)' : 'var(--muted)',
          transition: 'left 0.15s ease',
        }}
      />
    </button>
  );
}

function ToggleRow({
  label,
  on,
  onToggle,
  disabled = false,
}: {
  label: string;
  on: boolean;
  onToggle: () => void;
  disabled?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-3" style={disabled ? { opacity: 0.6 } : undefined}>
      <span style={{ fontSize: 14 }}>{label}</span>
      <Toggle on={on} onClick={onToggle} label={label} disabled={disabled} />
    </div>
  );
}

function RadioRow({
  label,
  selected,
  onSelect,
}: {
  label: string;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      onClick={onSelect}
      className="flex w-full cursor-pointer items-center justify-between py-3"
      style={{ background: 'transparent', border: 'none', color: 'inherit', font: 'inherit', padding: 0, paddingTop: 12, paddingBottom: 12 }}
    >
      <span style={{ fontSize: 14 }}>{label}</span>
      <span
        className="flex items-center justify-center"
        style={{
          width: 22,
          height: 22,
          borderRadius: 9999,
          border: `2px solid ${GREEN}`,
          flexShrink: 0,
        }}
      >
        {selected ? (
          <span style={{ width: 12, height: 12, borderRadius: 9999, background: GREEN }} />
        ) : null}
      </span>
    </button>
  );
}

/* ---------- layout pieces ---------- */

function SectionCard({ children }: { children: React.ReactNode }) {
  return (
    <section
      style={{
        background: 'var(--card-bg)',
        boxShadow: 'var(--card-shadow)',
        borderRadius: 20,
        padding: '6px 18px',
      }}
    >
      {children}
    </section>
  );
}

function CardTitle({ children }: { children: React.ReactNode }) {
  return (
    <h2
      style={{
        fontSize: 11,
        letterSpacing: '0.18em',
        textTransform: 'uppercase',
        color: GREEN,
        paddingTop: 12,
        paddingBottom: 4,
        fontWeight: 400,
      }}
    >
      {children}
    </h2>
  );
}

function RowDivider() {
  return <div style={{ height: 1, background: 'var(--line)' }} />;
}

/* ---------- page ---------- */

const FONT_ROWS: { key: keyof FontScales; label: string }[] = [
  { key: 'ar', label: 'Arabic font size' },
  { key: 'en', label: 'English font size' },
  { key: 'ur', label: 'Urdu font size' },
  { key: 'tr', label: 'Transliteration font size' },
];

const THEME_ROWS: { value: ThemeChoice; label: string }[] = [
  { value: 'system', label: 'System default' },
  { value: 'light', label: 'Light' },
  { value: 'dark', label: 'Dark' },
];

export default function Settings() {
  const { settings, setTheme, setFontScale, toggle, set } = useSettings();

  // v112: Android parity — "Reading layout" pills drive the same four keys the
  // reader's top-bar icon toggles (mushaf = translations off + continuous folio;
  // cards = translations restored + verse-by-verse).
  const arabicOnly = !settings.showEn && !settings.showUr && !settings.showTr;
  const mushafOn = !settings.verseByVerse && arabicOnly;
  const selectMushaf = () => {
    if (!mushafOn) set({ showEn: false, showUr: false, showTr: false, verseByVerse: false });
  };
  const selectCards = () => {
    if (arabicOnly) set({ showEn: true, showUr: true, showTr: true, verseByVerse: true });
    else if (!settings.verseByVerse) set({ verseByVerse: true });
  };
  // The last visible text can't be turned off (a card with nothing on it):
  // Arabic can only be hidden while a translation shows.
  const anyTranslationOn = settings.showEn || settings.showUr || settings.showTr;

  return (
    <div>
      <BackBar title="Settings" />
      <div className="mx-auto flex max-w-[560px] flex-col gap-4 px-4 py-4 pb-10">
        {/* Display */}
        <SectionCard>
          <CardTitle>Display</CardTitle>
          {FONT_ROWS.map((row, i) => (
            <div key={row.key}>
              {i > 0 && <RowDivider />}
              <div className="jq-slider-wrap">
                <SliderRow
                  label={row.label}
                  value={settings.fonts[row.key]}
                  onChange={(pct) => setFontScale(row.key, pct)}
                />
              </div>
            </div>
          ))}
          <RowDivider />
          <ToggleRow label="Show English" on={settings.showEn} onToggle={() => toggle('showEn')} />
          <RowDivider />
          <ToggleRow label="Show Urdu" on={settings.showUr} onToggle={() => toggle('showUr')} />
          <RowDivider />
          <ToggleRow
            label="Show transliteration"
            on={settings.showTr}
            onToggle={() => toggle('showTr')}
          />
          <RowDivider />
          <div className="py-3">
            <span style={{ fontSize: 14 }}>Reading layout</span>
            <div className="chips" style={{ marginTop: 10 }}>
              <button type="button" className={mushafOn ? 'chip' : 'chip on'} onClick={selectCards}>
                Cards
              </button>
              <button type="button" className={mushafOn ? 'chip on' : 'chip'} onClick={selectMushaf}>
                Mushaf
              </button>
            </div>
          </div>
          <RowDivider />
          <ToggleRow
            label="Arabic text in card view"
            on={settings.showAr}
            disabled={settings.showAr && !anyTranslationOn}
            onToggle={() => toggle('showAr')}
          />
        </SectionCard>

        {/* Recitation */}
        <SectionCard>
          <CardTitle>Recitation</CardTitle>
          <ToggleRow
            label="Continue to next verse automatically"
            on={settings.audioAutoAdvance}
            onToggle={() => toggle('audioAutoAdvance')}
          />
          <RowDivider />
          <Link
            to="/recitation"
            className="flex items-center justify-between py-3"
            style={{ color: 'inherit', textDecoration: 'none' }}
          >
            <span style={{ fontSize: 14 }}>Download audio for offline</span>
            <ChevronRight size={18} className="chev" style={{ color: 'var(--muted)' }} />
          </Link>
        </SectionCard>

        {/* Theme */}
        <SectionCard>
          <CardTitle>Theme</CardTitle>
          <div role="radiogroup" aria-label="Theme">
            {THEME_ROWS.map((row, i) => (
              <div key={row.value}>
                {i > 0 && <RowDivider />}
                <RadioRow
                  label={row.label}
                  selected={settings.theme === row.value}
                  onSelect={() => setTheme(row.value)}
                />
              </div>
            ))}
          </div>
        </SectionCard>

        {/* How to use */}
        <Link to="/help" className="card">
          <span style={{ fontSize: 14 }}>How to use</span>
          <ChevronRight size={18} className="chev" />
        </Link>

        {/* Install app — opens the install sheet (native prompt on Android, steps on iPhone).
            Hidden inside the native Android app (already installed). */}
        {!isAndroidApp && !(window.matchMedia?.('(display-mode: standalone)').matches ||
           (navigator as unknown as { standalone?: boolean }).standalone === true) && (
          <button
            type="button"
            className="card"
            style={{ width: '100%', border: 'none', font: 'inherit', textAlign: 'left' }}
            onClick={() => window.dispatchEvent(new Event('jq:show-install'))}
          >
            <span style={{ fontSize: 14 }}>Install app</span>
            <span style={{ marginLeft: 'auto', fontSize: 12, color: 'var(--muted)' }}>offline · home screen</span>
            <ChevronRight size={18} className="chev" style={{ marginLeft: 0 }} />
          </button>
        )}

        {/* About & credits */}
        <Link to="/about" className="card">
          <span style={{ fontSize: 14 }}>About &amp; credits</span>
          <ChevronRight size={18} className="chev" />
        </Link>

        {/* Diagnostics — repair moved here from About (keeps About read-only). */}
        <DiagnosticsCard />
      </div>
    </div>
  );
}

/* ---------- diagnostics ---------- */

function DiagnosticsCard() {
  const [busy, setBusy] = useState(false);

  // Unregister the service worker and wipe every Cache Storage bucket, then
  // reload — the fresh sw fetches the latest app files. localStorage (bookmarks,
  // settings) is untouched.
  const repair = async () => {
    setBusy(true);
    try {
      const regs = await navigator.serviceWorker.getRegistrations();
      await Promise.all(regs.map((r) => r.unregister()));
      const keys = await caches.keys();
      await Promise.all(keys.map((k) => caches.delete(k)));
    } catch {
      /* best effort */
    }
    location.reload();
  };

  return (
    <SectionCard>
      <CardTitle>Diagnostics</CardTitle>
      <div style={{ fontSize: 13, color: 'var(--muted)', paddingBottom: 10 }}>Build: {BUILD_TAG}</div>
      <button
        onClick={repair}
        disabled={busy}
        style={{
          padding: '10px 16px',
          borderRadius: 12,
          border: '1px solid var(--green)',
          color: 'var(--green)',
          background: 'transparent',
          fontSize: 14,
          marginBottom: 8,
        }}
      >
        {busy ? 'Repairing…' : 'Repair offline files & reload'}
      </button>
      <div style={{ fontSize: 12, color: 'var(--muted)', paddingBottom: 12 }}>
        Clears this device's stored app files and downloads everything fresh. Your bookmarks and
        settings are kept.
      </div>
    </SectionCard>
  );
}
