import { useCallback, useEffect, useRef, useState } from 'react';
import { Check, ChevronDown, Loader2, Trash2, X } from 'lucide-react';
import { useSettings } from '@/lib/settings';
import {
  deleteAllTranslationAudio,
  deleteSurahAudio,
  downloadSurahAudio,
  loadDownloadStatuses,
  loadTranslationManifest,
  saveDownloadStatuses,
  setIdFor,
  setLabel,
} from '@/lib/translationAudio';
import type { TranslationSetId } from '@/lib/translationAudio';
import type { Bundle } from '@/lib/data';

export type BookLang = 'en' | 'ur';

function formatMB(bytes: number): string {
  return `${(bytes / 1e6).toFixed(bytes >= 1e8 ? 0 : 1)} MB`;
}

type DlStatus = 'none' | 'downloading' | 'done';

interface DlJob {
  surah: number;
  done: number;
  total: number;
}

export function NarrationSetDownloads({ lang, bundle }: { lang: BookLang; bundle: Bundle }) {
  const { settings } = useSettings();
  const setId: TranslationSetId = setIdFor(lang, settings.transVoice);
  const [doneMap, setDoneMap] = useState<Record<number, true>>(() => loadDownloadStatuses(setId));
  const [job, setJob] = useState<DlJob | null>(null);
  const [note, setNote] = useState<string | null>(null);
  const [confirm, setConfirm] = useState<
    { kind: 'all' } | { kind: 'delete'; surah: number; name: string } | { kind: 'deleteAll' } | null
  >(null);
  const [sizes, setSizes] = useState<Record<string, { v: number; bytes: number }> | null>(null);
  // Collapsible dropdown card for each narration language
  const [open, setOpen] = useState(false);
  // Per-surah download rows are collapsed by default so the chapter list
  // stays within easy reach; expanded on demand.
  const [expanded, setExpanded] = useState(false);
  const cancelRef = useRef(false);
  const runningRef = useRef(false);

  // Voice/language switch → reload statuses + sizes for the new set.
  useEffect(() => {
    setDoneMap(loadDownloadStatuses(setId));
    setSizes(null);
    loadTranslationManifest()
      .then((m) => setSizes(m.sets[setId]?.surahs ?? null))
      .catch(() => setSizes(null));
  }, [setId]);

  const markDone = useCallback(
    (nn: number) => {
      setDoneMap((prev) => {
        const next = { ...prev, [nn]: true as const };
        saveDownloadStatuses(setId, next);
        return next;
      });
    },
    [setId],
  );

  const startSurah = useCallback(
    async (nn: number) => {
      if (runningRef.current) return;
      runningRef.current = true;
      cancelRef.current = false;
      setNote(null);
      setJob({ surah: nn, done: 0, total: 0 });
      try {
        const skipped = await downloadSurahAudio(
          setId,
          nn,
          (done, total) => setJob({ surah: nn, done, total }),
          () => cancelRef.current,
        );
        markDone(nn);
        if (skipped > 0) setNote(`Surah ${nn}: ${skipped} file(s) failed and were skipped.`);
      } catch (e) {
        if ((e as Error).message !== 'cancelled')
          setNote(`Surah ${nn} download failed — check your connection and retry.`);
      } finally {
        setJob(null);
        runningRef.current = false;
      }
    },
    [setId, markDone],
  );

  const startAll = useCallback(async () => {
    if (runningRef.current) return;
    runningRef.current = true;
    cancelRef.current = false;
    setNote(null);
    let skippedTotal = 0;
    try {
      for (const s of bundle.surahs) {
        if (cancelRef.current) throw new Error('cancelled');
        if (doneMap[s.n]) continue;
        setJob({ surah: s.n, done: 0, total: 0 });
        skippedTotal += await downloadSurahAudio(
          setId,
          s.n,
          (done, total) => setJob({ surah: s.n, done, total }),
          () => cancelRef.current,
        );
        markDone(s.n);
      }
      setNote(
        skippedTotal > 0
          ? `Download complete — ${skippedTotal} file(s) failed and were skipped.`
          : 'All narration downloaded for offline listening.',
      );
    } catch (e) {
      if ((e as Error).message === 'cancelled') setNote('Download cancelled.');
      else setNote('Download failed — check your connection and retry.');
    } finally {
      setJob(null);
      runningRef.current = false;
    }
  }, [bundle, doneMap, setId, markDone]);

  const deleteSurah = useCallback(
    async (nn: number) => {
      await deleteSurahAudio(setId, nn);
      setDoneMap((prev) => {
        const next = { ...prev };
        delete next[nn];
        saveDownloadStatuses(setId, next);
        return next;
      });
    },
    [setId],
  );

  const deleteAll = useCallback(async () => {
    await deleteAllTranslationAudio();
    setDoneMap({});
    saveDownloadStatuses(setId, {});
  }, [setId]);

  const totalBytes = sizes
    ? Object.values(sizes).reduce((a, x) => a + x.bytes, 0)
    : null;
  const doneCount = Object.keys(doneMap).length;

  return (
    <div className="flex flex-col gap-3">
      <button
        type="button"
        className="card w-full text-left"
        onClick={() => setOpen((o) => !o)}
        style={{ border: '1px solid var(--hairline)' }}
      >
        <span className="min-w-0 flex-1">
          <span className="block" style={{ fontSize: 16, fontWeight: 500 }}>
            {lang === 'en' ? 'English Translation' : 'Urdu Translation'}
          </span>
          <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
            {lang === 'en'
              ? 'Brian · Verse-by-verse English narration · ~500 MB'
              : 'Fateh Muhammad Jalandhari · Verse-by-verse Urdu narration · ~500 MB'}
          </span>
        </span>
        <span className="chev">
          <ChevronDown
            size={18}
            style={{ transform: open ? 'rotate(180deg)' : undefined, transition: 'transform .15s' }}
          />
        </span>
      </button>

      {open && (
        <div className="flex flex-col gap-3">
          {/* Download all */}
      <button
        className="card w-full text-left"
        onClick={() => {
          if (job) cancelRef.current = true;
          else setConfirm({ kind: 'all' });
        }}
      >
        <span className="vnum" style={{ background: 'var(--frame2)' }}>
          <ChevronDown size={16} />
        </span>
        <span className="min-w-0">
          <span className="block" style={{ fontSize: 15 }}>
            {job ? 'Cancel download' : 'Download all narration'}
          </span>
          <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
            {job
              ? `Downloading surah ${job.surah}… ${job.total ? Math.round((job.done / job.total) * 100) : 0}%`
              : `Whole Quran · ${totalBytes ? `~${formatMB(totalBytes)}` : '…'} · Wi-Fi recommended`}
          </span>
          {job ? (
            <span
              className="block mt-2 rounded-full overflow-hidden"
              style={{ height: 4, background: 'var(--line)' }}
            >
              <span
                className="block h-full"
                style={{
                  width: `${job.total ? Math.round((job.done / job.total) * 100) : 0}%`,
                  background: 'var(--green)',
                  transition: 'width .2s linear',
                }}
              />
            </span>
          ) : null}
        </span>
        <span className="chev">{job ? <X size={20} /> : <ChevronDown size={20} />}</span>
      </button>

      {note ? (
        <p className="px-2" style={{ fontSize: 12, color: 'var(--muted)' }}>
          {note}
        </p>
      ) : null}

      <div className="px-2 flex items-center justify-between">
        <span style={{ fontSize: 11.5, color: 'var(--muted)' }}>
          {doneCount} of 114 surahs downloaded
        </span>
        {doneCount > 0 && !job ? (
          <button
            className="chip"
            style={{ padding: '5px 10px', fontSize: 12, display: 'inline-flex', alignItems: 'center', gap: 5 }}
            onClick={() => setConfirm({ kind: 'deleteAll' })}
          >
            <Trash2 size={13} /> Delete all
          </button>
        ) : null}
      </div>

      <button
        className="card w-full text-left"
        onClick={() => setExpanded((e) => !e)}
        aria-expanded={expanded}
      >
        <span className="vnum" style={{ background: 'var(--frame2)' }}>
          <ChevronDown
            size={16}
            style={{ transform: expanded ? 'rotate(180deg)' : undefined, transition: 'transform .15s' }}
          />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block" style={{ fontSize: 15 }}>
            Download individual surahs
          </span>
          <span className="block" style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>
            {expanded ? 'Tap a surah to download or delete it' : 'Pick specific surahs instead of the whole Quran'}
          </span>
        </span>
      </button>

      {expanded && bundle.surahs.map((s) => {
        const st: DlStatus = job?.surah === s.n ? 'downloading' : doneMap[s.n] ? 'done' : 'none';
        const pct = job?.surah === s.n && job.total ? Math.round((job.done / job.total) * 100) : 0;
        const sz = sizes?.[String(s.n).padStart(3, '0')];
        return (
          <button
            key={s.n}
            className="card w-full text-left"
            onClick={() => {
              if (st === 'done') setConfirm({ kind: 'delete', surah: s.n, name: s.name_en });
              else if (st === 'none') void startSurah(s.n);
              else cancelRef.current = true;
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
                    : `Not downloaded${sz ? ` · ${formatMB(sz.bytes)}` : ''}`}
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
        </div>
      )}

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
                ? 'Download all narration?'
                : confirm.kind === 'deleteAll'
                  ? 'Delete all downloads?'
                  : 'Delete download?'}
            </div>
            <p style={{ fontSize: 14, lineHeight: 1.7, color: 'var(--vc-en)', marginTop: 10 }}>
              {confirm.kind === 'all'
                ? `This downloads the whole Quran narration (${setLabel(setId)}${totalBytes ? `, ~${formatMB(totalBytes)}` : ''}). Wi-Fi is recommended.`
                : confirm.kind === 'deleteAll'
                  ? 'This removes every downloaded narration from this device. You can re-download them any time.'
                  : `Remove the downloaded narration for ${confirm.name}? You can re-download it any time.`}
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
                  else if (c.kind === 'deleteAll') void deleteAll();
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
