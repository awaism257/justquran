export const BISMILLAH = 'بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ';

/** Gold-bordered bismillah band shown atop each surah (except surah 9).
 *  The text is shown for every surah including Al-Fatiha — there it doubles
 *  verse 1:1, which is the Bismillah itself, and the band is given a solid
 *  gold frame (gold=true) to mark the opening of the Qur'an.
 *  reciting=true highlights the band while the Bismillah audio prelude plays. */
export default function BismillahBand({ surahName, reciting = false, gold = false }:
  { surahName: string; reciting?: boolean; gold?: boolean }) {
  const cls = ['bism', gold && 'fatiha', reciting && 'reciting'].filter(Boolean).join(' ');
  return (
    <div className={cls}>
      <div className="bism-name">{surahName}</div>
      <div className="bism-text">{BISMILLAH}</div>
    </div>
  );
}
