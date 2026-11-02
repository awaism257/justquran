package org.justquran.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayArabicTest {
    private val verse = Verse(s = 8, v = 48, ar = "تَرَا\u089cءَتِ", en = "")

    @Test
    fun drawnTextAvoidsUnicode14MaddaWaajib() {
        assertEquals("تَرَا\u06e4ءَتِ", verse.displayArabic())
        assertEquals(verse.ar.length, verse.displayArabic().length)
    }

    @Test
    fun sharedTextKeepsVerbatimCharacters() {
        assertEquals(verse.ar, verse.shareArabic())
    }
}
