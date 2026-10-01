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

fun Verse.displayArabic(): String = ar.replace('\u08db', '\u06e9')
