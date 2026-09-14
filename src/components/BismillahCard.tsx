import { BISMILLAH } from '@/components/BismillahBand';
import type { Verse } from '@/lib/data';

/** Card-mode Bismillah: the surah's bismillah band rendered AS the first card
 *  of the verse list, with the translation of the Bismillah (1:1) underneath,
 *  following the same translation toggles as the verse cards.
 *  Used for every surah in card mode EXCEPT Al-Fatiha (whose first verse card
 *  IS the Bismillah — it gets the band styling itself) and At-Tawbah (no
 *  Bismillah by convention). Folio mode keeps the plain BismillahBand.
 *  reciting=true highlights the card while the Bismillah audio prelude plays. */
export default function BismillahCard({ surahName, verse, showEn, showUr, showTr, reciting = false }:
  { surahName: string; verse: Verse; showEn: boolean; showUr: boolean; showTr: boolean; reciting?: boolean }) {
  return (
    <div className={reciting ? 'bismcard reciting' : 'bismcard'}>
      <div className="bism-name">{surahName}</div>
      <div className="bism-text">{BISMILLAH}</div>
      {showTr && <p className="bismcard-tr">{verse.tr}</p>}
      {showEn && <p className="bismcard-en">{verse.en}</p>}
      {showUr && verse.ur ? <p className="bismcard-ur">{verse.ur}</p> : null}
    </div>
  );
}
