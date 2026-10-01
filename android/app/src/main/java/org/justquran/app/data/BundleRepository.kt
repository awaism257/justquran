package org.justquran.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import org.justquran.app.audio.ChainLogic

class BundleRepository(private val appContext: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val mutex = Mutex()
    private var bundle: QuranBundle? = null
    private var versesBySurah: Map<Int, List<Verse>> = emptyMap()
    private var rukuStartsBySurah: List<List<Int>> = emptyList()

    private val _ready = kotlinx.coroutines.flow.MutableStateFlow(false)
    val ready: kotlinx.coroutines.flow.StateFlow<Boolean> = _ready
    val bundleLoaded: kotlinx.coroutines.flow.StateFlow<Boolean> get() = _ready

    val loaded: Boolean
        get() = bundle != null

    fun getSurahMetaOrEmpty(): List<SurahMeta> = bundle?.surahs ?: emptyList()

    fun getAllVersesOrEmpty(): List<Verse> = bundle?.verses ?: emptyList()

    suspend fun load(): QuranBundle = mutex.withLock {
        bundle?.let { return@withLock it }
        val parsed: QuranBundle = withContext(Dispatchers.IO) {
            appContext.assets.open("data/quran-bundle.json").use { stream ->
                val text = stream.bufferedReader().readText()
                json.decodeFromString<QuranBundle>(text)
            }
        }
        versesBySurah = parsed.verses.groupBy { it.s }
        rukuStartsBySurah = withContext(Dispatchers.IO) {
            loadRukuStarts()
        }
        bundle = parsed
        _ready.value = true
        parsed
    }

    private fun loadRukuStarts(): List<List<Int>> {
        return runCatching {
            appContext.assets.open("data/rukus.json").use { stream ->
                val root = json.parseToJsonElement(stream.bufferedReader().readText()).jsonObject
                val startsObj = root["starts"]!!.jsonObject
                (1..ChainLogic.LAST_SURAH).map { s ->
                    val arr = startsObj[s.toString()]!!.jsonArray
                    arr.map { it.jsonPrimitive.int }
                }
            }
        }.getOrElse {
            List(114) { listOf(1) }
        }
    }

    fun rukuStarts(surah: Int): List<Int> {
        val idx = surah - 1
        return if (idx in rukuStartsBySurah.indices) rukuStartsBySurah[idx] else listOf(1)
    }

    fun rukuCount(surah: Int): Int = rukuStarts(surah).size

    fun rukuIndexOf(surah: Int, verse: Int): Int {
        val starts = rukuStarts(surah)
        for (i in starts.indices.reversed()) {
            if (starts[i] <= verse) return i
        }
        return 0
    }

    fun surahOrNull(n: Int): SurahMeta? = bundle?.surahs?.firstOrNull { it.n == n }
    fun surah(n: Int): SurahMeta? = surahOrNull(n)

    fun verseOrNull(s: Int, v: Int): Verse? = versesBySurah[s]?.firstOrNull { it.v == v }

    fun verses(surah: Int): List<Verse> = versesBySurah[surah] ?: emptyList()

    fun juzOfSurahStart(n: Int): Int = juzOf(n, 1)
}
