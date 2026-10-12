import BackBar from '@/components/BackBar';
import { isAndroidApp } from '@/lib/androidApp';
import { BUILD_TAG } from '@/lib/build';

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

const BOOKS: { cover: string; alt: string; title: string; desc: string; asin: string; sample?: string }[] = [
  {
    cover: 'books/book-odes.jpg',
    alt: 'Odes and Litanies',
    title: 'Odes and Litanies in Praise of the Prophet ﷺ',
    desc: 'The Burdah, Bānat Suʿād and Dalāʾil al-Khayrāt in Arabic and English, in one volume.',
    asin: 'B0HMLDCM2N',
  },
  {
    cover: 'books/book-garden-vol1.jpg',
    alt: 'The Garden of Purity, Volume 1',
    title: 'The Garden of Purity, Volume 1: The Dawn of Prophethood',
    desc: "Mīrkhwānd's life of the Prophet ﷺ in modern English, from his ancestry to Badr and Uḥud, with editorial footnotes.",
    asin: 'B0HMLBZP9S',
  },
  {
    cover: 'books/book-garden-vol2.jpg',
    alt: 'The Garden of Purity, Volume 2',
    title: 'The Garden of Purity, Volume 2: The Madinan Years',
    desc: 'From the Trench and Ḥudaybiyyah to the Farewell Pilgrimage and the passing of the Prophet ﷺ.',
    asin: 'B0HMP9ZH3Q',
  },
  {
    cover: 'books/book-seerah-colouring.jpg',
    alt: 'My First Seerah Colouring Book',
    title: 'My First Seerah Colouring Book',
    desc: 'For children aged 4–9: 25 scenes from Makkah to Madinah, each with a short story. No people are drawn.',
    asin: 'B0HMP8ZHHD',
    sample: 'https://mantlelibrary.app/downloads/Garden_of_Purity_Free_Colouring_Sample.pdf',
  },
];

// Renders ﷺ in Noto Naskh so it isn't drawn oversized by a fallback font.
function WithHonorific({ text }: { text: string }) {
  const parts = text.split('ﷺ');
  return (
    <>
      {parts.map((part, i) => (
        <span key={i}>
          {part}
          {i < parts.length - 1 && <span style={{ fontFamily: 'NotoNaskh, serif' }}>ﷺ</span>}
        </span>
      ))}
    </>
  );
}

function BookLink({ href, label, filled }: { href: string; label: string; filled?: boolean }) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      style={{
        display: 'inline-block',
        borderRadius: 9999,
        padding: '6px 14px',
        fontSize: 13,
        textDecoration: 'none',
        border: `1.5px solid ${GREEN}`,
        background: filled ? GREEN : 'transparent',
        color: filled ? 'rgb(16, 22, 19)' : GREEN,
      }}
    >
      {label}
    </a>
  );
}

function BooksCard() {
  return (
    <Card>
      <CardTitle>Books from Munajaat Press</CardTitle>
      <p style={{ fontSize: 13.5, lineHeight: 1.6, color: 'var(--muted)' }}>
        Printed and Kindle editions made by the same team. They are paid books sold on Amazon; the
        app itself stays free, with no ads.
      </p>
      <div className="flex flex-col gap-4" style={{ marginTop: 14 }}>
        {BOOKS.map((b) => (
          <div key={b.asin} className="flex items-start gap-3">
            <img
              src={`${import.meta.env.BASE_URL}${b.cover}`}
              alt={`Cover of ${b.alt}`}
              width={64}
              loading="lazy"
              style={{ flexShrink: 0, height: 'auto', borderRadius: 4, boxShadow: 'var(--card-shadow)' }}
            />
            <div style={{ minWidth: 0 }}>
              <div style={{ fontSize: 15, lineHeight: 1.4 }}>
                <WithHonorific text={b.title} />
              </div>
              <p style={{ fontSize: 13.5, lineHeight: 1.55, color: 'var(--muted)', margin: '4px 0 8px' }}>
                <WithHonorific text={b.desc} />
              </p>
              <div className="flex flex-wrap gap-2">
                <BookLink href={`https://www.amazon.co.uk/dp/${b.asin}`} label="Amazon UK" filled />
                <BookLink href={`https://www.amazon.com/dp/${b.asin}`} label="Amazon US" />
              </div>
              {b.sample && (
                <a
                  href={b.sample}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{ display: 'inline-block', marginTop: 8, fontSize: 13, color: GREEN, textDecoration: 'underline' }}
                >
                  Free sample pages to print (PDF, 4 MB)
                </a>
              )}
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
}

function AppInfo() {
  // The "Repair offline files & reload" button lives in Settings → Diagnostics.
  return (
    <Card>
      <CardTitle>App</CardTitle>
      <div style={{ fontSize: 14, lineHeight: 2 }}>
        <div>Build: {BUILD_TAG}</div>
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
            <div style={{ fontSize: 12.5, color: GREEN, marginTop: 8 }}>justquran.app</div>
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
          <CreditRow
            label="Narration"
            value="English & Urdu chapters read by AI text-to-speech — ClearQuran narration with the author's kind permission"
          />
          <CreditRow label="Hosting" value="Cloudflare / Netlify" />
          <CreditRow label="Source code" value="Open source — MIT License (GitHub)" />
        </Card>

        {/* Card 3 — Fonts */}
        <Card>
          <CardTitle>Fonts</CardTitle>
          <p style={{ fontSize: 14, lineHeight: 1.6 }}>
            DigitalKhatt IndoPak (Amine Anane / Tarteel Inc) · Amiri Quran · Noto Nastaliq Urdu — SIL Open Font License
          </p>
        </Card>

        {/* Books from Munajaat Press (web app only; Google Play could treat it as advertising) */}
        {!isAndroidApp && <BooksCard />}

        {/* Card 4 — Support this app (web app only; never in the Android app —
            Google Play policy forbids external donation links in the app) */}
        {!isAndroidApp && (
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
        )}

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
