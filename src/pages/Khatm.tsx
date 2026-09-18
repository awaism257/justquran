import BackBar from '@/components/BackBar';
import { CLOSING_HADITH, KHATM_INTRO, KHATM_INTRO_ATTRIBUTION, SHORT_DUA } from '@/lib/khatmDua';

/** Honorific ligatures (﵀ rahimahu Allah, ﵁ radi Allahu anh, ﷺ salla Allahu
 *  alayhi wa sallam) in the English texts are wrapped in .honorific spans so
 *  they render with the bundled Noto Naskh font. */
const HONORIFIC_SPLIT_RE = /([\uFD40\uFD41\uFDFA])/g;
const HONORIFIC_TEST_RE = /^[\uFD40\uFD41\uFDFA]$/;

function withHonorifics(text: string): React.ReactNode {
  const parts = text.split(HONORIFIC_SPLIT_RE);
  if (parts.length === 1) return text;
  return parts.map((part, i) =>
    HONORIFIC_TEST_RE.test(part) ? (
      <span key={i} className="honorific">
        {part}
      </span>
    ) : (
      part
    ),
  );
}

function TextCard({ children }: { children: React.ReactNode }) {
  return (
    <section
      style={{
        background: 'var(--card-bg)',
        boxShadow: 'var(--card-shadow)',
        borderRadius: 20,
        padding: 18,
        fontSize: 15,
        lineHeight: 1.8,
      }}
    >
      {children}
    </section>
  );
}

const URDU_STACK = { fontFamily: "'Noto Nastaliq Urdu', serif", direction: 'rtl' as const };

export default function Khatm() {
  return (
    <div className="tiles fatiha-bg" style={{ minHeight: '100dvh' }}>
      <BackBar home title="Completing the Quran" />
      <div className="mx-auto flex max-w-[560px] flex-col gap-3 px-4 py-4 pb-10">
        <header className="text-center" style={{ marginBottom: 4 }}>
          <h1 style={{ fontSize: 22 }}>Completing the Quran</h1>
          <p style={{ ...URDU_STACK, fontSize: 19, lineHeight: 2.2, color: 'var(--frame2)' }}>
            دعائے ختمِ قرآن
          </p>
        </header>

        {/* Intro */}
        <TextCard>
          <p>{withHonorifics(KHATM_INTRO.en)}</p>
          <p style={{ ...URDU_STACK, fontSize: 'calc(13px * var(--fs-ur, 1))', lineHeight: 2.3, marginTop: 10, color: 'var(--vc-ur)' }}>
            {KHATM_INTRO.ur}
          </p>
          <p style={{ fontSize: 13.5, lineHeight: 1.8, marginTop: 12, fontStyle: 'italic', color: 'var(--text)' }}>
            {withHonorifics(KHATM_INTRO_ATTRIBUTION.en)}
          </p>
          <p style={{ ...URDU_STACK, fontSize: 'calc(13px * var(--fs-ur, 1))', lineHeight: 2.3, marginTop: 6, color: 'var(--text)' }}>
            {KHATM_INTRO_ATTRIBUTION.ur}
          </p>
        </TextCard>

        {/* The short dua — one elegant card; segments flow ar → ur → en,
            separated by a subtle thin divider (no numbered boxes). */}
        <TextCard>
          {SHORT_DUA.map((seg, i) => (
            <div key={i}>
              {i > 0 && (
                <hr
                  style={{
                    border: 'none',
                    borderTop: '1px solid var(--line)',
                    margin: '16px 24px',
                  }}
                />
              )}
              <p className="khatm-ar">{seg.ar}</p>
              <p className="vc-ur">{seg.ur}</p>
              <p className="vc-en">{seg.en}</p>
            </div>
          ))}
        </TextCard>

        {/* Closing hadith + practice note */}
        <TextCard>
          <p style={{ fontStyle: 'italic' }}>{withHonorifics(CLOSING_HADITH.en)}</p>
          <p style={{ ...URDU_STACK, fontSize: 'calc(13px * var(--fs-ur, 1))', lineHeight: 2.3, marginTop: 10, color: 'var(--vc-ur)' }}>
            {CLOSING_HADITH.ur}
          </p>
          <p style={{ fontSize: 13.5, lineHeight: 1.8, marginTop: 14, fontStyle: 'italic', color: 'var(--text)' }}>
            {withHonorifics(CLOSING_HADITH.note_en)}
          </p>
          <p style={{ ...URDU_STACK, fontSize: 'calc(13px * var(--fs-ur, 1))', lineHeight: 2.3, marginTop: 6, color: 'var(--text)' }}>
            {CLOSING_HADITH.note_ur}
          </p>
        </TextCard>
      </div>
    </div>
  );
}
