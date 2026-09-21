import { useCallback, useEffect, useRef, useState } from 'react';
import { Check, ChevronDown, Loader2, Trash2, X } from 'lucide-react';
import BackBar from '@/components/BackBar';
import { AUDIO_CACHE, verseUrl } from '@/lib/audio';
import { loadBundle, type Bundle } from '@/lib/data';

type Status = 'none' | 'downloading' | 'done';

const LS_KEY = 'jq-audio-status-v1';

function loadStatuses(): Record<number, true> {
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw) as Record<string, true>;
    const out: Record<number, true> = {};
    for (const k of Object.keys(parsed)) out[Number(k)] = true;
    return out;
  } catch {
    return {};
  }
}

function saveStatuses(done: Record<number, true>) {
  try {
    localStorage.setItem(LS_KEY, JSON.stringify(done));
  } catch {
    /* storage full/unavailable — non-fatal */
  }
}

interface Job {
  surah: number;
  done: number;
  total: number;
}

export default function Recitation() {
  const [bundle, setBundle] = useState<Bundle | null>(null);
  const [doneMap, setDoneMap] = useState<Record<number, true>>(() => loadStatuses());
  const [job, setJob] = useState<Job | null>(null); // currently downloading surah
  const [allMode, setAllMode] = useState(false);
  const [note, setNote] = useState<string | null>(null);
  const [confirm, setConfirm] = useState<
    | { kind: 'all' }
    | { kind: 'delete'; surah: number; name: string }
    | { kind: 'deleteAll' }
    | null
  >(null);

  const cancelRef = useRef(false);
  const runningRef = useRef(false);

  useEffect(() => {
    loadBundle().then(setBundle).catch(() => setNote('Could not load surah list.'));
  }, []);

  const markDone = useCallback((n: number) => {
    setDoneMap((prev) => {
      const next = { ...prev, [n]: true as const };
      saveStatuses(next);
      return next;
    });
  }, []);

  /** Download one surah's verses sequentially into Cache Storage. */
  const downloadSurah = useCallback(
    async (n: number, total: number): Promise<number> => {
      const cache = await caches.open(AUDIO_CACHE);
      let skipped = 0;
      // The surah-playback Bismillah prelude reuses Al-Hussary's 1:1 file —
      // bundle it with every surah that opens with one, for full offline play.
      if (n !== 1 && n !== 9) {
        const bUrl = verseUrl(1, 1);
        if (!(await cache.match(bUrl))) {
          try {
            const res = await fetch(bUrl);
            if (res.ok) await cache.put(bUrl, res);
          } catch {
            /* prelude simply won't play offline */
          }
        }
      }
      for (let v = 1; v <= total; v++) {
        if (cancelRef.current) throw new Error('cancelled');
        const url = verseUrl(n, v);
        const existing = await cache.match(url);
        if (!existing) {
          let ok = false;
          for (let attempt = 0; attempt < 2 && !ok; attempt++) {
            try {
              const res = await fetch(url);
              if (!res.ok) throw new Error(`HTTP ${res.status}`);
              await cache.put(url, res);
              ok = true;
            } catch {
              if (cancelRef.current) throw new Error('cancelled');
              if (attempt === 1) skipped++;
            }
          }
        }
        setJob({ surah: n, done: v, total });
      }
      markDone(n);
      return skipped;
    },
    [markDone],
  );

  const startSurah = useCallback(
    async (n: number, total: number) => {
      if (runningRef.current) return;
      runningRef.current = true;
      cancelRef.current = false;
      setAllMode(false);
      setNote(null);
      setJob({ surah: n, done: 0, total });
      try {
        const skipped = await downloadSurah(n, total);
        if (skipped > 0) setNote(`Surah ${n}: ${skipped} verse(s) failed and were skipped.`);
      } catch (e) {
        if ((e as Error).message !== 'cancelled')
          setNote(`Surah ${n} download failed — check your connection and retry.`);
      } finally {
        setJob(null);
        runningRef.current = false;
      }
    },
    [downloadSurah],
  );

  const startAll = useCallback(async () => {
    if (runningRef.current || !bundle) return;
    runningRef.current = true;
    cancelRef.current = false;
    setAllMode(true);
    setNote(null);
    let skippedTotal = 0;
    try {
      for (const s of bundle.surahs) {
        if (cancelRef.current) throw new Error('cancelled');
        if (doneMap[s.n]) continue;
        setJob({ surah: s.n, done: 0, total: s.ayahs });
        skippedTotal += await downloadSurah(s.n, s.ayahs);
      }
      setNote(
        skippedTotal > 0
          ? `Download complete — ${skippedTotal} verse(s) failed and were skipped.`
          : 'All audio downloaded for offline use.',
      );
    } catch (e) {
      if ((e as Error).message === 'cancelled') setNote('Download cancelled.');
      else setNote('Download failed — check your connection and retry.');
    } finally {
      setJob(null);
      setAllMode(false);
      runningRef.current = false;
    }
  }, [bundle, doneMap, downloadSurah]);

  const deleteSurah = useCallback(
    async (n: number) => {
      const cache = await caches.open(AUDIO_CACHE);
      const total = bundle?.surahs.find((s) => s.n === n)?.ayahs ?? 0;
      for (let v = 1; v <= total; v++) await cache.delete(verseUrl(n, v));
      setDoneMap((prev) => {
        const next = { ...prev };
        delete next[n];
        saveStatuses(next);
        return next;
      });
    },
    [bundle],
  );

  /** Remove the whole audio cache and reset every surah's done flag. */
  const deleteAllAudio = useCallback(async () => {
    try {
      await caches.delete(AUDIO_CACHE);
    } catch {
      /* best effort */
    }
    setDoneMap({});
    saveStatuses({});
  }, []);

  const statusOf = (n: number): Status => {
    if (job?.surah === n) return 'downloading';
    return doneMap[n] ? 'done' : 'none';
  };

  const totalVerses = bundle ? bundle.surahs.reduce((a, s) => a + s.ayahs, 0) : 0;
  const doneSurahs = Object.keys(doneMap).length;
  const overallPct =
    allMode && job && bundle && totalVerses > 0
      ? Math.min(
          100,
          Math.round(
            ((bundle.surahs
              .slice(0, job.surah - 1)
              .reduce((a, s) => a + s.ayahs, 0) +
              job.done) /
              totalVerses) *
              100,
          ),
        )
      : null;

  return (
    <div>
      <BackBar title="Recitation" meta="Al-Hussary · Murattal" />

      <div className="px-4 pt-4 pb-6 flex flex-col gap-3">
        {/* Download all */}
        <button
          className="card w-full text-left"
          onClick={() => {
            if (job) {
              cancelRef.current = true;
            } else {
              setConfirm({ kind: 'all' });
            }
          }}
        >
          <span className="vnum" style={{ background: 'var(--frame2)' }}>
            <ChevronDown size={16} />
          </span>
          <span className="min-w-0">
            <span className="block" style={{ fontSize: 15 }}>
              {job ? 'Cancel download' : 'Download all audio'}
            </span>
            <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
              {overallPct !== null
                ? `Surah ${job?.surah} of 114 · ${overallPct}%`
                : job
                  ? `Downloading surah ${job.surah}… ${Math.round((job.done / job.total) * 100)}%`
                  : `Whole Quran · ~1 GB · Wi-Fi recommended`}
            </span>
            {job ? (
              <span
                className="block mt-2 rounded-full overflow-hidden"
                style={{ height: 4, background: 'var(--line)' }}
              >
                <span
                  className="block h-full"
                  style={{
                    width: `${overallPct !== null ? overallPct : Math.round((job.done / job.total) * 100)}%`,
                    background: 'var(--green)',
                    transition: 'width .2s linear',
                  }}
                />
              </span>
            ) : null}
          </span>
          <span className="chev">
            {job ? <X size={20} /> : <ChevronDown size={20} />}
          </span>
        </button>

        {note ? (
          <p className="px-2" style={{ fontSize: 12, color: 'var(--muted)' }}>
            {note}
          </p>
        ) : null}

        <div className="px-2 flex items-center justify-between">
          <span style={{ fontSize: 11.5, color: 'var(--muted)' }}>
            {doneSurahs} of 114 surahs downloaded
          </span>
          {doneSurahs > 0 && !job ? (
            <button
              className="chip"
              style={{ padding: '5px 10px', fontSize: 12, display: 'inline-flex', alignItems: 'center', gap: 5 }}
              onClick={() => setConfirm({ kind: 'deleteAll' })}
            >
              <Trash2 size={13} /> Delete all
            </button>
          ) : null}
        </div>

        {/* Per-surah rows */}
        {bundle?.surahs.map((s) => {
          const st = statusOf(s.n);
          const pct = job?.surah === s.n ? Math.round((job.done / job.total) * 100) : 0;
          return (
            <button
              key={s.n}
              className="card w-full text-left"
              onClick={() => {
                if (st === 'done') setConfirm({ kind: 'delete', surah: s.n, name: s.name_en });
                else if (st === 'none') void startSurah(s.n, s.ayahs);
                else cancelRef.current = true; // downloading → cancel
              }}
            >
              <span className="vnum">{s.n}</span>
              <span className="min-w-0 flex-1">
                <span className="block" style={{ fontSize: 15 }}>
                  {s.name_en}
                </span>
                <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
                  {st === 'done'
                    ? 'Downloaded ✓'
                    : st === 'downloading'
                      ? `Downloading… ${pct}%`
                      : `Not downloaded · ${s.ayahs} verses`}
                </span>
                {st === 'downloading' ? (
                  <span
                    className="block mt-2 rounded-full overflow-hidden"
                    style={{ height: 3, background: 'var(--line)' }}
                  >
                    <span
                      className="block h-full"
                      style={{ width: `${pct}%`, background: 'var(--green)', transition: 'width .2s linear' }}
                    />
                  </span>
                ) : null}
              </span>
              <span className="chev">
                {st === 'done' ? (
                  <Check size={18} style={{ color: 'var(--green)' }} />
                ) : st === 'downloading' ? (
                  <Loader2 size={18} className="animate-spin" style={{ color: 'var(--green)' }} />
                ) : (
                  <ChevronDown size={18} />
                )}
              </span>
            </button>
          );
        })}

        <p className="text-center pt-2 pb-6" style={{ fontSize: 12, color: 'var(--muted)' }}>
          Streaming works online · download for offline
        </p>
      </div>

      {/* Confirm dialog */}
      {confirm ? (
        <div className="popup-overlay" onClick={() => setConfirm(null)}>
          <div
            className="popup"
            style={{ borderRadius: 18, margin: 16, alignSelf: 'center' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="popup-label">
              {confirm.kind === 'all'
                ? 'Download all audio?'
                : confirm.kind === 'deleteAll'
                  ? 'Delete all downloads?'
                  : 'Delete download?'}
            </div>
            <p style={{ fontSize: 14, lineHeight: 1.7, color: 'var(--vc-en)', marginTop: 10 }}>
              {confirm.kind === 'all'
                ? 'This downloads the whole Quran recitation (~1 GB). Wi-Fi is recommended.'
                : confirm.kind === 'deleteAll'
                  ? 'This removes every downloaded surah from this device. You can re-download them any time.'
                  : `Remove the downloaded audio for ${confirm.name}? You can re-download it any time.`}
            </p>
            <div className="flex gap-3 mt-4">
              <button
                className="chip flex-1"
                style={{ padding: '10px 14px', textAlign: 'center' }}
                onClick={() => setConfirm(null)}
              >
                Cancel
              </button>
              <button
                className="chip on flex-1"
                style={{ padding: '10px 14px', textAlign: 'center', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}
                onClick={() => {
                  const c = confirm;
                  setConfirm(null);
                  if (c.kind === 'all') void startAll();
                  else if (c.kind === 'deleteAll') void deleteAllAudio();
                  else void deleteSurah(c.surah);
                }}
              >
                {confirm.kind === 'all' ? (
                  'Download'
                ) : (
                  <>
                    <Trash2 size={14} /> {confirm.kind === 'deleteAll' ? 'Delete all' : 'Delete'}
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
