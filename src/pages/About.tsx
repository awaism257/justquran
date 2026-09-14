import { useState } from 'react';
import BackBar from '@/components/BackBar';

const GREEN = 'rgb(201, 166, 88)';

function Card({ children }: { children: React.ReactNode }) {
  return (
    <section
      style={{
        background: 'var(--card-bg)',
        boxShadow: 'var(--card-shadow)',
        borderRadius: 20,
        padding: 18,
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
        fontWeight: 400,
        marginBottom: 10,
      }}
    >
      {children}
    </h2>
  );
}

function CreditRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline gap-3 py-2" style={{ fontSize: 14, lineHeight: 1.6 }}>
      <span style={{ color: 'var(--muted)', flexShrink: 0, width: 108 }}>{label}</span>
      <span>{value}</span>
    </div>
  );
}

const BUILD_TAG = 'v83';

function AppInfo() {
  const [busy, setBusy] = useState(false);

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
    <Card>
      <CardTitle>App</CardTitle>
      <div style={{ fontSize: 14, lineHeight: 2 }}>
        <div>Build: {BUILD_TAG}</div>
      </div>
      <button
        onClick={repair}
        disabled={busy}
        style={{
          marginTop: 12,
          padding: '10px 16px',
          borderRadius: 12,
          border: '1px solid var(--green)',
          color: 'var(--green)',
          background: 'transparent',
          fontSize: 14,
        }}
      >
        {busy ? 'Repairing…' : 'Repair offline files & reload'}
      </button>
      <div style={{ fontSize: 12, color: 'var(--muted)', marginTop: 8 }}>
        Clears this device's stored app files and downloads everything fresh. Your bookmarks and
        settings are kept.
      </div>
    </Card>
  );
}

export default function About() {
  return (
    <div className="tiles" style={{ minHeight: '100dvh' }}>
      <BackBar title="About &amp; credits" />
      <div className="mx-auto flex max-w-[560px] flex-col gap-4 px-4 py-4 pb-10">
        {/* Card 1 — centred */}
        <Card>
          <div className="text-center">
            <div style={{ fontSize: 24 }}>JustQuran</div>
            <div style={{ fontSize: 13, color: 'var(--muted)', marginTop: 8 }}>
              Free · No ads · No sign-in · No tracking · Offline
            </div>
            <div style={{ fontSize: 12.5, color: GREEN, marginTop: 8 }}>justquran-app.netlify.app</div>
          </div>
        </Card>

        {/* Card 2 — Texts & licences */}
        <Card>
          <CardTitle>Texts &amp; licences</CardTitle>
          <CreditRow label="Arabic" value="Digital Khatt Indopak script — QUL / Tarteel Inc (qul.tarteel.ai)" />
          <CreditRow label="English" value="Talal Itani, ClearQuran.com — CC BY-ND 4.0" />
          <CreditRow
            label="Urdu"
            value="Jalandhari (public domain) — digitised by the JustQuran project from the original printed edition"
          />
          <CreditRow label="Transliteration" value="Tanzil Project (tanzil.net)" />
          <CreditRow label="Recitation" value="Sheikh Mahmoud Khalil Al-Hussary (Murattal)" />
          <CreditRow label="Hosting" value="Netlify — Open Source plan (netlify.com)" />
          <CreditRow label="Source code" value="Open source — MIT License (GitHub)" />
        </Card>

        {/* Card 3 — Fonts */}
        <Card>
          <CardTitle>Fonts</CardTitle>
          <p style={{ fontSize: 14, lineHeight: 1.6 }}>
            DigitalKhatt IndoPak (Amine Anane / Tarteel Inc) · Amiri Quran · Noto Nastaliq Urdu — SIL Open Font License
          </p>
        </Card>

        {/* Card 4 — Support this app (web app only; never in the Android app) */}
        <Card>
          <div className="text-center">
            <div style={{ fontSize: 17, color: GREEN, marginBottom: 10 }}>Support this app</div>
            <p style={{ fontSize: 14, lineHeight: 1.7 }}>
              JustQuran is free and contains no adverts. If it has benefited you, you can leave a
              small tip towards hosting and development costs — entirely optional.
            </p>
            <a
              href="https://ko-fi.com/awaismahmood257"
              target="_blank"
              rel="noopener noreferrer"
              style={{
                display: 'inline-block',
                marginTop: 14,
                background: GREEN,
                color: 'rgb(16, 22, 19)',
                borderRadius: 9999,
                padding: '10px 22px',
                fontSize: 14,
                textDecoration: 'none',
              }}
            >
              ♥ Leave a tip on Ko-fi
            </a>
          </div>
        </Card>

        <AppInfo />

        <p
          className="text-center"
          style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 6, lineHeight: 1.7 }}
        >
          Compiled by Awais Mahmood with the help of Kimi K3 AI.
        </p>
        <p className="text-center" style={{ fontSize: 12.5, color: GREEN, lineHeight: 1.7 }}>
          Distributed free of charge.
        </p>
      </div>
    </div>
  );
}
