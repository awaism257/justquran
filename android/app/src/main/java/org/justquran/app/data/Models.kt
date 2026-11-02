package org.justquran.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SurahMeta(
    val n: Int,
    @SerialName("name_ar") val nameAr: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("name_meaning") val nameMeaning: String = "",
    val ayahs: Int,
) {
    val name_ar: String get() = nameAr
    val name_en: String get() = nameEn
    val name_meaning: String get() = nameMeaning
}

@Serializable
data class Verse(
    val s: Int,
    val v: Int,
    val ar: String,
    val en: String,
    val tr: String = "",
    val ur: String? = null,
    val wq: String? = null,
)

@Serializable
data class QuranBundle(
    val surahs: List<SurahMeta> = emptyList(),
    val verses: List<Verse> = emptyList(),
)

/** The verse text with verbatim characters, apart from the U+08DB mark; for sharing. */
fun Verse.shareArabic(): String = ar.replace('\u08db', '\u06e9')

/**
 * The verse text for drawing on screen. Android 12L and older don't know U+089C
 * (madda waajib, Unicode 14) and draw the word's two halves in swapped order, so
 * it is drawn via U+06E4, which the app's copy of the font maps to the same glyph.
 * Same length as [ar], so character offsets still line up.
 */
fun Verse.displayArabic(): String = shareArabic().replace('\u089c', '\u06e4')
