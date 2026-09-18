// Content for the "Completing the Quran" (دعائے ختمِ قرآن) screen.
// Arabic & Urdu: Qudrat Ullah Company edition of the Jalandhari translation,
// printed pp. 1145–1149 (our licensed scan). English: JustQuran project.

export interface DuaSegment {
  ar: string;
  ur: string;
  en: string;
}

export const KHATM_INTRO = {
  en: "Imam al-Nawawi ﵀ relates in al-Adhkar that it is authentically reported that Mujahid ﵀ — the leading scholar of Quran exegesis among the Followers in Makkah — said: 'They (the early Muslims) used to gather at the completion of the Quran and say: Mercy is now descending.' — al-Nawawi, al-Adhkar",
  ur: 'امام نووی رحمہ اللہ کتاب الاذکار میں نقل کرتے ہیں کہ ثابت سند سے منقول ہے کہ مکے کے تابعین کے امام المفسرین مجاہد رحمہ اللہ فرماتے ہیں: "لوگ (صحابہ و تابعین) ختمِ قرآن پر جمع ہوتے اور کہتے: ابھی رحمت نازل ہوئی ہے۔" — الاذکار، امام نووی',
};

export const KHATM_INTRO_ATTRIBUTION = {
  en: "The dua below is traditionally attributed to Abdullah ibn Mas'ud ﵁ (may Allah be pleased with him).",
  ur: 'ذیل کی دعا روایتاً عبداللہ بن مسعود رضی اللہ عنہ سے منسوب ہے۔',
};

// The short dua (p. 1145) — five segments.
export const SHORT_DUA: DuaSegment[] = [
  {
    ar: 'اَللّٰهُمَّ أَنِسْ وَحْشَتِيْ فِيْ قَبْرِيْ ۝',
    ur: 'اے اللہ! مجھ سے میری قبر کی وحشت دور فرما۔',
    en: 'O Allah, remove the loneliness of my grave.',
  },
  {
    ar: 'اَللّٰهُمَّ ارْحَمْنِيْ بِالْقُرْآنِ الْعَظِيْمِ وَاجْعَلْهُ لِيْ إِمَامًا وَّنُوْرًا وَّهُدًى وَّرَحْمَةً ۝',
    ur: 'اے اللہ! عظمت والے قرآن کے ذریعے مجھ پر رحم فرما، اور اس کو میرے لیے مقتدا اور نور اور ہدایت اور رحمت والا بنا۔',
    en: 'O Allah, have mercy on me through the Glorious Quran, and make it for me a leader, a light, a guidance and a mercy.',
  },
  {
    ar: 'اَللّٰهُمَّ ذَكِّرْنِيْ مِنْهُ مَا نَسِيْتُ وَعَلِّمِنِيْ مِنْهُ مَا جَهِلْتُ ۝',
    ur: 'اے اللہ! اس کے اندر جو میں بھول گیا ہوں وہ مجھے یاد دلا، اور جو مجھے نہیں معلوم وہ مجھے سکھا دے۔',
    en: 'O Allah, remind me of what I have forgotten of it, and teach me what I do not know of it.',
  },
  {
    ar: 'وَارْزُقْنِيْ تِلَاوَتَهٗ أَنَآءَ الَّيْلِ وَأَنَآءَ النَّهَارِ ۝',
    ur: 'اور دن رات اس کی تلاوت کرنے کی مجھے توفیق عطا فرما۔',
    en: 'And grant me the ability to recite it throughout the night and the day.',
  },
  {
    ar: 'وَاجْعَلْهُ لِيْ حُجَّةً يَّا رَبَّ الْعٰلَمِيْنَ ۝',
    ur: 'اور اے سب جہانوں کے پالنے والے! اس کو میرے لیے دلیل بنا۔',
    en: 'And make it a proof in my favour, O Lord of the worlds.',
  },
];

// The pause (waqf) signs — one row per sign, shown on the Help page.
export interface PauseSign {
  sign: string;
  name_en: string;
  en: string;
  ur: string;
}

