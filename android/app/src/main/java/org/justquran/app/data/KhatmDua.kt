package org.justquran.app.data

data class LText(
    val en: String,
    val ur: String
)

data class DuaSegment(
    val ar: String,
    val ur: String,
    val en: String
)

data class PauseSign(
    val sign: String,
    val nameEn: String,
    val descEn: String,
    val descUr: String
) {
    val en: String get() = descEn
    val ur: String get() = descUr
}

data class PausesNote(
    val headingEn: String,
    val headingUr: String,
    val en: String,
    val ur: String,
    val signs: List<PauseSign>
)

data class ClosingHadith(
    val en: String,
    val ur: String,
    val noteEn: String,
    val noteUr: String
)

val KHATM_INTRO = LText(
    en = "Imam al-Nawawi \ufd40 relates in al-Adhkar that it is authentically reported that Mujahid \ufd40 — the leading scholar of Quran exegesis among the Followers in Makkah — said: 'They (the early Muslims) used to gather at the completion of the Quran and say: Mercy is now descending.' — al-Nawawi, al-Adhkar",
    ur = "امام نووی رحمہ اللہ کتاب الاذکار میں نقل کرتے ہیں کہ ثابت سند سے منقول ہے کہ مکے کے تابعین کے امام المفسرین مجاہد رحمہ اللہ فرماتے ہیں: \"لوگ (صحابہ و تابعین) ختمِ قرآن پر جمع ہوتے اور کہتے: ابھی رحمت نازل ہوئی ہے۔\" — الاذکار، امام نووی"
)

val KHATM_INTRO_ATTRIBUTION = LText(
    en = "The dua below is traditionally attributed to Abdullah ibn Mas'ud \ufd41 (may Allah be pleased with him).",
    ur = "ذیل کی دعا روایتاً عبداللہ بن مسعود رضی اللہ عنہ سے منسوب ہے۔"
)

val SHORT_DUA: List<DuaSegment> = listOf(
    DuaSegment(
        ar = "اَللّٰهُمَّ أَنِسْ وَحْشَتِيْ فِيْ قَبْرِيْ \u06dd",
        ur = "اے اللہ! مجھ سے میری قبر کی وحشت دور فرما۔",
        en = "O Allah, remove the loneliness of my grave."
    ),
    DuaSegment(
        ar = "اَللّٰهُمَّ ارْحَمْنِيْ بِالْقُرْآنِ الْعَظِيْمِ وَاجْعَلْهُ لِيْ إِمَامًا وَّنُوْرًا وَّهُدًى وَّرَحْمَةً \u06dd",
        ur = "اے اللہ! عظمت والے قرآن کے ذریعے مجھ پر رحم فرما، اور اس کو میرے لیے مقتدا اور نور اور ہدایت اور رحمت والا بنا۔",
        en = "O Allah, have mercy on me through the Glorious Quran, and make it for me a leader, a light, a guidance and a mercy."
    ),
    DuaSegment(
        ar = "اَللّٰهُمَّ ذَكِّرْنِيْ مِنْهُ مَا نَسِيْتُ وَعَلِّمِنِيْ مِنْهُ مَا جَهِلْتُ \u06dd",
        ur = "اے اللہ! اس کے اندر جو میں بھول گیا ہوں وہ مجھے یاد دلا، اور جو مجھے نہیں معلوم وہ مجھے سکھا دے۔",
        en = "O Allah, remind me of what I have forgotten of it, and teach me what I do not know of it."
    ),
    DuaSegment(
        ar = "وَارْزُقْنِيْ تِلَاوَتَهٗ أَنَآءَ الَّيْلِ وَأَنَآءَ النَّهَارِ \u06dd",
        ur = "اور دن رات اس کی تلاوت کرنے کی مجھے توفیق عطا فرما۔",
        en = "And grant me the ability to recite it throughout the night and the day."
    ),
    DuaSegment(
        ar = "وَاجْعَلْهُ لِيْ حُجَّةً يَّا رَبَّ الْعٰلَمِيْنَ \u06dd",
        ur = "اور اے سب جہانوں کے پالنے والے! اس کو میرے لیے دلیل بنا۔",
        en = "And make it a proof in my favour, O Lord of the worlds."
    )
)

