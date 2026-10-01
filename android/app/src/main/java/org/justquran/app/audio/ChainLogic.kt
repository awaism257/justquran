package org.justquran.app.audio

object ChainLogic {
    const val LAST_SURAH: Int = 114

    sealed interface Advance {
        data class NextVerse(val s: Int, val v: Int) : Advance
        data class NextSurah(val s: Int) : Advance
        object End : Advance
    }

    fun needsPrelude(surah: Int, fromVerse: Int): Boolean {
        return fromVerse == 1 && surah != 1 && surah != 9
    }

    fun afterVerse(
        s: Int,
        v: Int,
        ayahsOf: (Int) -> Int,
        autoAdvance: Boolean
    ): Advance {
        if (v < ayahsOf(s)) {
            return Advance.NextVerse(s, v + 1)
        }
        if (autoAdvance && s < LAST_SURAH) {
            return Advance.NextSurah(s + 1)
        }
        return Advance.End
    }

    fun chainContinuation(surah: Int, autoAdvance: Boolean): Int? {
        if (!autoAdvance || surah >= LAST_SURAH) {
            return null
        }
        return surah + 1
    }
}
