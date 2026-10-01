package org.justquran.app.audio

import java.util.Locale

object AudioUrls {
    const val BASE_PRIMARY: String = "https://everyayah.com/data/Husary_64kbps"
    const val BASE_FALLBACK: String = "https://mirrors.quranicaudio.com/everyayah/Husary_64kbps"
    const val NARR_BASE: String = "https://pub-fafe102872f84521ab2a82e3dc2eeab0.r2.dev"

    fun sss(s: Int): String = String.format(Locale.US, "%03d", s)
    fun vvv(v: Int): String = String.format(Locale.US, "%03d", v)

    fun fileName(s: Int, v: Int): String {
        return String.format(Locale.US, "%03d%03d.mp3", s, v)
    }

    fun streamUrl(s: Int, v: Int, fallback: Boolean = false): String {
        val base = if (fallback) BASE_FALLBACK else BASE_PRIMARY
        return "$base/${fileName(s, v)}"
    }

    fun englishNarrationUrl(s: Int, v: Int): String {
        return if (v == 0) "$NARR_BASE/en/brian-allah/${sss(s)}000.mp3"
        else "$NARR_BASE/en/brian-allah/${fileName(s, v)}"
    }

    fun urduNarrationUrl(s: Int, v: Int): String {
        return if (v == 0) "$NARR_BASE/ur/001001.mp3"
        else "$NARR_BASE/ur/${fileName(s, v)}"
    }
}
