// "Follow with translation" chain (v133, Settings → Recitation): after the
// Arabic recitation of a verse, read its meaning aloud — English (Brian)
// and/or Urdu (Jalandhari), each an opt-out sub-toggle. Shared by the
// card-mode surah chain (Reading) and the single-verse speaker button
// (VerseCard). The paged mushaf never uses this module: its recitation
// stays Arabic-only.

import {
  bismillahUrl,
  playTranslationFile,
  translationVerseUrl,
} from '@/lib/translationAudio';
import type { PlayHandle } from '@/lib/translationAudio';

export interface FollowLegs {
  english: boolean;
  urdu: boolean;
}

/**
 * Play the enabled narration legs of verse s:v back to back (English first,
 * then Urdu), then call onDone. v=0 means the surah's spoken Bismillah.
 *
 * A leg that fails to load is skipped silently and onDone still runs, so the
 * recitation chain never dies on a missing/unreachable narration file. If
 * playback is stopped or replaced mid-leg ('stopped'), the chain aborts
 * quietly — the stopper owns the cleanup.
 *
 * isCurrent() is the caller's chain-generation guard: once it goes false
 * (user skipped/stopped, surah changed), no further leg starts.
 */
export function playFollowLegs(opts: {
  s: number;
  v: number;
  legs: FollowLegs;
  isCurrent: () => boolean;
  onHandle: (h: PlayHandle) => void;
  onLegStart?: (lang: 'en' | 'ur') => void;
  onProgress?: (pct: number) => void;
  onDone: () => void;
}): void {
  const { s, v, legs, isCurrent, onHandle, onLegStart, onProgress, onDone } = opts;

  const queue: { lang: 'en' | 'ur'; url: Promise<string | null> }[] = [];
  if (legs.english) {
    queue.push({
      lang: 'en',
      url:
        v === 0
          ? bismillahUrl('en/brian-allah', s)
          : translationVerseUrl('en/brian-allah', s, v),
    });
  }
  if (legs.urdu) {
    queue.push({
      lang: 'ur',
      url: v === 0 ? bismillahUrl('ur', s) : translationVerseUrl('ur', s, v),
    });
  }
  if (queue.length === 0) {
    onDone();
    return;
  }

  const runLeg = (i: number): void => {
    if (!isCurrent()) return;
    if (i >= queue.length) {
      onDone();
      return;
    }
    const leg = queue[i];
    onLegStart?.(leg.lang);
    const h = playTranslationFile(
      leg.url.then((u) => {
        if (!u) throw new Error('no spoken Bismillah for this surah');
        return u;
      }),
      (reason) => {
        if (!isCurrent()) return;
        // 'stopped' — the user stopped or another source took over: abort.
        if (reason === 'stopped') return;
        // 'ended' → next leg; 'error' → skip the failed leg and keep going.
        runLeg(i + 1);
      },
      onProgress,
    );
    onHandle(h);
  };
  runLeg(0);
}
