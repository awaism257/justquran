import { useEffect, useLayoutEffect, useRef, useState, type CSSProperties } from 'react';
import type { Bundle } from '@/lib/data';
import { pageVerses } from '@/lib/pages';
import FolioPage from '@/components/FolioPage';

/** Font-size steps tried in turn until the page's text fits the fixed frame
    (the page must never scroll internally). */
const SHRINK_STEPS = [1, 0.92, 0.84, 0.76];

interface PagedFolioProps {
  bundle: Bundle;
  /** 1-based mushaf page (1..604) */
  page: number;
  /** dark-theme folio (`.night`) vs cream paper */
  night: boolean;
  /** verse currently being recited (whole-surah playback), highlighted inline */
  activeV?: number | null;
}

/**
 * Fixed-page mushaf (v114): one of the 604 printed-Medina pages, rendered with
 * FolioPage's exact frame/ornament/verse-marker markup inside a non-scrolling
 * box. If the text overflows, --paged-shrink steps the Arabic size down
 * (100% → 92% → 84% → 76%) until it fits.
 */
export default function PagedFolio({ bundle, page, night, activeV = null }: PagedFolioProps) {
  const verses = pageVerses(page, bundle);
  const wrapRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);
  const [shrink, setShrink] = useState(0);

  // new page (or new data) → try full size again
  useEffect(() => setShrink(0), [page, bundle]);

  useLayoutEffect(() => {
    const wrap = wrapRef.current;
    const content = contentRef.current;
    if (!wrap || !content) return;
    if (shrink < SHRINK_STEPS.length - 1 && content.scrollHeight > wrap.clientHeight + 1) {
      setShrink(shrink + 1);
    }
  });

  return (
    <div ref={wrapRef} style={{ height: '100%', minHeight: 0, overflow: 'hidden' }}>
      <div
        ref={contentRef}
        style={{ '--paged-shrink': SHRINK_STEPS[shrink] } as CSSProperties}
      >
        {/* Page 1 is Al-Fatiha alone, so the gold frame / Bismillah-skip rules
            of the continuous folio apply exactly as in surah view. */}
        <FolioPage verses={verses} night={night} activeV={activeV} fatiha={page === 1} />
      </div>
    </div>
  );
}
