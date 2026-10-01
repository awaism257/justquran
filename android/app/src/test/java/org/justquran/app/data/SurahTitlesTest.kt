package org.justquran.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SurahTitlesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testEnglishSurahTitlesMatchItaniClearQuran() {
        assertEquals(114, SurahTitles.english.size)
        assertEquals(114, SurahTitles.urdu.size)

        // Verify specific known differences from standard Tanzil meanings:
        assertEquals("The Opening", SurahTitles.englishName(1))
        assertEquals("The Heifer", SurahTitles.englishName(2))
        assertEquals("Women", SurahTitles.englishName(4))
        assertEquals("The Table", SurahTitles.englishName(5))
        assertEquals("Livestock", SurahTitles.englishName(6))
        assertEquals("The Elevations", SurahTitles.englishName(7))
        assertEquals("The Spoils", SurahTitles.englishName(8))
        assertEquals("Repentance", SurahTitles.englishName(9))
        assertEquals("The Rock", SurahTitles.englishName(15))
        assertEquals("History", SurahTitles.englishName(28))
        assertEquals("The Confederates", SurahTitles.englishName(33))
        assertEquals("Originator", SurahTitles.englishName(35))
        assertEquals("Mankind", SurahTitles.englishName(114))

        // Check bookTitle helper
        assertEquals("The Heifer", SurahTitles.bookTitle("en", 2))
        assertEquals("سورۃ بقرہ", SurahTitles.bookTitle("ur", 2))

        // Verify every entry against assets/data/surah-titles.json
        val titlesFile = File("src/main/assets/data/surah-titles.json")
        assertTrue("surah-titles.json should exist", titlesFile.exists())

        val rootObj = json.decodeFromString<JsonObject>(titlesFile.readText())
        val enObj = rootObj["en"] as JsonObject

        for (i in 1..114) {
            val expected = enObj[i.toString()]?.jsonPrimitive?.content
            val actual = SurahTitles.englishName(i)
            assertEquals("Surah $i title mismatch", expected, actual)
        }
    }
}
