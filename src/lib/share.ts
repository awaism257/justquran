import type { Verse } from '@/lib/data';

/** Plain-text rendering of a verse for copy/share. */
export function verseText(v: Verse): string {
  const parts = [v.ar, '', v.en];
  if (v.ur) parts.push('', v.ur);
  parts.push('', `— Surah ${v.s}, verse ${v.v} · JustQuran`);
  return parts.join('\n');
}

/** Copy verse to clipboard. Returns true on success. */
export async function copyVerse(v: Verse): Promise<boolean> {
  const text = verseText(v);
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    // fallback for older browsers / non-secure contexts
    try {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      return true;
    } catch {
      return false;
    }
  }
}

/**
 * One-button share-or-copy: opens the native share sheet (which itself
 * contains a "Copy" action on both Android and iOS). On devices without a
 * share sheet (desktop browsers), falls back to a plain clipboard copy.
 * Returns 'shared' | 'copied' | 'failed'.
 */
export async function shareVerse(v: Verse): Promise<'shared' | 'copied' | 'failed'> {
  const text = verseText(v);
  if (typeof navigator.share === 'function') {
    try {
      await navigator.share({ text });
      return 'shared';
    } catch (e) {
      // user cancelled the sheet — treat as done, not failure
      if (e instanceof Error && e.name === 'AbortError') return 'shared';
    }
  }
  return (await copyVerse(v)) ? 'copied' : 'failed';
}
