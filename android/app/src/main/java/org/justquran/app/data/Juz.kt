package org.justquran.app.data

data class JuzStart(val juz: Int, val s: Int, val v: Int)

val JUZ_STARTS: List<JuzStart> = listOf(
    JuzStart(1, 1, 1), JuzStart(2, 2, 142), JuzStart(3, 2, 253), JuzStart(4, 3, 93),
    JuzStart(5, 4, 24), JuzStart(6, 4, 148), JuzStart(7, 5, 82), JuzStart(8, 6, 111),
    JuzStart(9, 7, 88), JuzStart(10, 8, 41), JuzStart(11, 9, 93), JuzStart(12, 11, 6),
    JuzStart(13, 12, 53), JuzStart(14, 15, 1), JuzStart(15, 17, 1), JuzStart(16, 18, 75),
    JuzStart(17, 21, 1), JuzStart(18, 23, 1), JuzStart(19, 25, 21), JuzStart(20, 27, 56),
    JuzStart(21, 29, 46), JuzStart(22, 33, 31), JuzStart(23, 36, 28), JuzStart(24, 39, 32),
    JuzStart(25, 41, 47), JuzStart(26, 46, 1), JuzStart(27, 51, 31), JuzStart(28, 58, 1),
    JuzStart(29, 67, 1), JuzStart(30, 78, 1)
)

val JUZ_NAMES: List<String> = listOf(
    "Alif Lām Mīm", "Sayaqūlu", "Tilka ar-Rusulu", "Lan Tānālū", "Wal-Muḥṣanāt",
    "Lā Yuḥibbu-llāhu", "Wa Idhā Samiʿū", "Wa Law Annanā", "Qāla al-Malaʾ", "Wa-Aʿlamū",
    "Yaʿtadhirūna", "Wa Mā min Dābbah", "Wa Mā Ubarriʾu", "Rubamā", "Subḥāna lladhī",
    "Qāla a-Lam", "Iqtaraba", "Qad Aflaḥa", "Wa-Qāla lladhīna", "Amman Khalaqa",
    "Utlu Mā Ūḥiya", "Wa-Man Yaqnut", "Wa-Mā Liya", "Fa-Man Aẓlamu", "Ilayhi Yuraddu",
    "Ḥā Mīm", "Qāla Fa-Mā Khaṭbukum", "Qad Samiʿa llāhu", "Tabāraka lladhī", "ʿAmma"
)

val JUZ_NAMES_AR: List<String> = listOf(
    "الم", "سيقول", "تلك الرسل", "لن تنالوا", "والمحصنات",
    "لا يحب الله", "وإذا سمعوا", "ولو أننا", "قال الملأ", "واعلموا",
    "يعتذرون", "وما من دابة", "وما أبرئ", "ربما", "سبحان الذي",
    "قال ألم", "اقترب", "قد أفلح", "وقال الذين", "أمن خلق",
    "اتل ما أوحي", "ومن يقنت", "وما لي", "فمن أظلم", "إليه يرد",
    "حم", "قال فما خطبكم", "قد سمع الله", "تبارك الذي", "عم"
)

private val JUZ_START_MAP: Map<String, Int> = JUZ_STARTS.associate { "${it.s}:${it.v}" to it.juz }

fun juzOf(s: Int, v: Int): Int {
    var juz = 1
    for (start in JUZ_STARTS) {
        if (start.s > s || (start.s == s && start.v > v)) break
        juz = start.juz
    }
    return juz
}

fun juzStartAt(s: Int, v: Int): Int? = JUZ_START_MAP["$s:$v"]