export const PAUSES_NOTE = {
  heading_en: 'The pauses of the Quran',
  heading_ur: 'تلاوت کے دوران کہاں رکنا ہے',
  en: "This card lists all the pause and section marks that appear in the app's IndoPak script, ordered from the most important. When in doubt, pause at a verse end.",
  ur: 'ایپ میں استعمال ہونے والی تمام وقف کی علامتیں اہمیت کے ترتیب سے درج ہیں۔',
  signs: [
    { sign: '\u06e9', name_en: 'Sajdah', en: 'Verse of prostration: sajdah tilawah is due at this point. Appears 15 times in the Quran.', ur: 'سجدہ — یہ سجدۃ تلاوت کی جگہ ہے، یہاں سجدہ واجب ہے۔' },
    { sign: '\u06D8', name_en: 'Meem (compulsory stop)', en: 'Compulsory stop — you must pause here, or the meaning is altered.', ur: 'میم — یہاں وقف لازم ہے۔' },
    { sign: '\u0614', name_en: 'Takhallus', en: 'The meaning is complete at this point — a valid place to stop.', ur: 'تخلص — یہاں معنی مکمل ہو جاتا ہے، وقف درست ہے۔' },
    { sign: '\u06D9', name_en: 'La (no stop)', en: 'Do not stop here — stopping would break the meaning.', ur: 'لا — یہاں وقف نہ کیجیے۔' },
    { sign: '\u0615', name_en: 'Tah (absolute pause)', en: 'It is better to stop here.', ur: 'ط — یہاں وقف کرنا بہتر ہے۔' },
    { sign: '\u06DA', name_en: 'Jeem (permissible stop)', en: 'You may stop here or continue — both are fine.', ur: 'ج — رک سکتے ہیں اور جاری بھی رکھ سکتے ہیں۔' },
    { sign: '\u08D7', name_en: 'Qaf', en: 'Stopping here is held to be better.', ur: 'ق — یہاں وقف کرنا بہتر کہا گیا ہے۔' },
    { sign: '\u06D6', name_en: 'Silah (better to continue)', en: 'Stopping is permitted, but continuing is preferable.', ur: 'صلے — رکنا جائز مگر آگے پڑھنا افضل ہے۔' },
    { sign: '\u08D5', name_en: 'Sad (licensed pause)', en: 'Pause only if needed; continuing is much better.', ur: 'ص — ضرورت ہو تو رک سکتے ہیں، بصورتِ دیگر تلاوت جاری رکھیں۔' },
    { sign: '\u0617', name_en: 'Zain (no pause needed)', en: 'Continue reading — no pause is needed here.', ur: 'ز — یہاں وقف کی ضرورت نہیں۔' },
    { sign: '\u06DB', name_en: 'Embracing stop', en: 'Stop at one of the two marked points, never both.', ur: 'معانقہ — دونوں نشانوں میں سے صرف ایک جگہ رکیں۔' },
    { sign: '\u08DE', name_en: 'Qif (stop!)', en: 'An explicit instruction to stop here.', ur: 'قف — یہاں رک جائیے۔' },
    { sign: '\u08DD', name_en: 'Saktah', en: 'A brief silent pause without taking a breath.', ur: 'سکتہ — سانس لیے بغیر مختصر وقفہ۔' },
    { sign: '\u08DF', name_en: 'Waqfah', en: 'A longer pause, still without taking a breath.', ur: 'وقفہ — سانس لیے بغیر کچھ طویل وقفہ۔' },
    { sign: '\u08D6', name_en: 'Ruku mark', en: 'Not a pause rule — marks the end of a ruku (a thematic section of the Quran).', ur: 'رکوع کی علامت — یہاں ایک رکوع مکمل ہوتا ہے۔' },
  ] as PauseSign[],
};

export const CLOSING_HADITH = {
  en: "The Prophet ﷺ said: 'Whoever prays an obligatory prayer has an accepted supplication, and whoever completes a reading of the Quran has an accepted supplication.' — narrated by al-Tabarani; al-Darimi, al-Sunan",
  ur: 'نبی کریم صلی اللہ علیہ وسلم نے فرمایا: "جو شخص فرض نماز پڑھتا ہے اس کے لیے ایک قبول شدہ دعا ہے، اور جو شخص قرآن کی تلاوت مکمل کرتا ہے اس کے لیے ایک قبول شدہ دعا ہے۔" — رواہ الطبرانی، سنن الدارمی',
  note_en: 'Imam al-Nawawi ﵀ writes in al-Adhkar that it is recommended to begin another reading of the Quran immediately after completing one, as the pious predecessors loved to do. — al-Nawawi, al-Adhkar',
  note_ur: 'امام نووی رحمہ اللہ الاذکار میں لکھتے ہیں کہ قرآن کا ختم کرتے ہی نئی تلاوت شروع کر دینا مستحب ہے، کیونکہ صالحین اس کو پسند کرتے تھے۔ — الاذکار، امام نووی',
};
