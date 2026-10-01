package org.justquran.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.justquran.app.audio.AudioUrls
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class AudioDownloadRepository(private val appContext: Context) {
    val dir = File(appContext.filesDir, "audio")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null

    private val _scanAr = MutableStateFlow<Map<Int, Pair<Int, Long>>>(emptyMap())
    val scanAr: StateFlow<Map<Int, Pair<Int, Long>>> = _scanAr

    private val _scanEn = MutableStateFlow<Map<Int, Pair<Int, Long>>>(emptyMap())
    val scanEn: StateFlow<Map<Int, Pair<Int, Long>>> = _scanEn

    private val _scanUr = MutableStateFlow<Map<Int, Pair<Int, Long>>>(emptyMap())
    val scanUr: StateFlow<Map<Int, Pair<Int, Long>>> = _scanUr

    // For backwards compatibility
    val scan: StateFlow<Map<Int, Pair<Int, Long>>> = _scanAr

    private val _downloading = MutableStateFlow<Downloading?>(null)
    val downloading: StateFlow<Downloading?> = _downloading

    data class Downloading(
        val surah: Int,
        val done: Int,
        val total: Int,
        val error: String? = null,
        val all: Boolean = false,
        val lang: String = "ar"
    ) {
        val progressPct: Float
            get() = if (total > 0) (done * 100f) / total else 0f
    }

    init {
        refresh()
    }

    fun dirFor(lang: String): File {
        return File(dir, lang)
    }

    fun scanFlow(lang: String): StateFlow<Map<Int, Pair<Int, Long>>> = when (lang) {
        "en" -> _scanEn
        "ur" -> _scanUr
        else -> _scanAr
    }

    fun localFile(s: Int, v: Int, lang: String = "ar"): File? {
        val fileName = if (lang == "ar") {
            AudioUrls.fileName(s, v)
        } else if (v == 0) {
            if (lang == "en") "${AudioUrls.sss(s)}000.mp3" else "001001.mp3"
        } else {
            AudioUrls.fileName(s, v)
        }

        // Check language subfolder first
        val langFile = File(dirFor(lang), fileName)
        if (langFile.isFile && langFile.length() > 0) return langFile

        // For Arabic, check legacy root audio directory as fallback
        if (lang == "ar") {
            val rootFile = File(dir, fileName)
            if (rootFile.isFile && rootFile.length() > 0) return rootFile
        }
        return null
    }

    fun refresh() {
        scope.launch {
            _scanAr.value = scanDirectory(dirFor("ar"), legacyFallback = dir)
            _scanEn.value = scanDirectory(dirFor("en"))
            _scanUr.value = scanDirectory(dirFor("ur"))
        }
    }

    private fun scanDirectory(targetDir: File, legacyFallback: File? = null): Map<Int, Pair<Int, Long>> {
        val map = mutableMapOf<Int, Pair<Int, Long>>()
        fun scanDir(d: File) {
            d.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".mp3")) {
                    val s = file.name.take(3).toIntOrNull()
                    if (s != null) {
                        val cur = map[s] ?: Pair(0, 0L)
                        map[s] = Pair(cur.first + 1, cur.second + file.length())
                    }
                }
            }
        }
        scanDir(targetDir)
        if (legacyFallback != null && legacyFallback.exists()) {
            legacyFallback.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".mp3")) {
                    val s = file.name.take(3).toIntOrNull()
                    if (s != null && !map.containsKey(s)) {
                        val cur = map[s] ?: Pair(0, 0L)
                        map[s] = Pair(cur.first + 1, cur.second + file.length())
                    }
                }
            }
        }
        return map
    }

    fun downloadSurah(surah: Int, ayahs: Int, lang: String = "ar") {
        if (downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            dirFor(lang).mkdirs()
            if (fetchSurahSync(this, surah, ayahs, false, lang)) {
                _downloading.value = null
            }
            refresh()
        }
    }

    fun downloadAll(surahs: List<SurahMeta>, lang: String = "ar") {
        if (downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            dirFor(lang).mkdirs()
            var ok = true
            val currentScan = scanFlow(lang).value
            for (meta in surahs) {
                ensureActive()
                val current = currentScan[meta.n]?.first ?: 0
                if (current < meta.ayahs) {
                    ok = fetchSurahSync(this, meta.n, meta.ayahs, true, lang)
                    if (!ok) break
                }
            }
            if (ok) {
                _downloading.value = null
            }
            refresh()
        }
    }

    private fun fetchSurahSync(
        scope: CoroutineScope,
        surah: Int,
        ayahs: Int,
        all: Boolean,
        lang: String
    ): Boolean {
        val targetDir = dirFor(lang)
        targetDir.mkdirs()

        // Bismillah download
        if (surah != 1 && surah != 9) {
            val bisFile = localFile(surah, 0, lang)
            if (bisFile == null) {
                val bisUrl = when (lang) {
                    "en" -> AudioUrls.englishNarrationUrl(surah, 0)
                    "ur" -> AudioUrls.urduNarrationUrl(surah, 0)
                    else -> AudioUrls.streamUrl(1, 1)
                }
                val bisName = if (lang == "en") "${AudioUrls.sss(surah)}000.mp3"
                else if (lang == "ur") "001001.mp3"
                else AudioUrls.fileName(1, 1)
                fetchFile(targetDir, bisName, listOf(bisUrl))
            }
        }

        var count = (1..ayahs).count { localFile(surah, it, lang) != null }
        _downloading.value = Downloading(surah, count, ayahs, null, all, lang)
        for (v in 1..ayahs) {
            scope.ensureActive()
            if (localFile(surah, v, lang) == null) {
                val urls = when (lang) {
                    "en" -> listOf(AudioUrls.englishNarrationUrl(surah, v))
                    "ur" -> listOf(AudioUrls.urduNarrationUrl(surah, v))
                    else -> listOf(
                        AudioUrls.streamUrl(surah, v, false),
                        AudioUrls.streamUrl(surah, v, true)
                    )
                }
                val name = AudioUrls.fileName(surah, v)
                val ok = fetchFile(targetDir, name, urls)
                if (!ok) {
                    _downloading.value = Downloading(
                        surah, count, ayahs,
                        "Couldn't download verse $v. Check your internet connection and try again.",
                        all, lang
                    )
                    refresh()
                    return false
                }
                count++
                _downloading.value = Downloading(surah, count, ayahs, null, all, lang)
            }
        }
        return true
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloading.value = null
    }

    fun deleteSurah(surah: Int, lang: String = "ar") {
        val prefix = String.format(Locale.US, "%03d", surah)
        scope.launch {
            dirFor(lang).listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith(prefix)) {
                    file.delete()
                }
            }
            if (lang == "ar") {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith(prefix)) {
                        file.delete()
                    }
                }
            }
            refresh()
        }
    }

    fun deleteAll(lang: String = "ar") {
        if (_downloading.value?.lang == lang) {
            downloadJob?.cancel()
            downloadJob = null
            _downloading.value = null
        }
        scope.launch {
            dirFor(lang).deleteRecursively()
            if (lang == "ar") {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".mp3")) {
                        file.delete()
                    }
                }
            }
            refresh()
        }
    }

    private fun fetchFile(targetDir: File, name: String, urls: List<String>): Boolean {
        for (urlStr in urls) {
            try {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                try {
                    if (conn.responseCode == 200) {
                        val part = File(targetDir, "$name.part")
                        conn.inputStream.use { input ->
                            FileOutputStream(part).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (part.length() == 0L) {
                            part.delete()
                        } else {
                            val target = File(targetDir, name)
                            if (!part.renameTo(target)) {
                                part.copyTo(target, overwrite = true)
                                part.delete()
                            }
                            conn.disconnect()
                            return true
                        }
                    }
                    conn.disconnect()
                } catch (t: Throwable) {
                    conn.disconnect()
                    throw t
                }
            } catch (e: Exception) {
                File(targetDir, "$name.part").delete()
            }
        }
        return false
    }
}
