package org.justquran.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BundleParsingTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testQuranBundleJsonParsesCorrectly() {
        val bundleFile = File("src/main/assets/data/quran-bundle.json")
        assertTrue("Bundle file should exist", bundleFile.exists())

        val text = bundleFile.readText()
        val bundle = json.decodeFromString<QuranBundle>(text)

        assertEquals("Should have 114 surahs", 114, bundle.surahs.size)
        assertEquals("Should have 6236 verses", 6236, bundle.verses.size)

        val fatiha = bundle.surahs.first()
        assertEquals(1, fatiha.n)
        assertEquals("Al-Fatihah", fatiha.nameEn)
        assertEquals("الفاتحة", fatiha.nameAr)
        assertEquals("The Opener", fatiha.nameMeaning)
        assertEquals(7, fatiha.ayahs)

        val nas = bundle.surahs.last()
        assertEquals(114, nas.n)
        assertEquals("An-Nas", nas.nameEn)
        assertEquals(6, nas.ayahs)

        val v1_1 = bundle.verses.first()
        assertEquals(1, v1_1.s)
        assertEquals(1, v1_1.v)
        assertTrue(v1_1.ar.isNotEmpty())
        assertTrue(v1_1.en.isNotEmpty())
        assertNotNull(v1_1.ur)

        val v2_255 = bundle.verses.firstOrNull { it.s == 2 && it.v == 255 }
        assertNotNull("Ayat al-Kursi should exist", v2_255)
        assertEquals(2, v2_255!!.s)
        assertEquals(255, v2_255.v)
    }
}