val PAUSES_NOTE = PausesNote(
    headingEn = "The pauses of the Quran",
    headingUr = "تلاوت کے دوران کہاں رکنا ہے",
    en = "This card lists all the pause and section marks that appear in the app's IndoPak script, ordered from the most important. When in doubt, pause at a verse end.",
    ur = "ایپ میں استعمال ہونے والی تمام وقف کی علامتیں اہمیت کے ترتیب سے درج ہیں۔",
    signs = listOf(
        PauseSign(
            sign = "۩",
            nameEn = "Sajdah",
            descEn = "Verse of prostration: sajdah tilawah is due at this point. Appears 15 times in the Quran.",
            descUr = "سجدہ — یہ سجدۃ تلاوت کی جگہ ہے، یہاں سجدہ واجب ہے۔"
        ),
        PauseSign(
            sign = "ۘ",
            nameEn = "Meem (compulsory stop)",
            descEn = "Compulsory stop — you must pause here, or the meaning is altered.",
            descUr = "میم — یہاں وقف لازم ہے۔"
        ),
        PauseSign(
            sign = "ؔ",
            nameEn = "Takhallus",
            descEn = "The meaning is complete at this point — a valid place to stop.",
            descUr = "تخلص — یہاں معنی مکمل ہو جاتا ہے، وقف درست ہے۔"
        ),
        PauseSign(
            sign = "ۙ",
            nameEn = "La (no stop)",
            descEn = "Do not stop here — stopping would break the meaning.",
            descUr = "لا — یہاں وقف نہ کیجیے۔"
        ),
        PauseSign(
            sign = "ؕ",
            nameEn = "Tah (absolute pause)",
            descEn = "It is better to stop here.",
            descUr = "ط — یہاں وقف کرنا بہتر ہے۔"
        ),
        PauseSign(
            sign = "ۚ",
            nameEn = "Jeem (permissible stop)",
            descEn = "You may stop here or continue — both are fine.",
            descUr = "ج — رک سکتے ہیں اور جاری بھی رکھ سکتے ہیں۔"
        ),
        PauseSign(
            sign = "ࣗ",
            nameEn = "Qaf",
            descEn = "Stopping here is held to be better.",
            descUr = "ق — یہاں وقف کرنا بہتر کہا گیا ہے۔"
        ),
        PauseSign(
            sign = "ۖ",
            nameEn = "Silah (better to continue)",
            descEn = "Stopping is permitted, but continuing is preferable.",
            descUr = "صلے — رکنا جائز مگر آگے پڑھنا افضل ہے۔"
        ),
        PauseSign(
            sign = "ࣕ",
            nameEn = "Sad (licensed pause)",
            descEn = "Pause only if needed; continuing is much better.",
            descUr = "ص — ضرورت ہو تو رک سکتے ہیں، بصورتِ دیگر تلاوت جاری رکھیں۔"
        ),
        PauseSign(
            sign = "ؗ",
            nameEn = "Zain (no pause needed)",
            descEn = "Continue reading — no pause is needed here.",
            descUr = "ز — یہاں وقف کی ضرورت نہیں۔"
        ),
        PauseSign(
            sign = "ۛ",
            nameEn = "Embracing stop",
            descEn = "Stop at one of the two marked points, never both.",
            descUr = "معانقہ — دونوں نشانوں میں سے صرف ایک جگہ رکیں۔"
        ),
        PauseSign(
            sign = "ࣞ",
            nameEn = "Qif (stop!)",
            descEn = "An explicit instruction to stop here.",
            descUr = "قف — یہاں رک جائیے۔"
        ),
        PauseSign(
            sign = "ࣝ",
            nameEn = "Saktah",
            descEn = "A brief silent pause without taking a breath.",
            descUr = "سکتہ — سانس لیے بغیر مختصر وقفہ۔"
        ),
        PauseSign(
            sign = "ࣟ",
            nameEn = "Waqfah",
            descEn = "A longer pause, still without taking a breath.",
            descUr = "وقفہ — سانس لیے بغیر کچھ طویل وقفہ۔"
        ),
        PauseSign(
            sign = "ࣖ",
            nameEn = "Ruku mark",
            descEn = "Not a pause rule — marks the end of a ruku (a thematic section of the Quran).",
            descUr = "رکوع کی علامت — یہاں ایک رکوع مکمل ہوتا ہے۔"
        ),
        PauseSign(
            sign = "۞",
            nameEn = "Juz marker",
            descEn = "Not a pause rule — marks the start of a juz (para), one of the Quran's 30 sections. It appears before the verse where a new juz begins, and that verse is also highlighted with a soft gold wash.",
            descUr = "جز کا نشان — وقف کی علامت نہیں؛ یہاں قرآن کے تیس پاروں میں سے نئے پارے کا آغاز ہوتا ہے۔ یہ نشان نئے پارے کی پہلی آیت سے پہلے دکھایا جاتا ہے، اور اس آیت کے پیچھے ہلکا سنہری سایہ بھی ہوتا ہے۔"
        )
    )
)

val CLOSING_HADITH = ClosingHadith(
    en = "The Prophet ﷺ said: 'Whoever prays an obligatory prayer has an accepted supplication, and whoever completes a reading of the Quran has an accepted supplication.' — narrated by al-Tabarani; al-Darimi, al-Sunan",
    ur = "نبی کریم صلی اللہ علیہ وسلم نے فرمایا: \"جو شخص فرض نماز پڑھتا ہے اس کے لیے ایک قبول شدہ دعا ہے، اور جو شخص قرآن کی تلاوت مکمل کرتا ہے اس کے لیے ایک قبول شدہ دعا ہے۔\" — رواہ الطبرانی، سنن الدارمی",
    noteEn = "Imam al-Nawawi \ufd40 writes in al-Adhkar that it is recommended to begin another reading of the Quran immediately after completing one, as the pious predecessors loved to do. — al-Nawawi, al-Adhkar",
    noteUr = "امام نووی رحمہ اللہ الاذکار میں لکھتے ہیں کہ قرآن کا ختم کرتے ہی نئی تلاوت شروع کر دینا مستحب ہے، کیونکہ صالحین اس کو پسند کرتے تھے۔ — الاذکار، امام نووی"
)
