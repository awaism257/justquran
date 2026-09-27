/**
 * Audio bus — makes sure only one audio source plays at a time.
 * Each playback module (recitation, translation narration) registers its
 * stop function here; starting playback anywhere stops everything else.
 */

const stoppers = new Set<() => void>();

export function registerStopper(fn: () => void): void {
  stoppers.add(fn);
}

/** Stop every registered audio source except (optionally) one. */
export function stopAllAudio(except?: () => void): void {
  for (const fn of stoppers) {
    if (fn !== except) {
      try {
        fn();
      } catch {
        /* stopping is best-effort */
      }
    }
  }
}
