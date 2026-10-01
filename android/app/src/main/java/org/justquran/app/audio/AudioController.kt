package org.justquran.app.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.justquran.app.R
import org.justquran.app.data.AppSettings
import org.justquran.app.data.AudioDownloadRepository
import org.justquran.app.data.BundleRepository
import org.justquran.app.data.SettingsRepository
import java.io.File
import java.util.ArrayDeque

data class VerseRef(val s: Int, val v: Int)

class AudioController(
    private val appContext: Context,
    private val bundleRepository: BundleRepository,
    private val settingsRepository: SettingsRepository,
    private val downloads: AudioDownloadRepository
) {
    companion object {
        const val RECITER: String = "Sheikh Mahmoud Khalil Al-Hussary"
        const val ERROR_COPY: String =
            "Couldn't load the recitation audio. Check your internet connection, then tap ▶ Play surah to try again."
        private const val PROGRESS_POLL_MS: Long = 250L
    }

    data class PendingAdvance(val surah: Int, val chain: Boolean)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _currentVerse = MutableStateFlow<VerseRef?>(null)
    val currentVerse: StateFlow<VerseRef?> = _currentVerse

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _progressPct = MutableStateFlow(0f)
    val progressPct: StateFlow<Float> = _progressPct

    private val _autoAdvance = MutableStateFlow(false)
    val autoAdvance: StateFlow<Boolean> = _autoAdvance

    private val _chainActive = MutableStateFlow(false)
    val chainActive: StateFlow<Boolean> = _chainActive

    private val _chainSurah = MutableStateFlow<Int?>(null)
    val chainSurah: StateFlow<Int?> = _chainSurah

    private val _prelude = MutableStateFlow(false)
    val prelude: StateFlow<Boolean> = _prelude

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _pendingAdvance = MutableStateFlow<PendingAdvance?>(null)
    val pendingAdvance: StateFlow<PendingAdvance?> = _pendingAdvance

    private val _openVerseRequest = MutableStateFlow<VerseRef?>(null)
    val openVerseRequest: StateFlow<VerseRef?> = _openVerseRequest

    private val _translationLang = MutableStateFlow<String?>(null)
    /** Null = Arabic recitation active; "en" / "ur" = translation narration active. */
    val translationLang: StateFlow<String?> = _translationLang

    private var controller: MediaController? = null
    private val pendingActions = ArrayDeque<(MediaController) -> Unit>()
    private var progressJob: Job? = null
    private var triedFallback: Boolean = false
    private var chainMode: String? = null

    private val artworkBytes: ByteArray by lazy {
        appContext.resources.openRawResource(R.drawable.media_artwork).use { it.readBytes() }
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            triedFallback = false
            _progressPct.value = 0f
            if (mediaItem != null) {
                _active.value = true
                val mediaId = mediaItem.mediaId
                val parts = mediaId.split(":")
                if (parts[0] == "verse" || parts[0] == "narr") {
                    val s = (if (parts.size == 4) parts[2] else parts[1]).toIntOrNull()
                    val v = (if (parts.size == 4) parts[3] else parts[2]).toIntOrNull()
                    if (s != null && v != null) {
                        _prelude.value = false
                        _currentVerse.value = VerseRef(s, v)
                        return
                    }
                } else if (parts[0] == "prelude") {
                    val s = parts.last().toIntOrNull()
                    if (s != null) {
                        _prelude.value = true
                        _currentVerse.value = VerseRef(s, 0)
                        return
                    }
                }
            } else {
                _active.value = false
                _prelude.value = false
            }
        }

        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) {
                startProgressLoop()
            } else {
                progressJob?.cancel()
                progressJob = null
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_IDLE) {
                _active.value = controller?.currentMediaItem != null
            } else if (playbackState == Player.STATE_ENDED) {
                onEnded()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: run {
                failChain()
                return
            }
            val curItem = c.currentMediaItem
            val mediaId = curItem?.mediaId ?: ""
            // Translation narration leg failed -> skip gracefully to next leg/verse
            if (mediaId.startsWith("narr:") || mediaId.startsWith("prelude:en") || mediaId.startsWith("prelude:ur")) {
                if (c.hasNextMediaItem()) {
                    runCatching {
                        c.seekToNextMediaItem()
                        c.prepare()
                        c.play()
                    }
                    return
                } else {
                    onEnded()
                    return
                }
            }
            val uri = curItem?.localConfiguration?.uri
            if (!triedFallback && uri != null) {
                val uriStr = uri.toString()
                if (uriStr.startsWith(AudioUrls.BASE_PRIMARY)) {
                    triedFallback = true
                    val fallbackUri = Uri.parse(AudioUrls.BASE_FALLBACK + uriStr.removePrefix(AudioUrls.BASE_PRIMARY))
                    val idx = c.currentMediaItemIndex
                    if (idx >= 0) {
                        runCatching {
                            c.replaceMediaItem(idx, curItem.buildUpon().setUri(fallbackUri).build())
                            c.seekToDefaultPosition(idx)
                            c.prepare()
                            c.play()
                        }
                        return
                    }
                }
            }
            failChain()
        }
    }

    init {
        scope.launch {
            settingsRepository.settings.collect { settings ->
                _autoAdvance.value = settings.audioAutoAdvance
            }
        }
        connect()
    }

    private fun connect() {
        val future: ListenableFuture<MediaController> = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, AudioService::class.java))
        ).buildAsync()

        future.addListener({
            try {
                val mc = future.get()
                controller = mc
                runCatching { mc.addListener(playerListener) }
                while (!pendingActions.isEmpty()) {
                    val action = pendingActions.removeFirst()
                    runCatching { action(mc) }.onFailure { failQuietly() }
                }
            } catch (_: Throwable) {
                _error.value = ERROR_COPY
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun withController(action: (MediaController) -> Unit) {
        scope.launch {
            val mc = controller
            if (mc == null) {
                pendingActions.addLast(action)
            } else {
                runCatching { action(mc) }.onFailure { failQuietly() }
            }
        }
    }

    fun play(s: Int, v: Int) = playVerse(s, v)
    fun playVerse(s: Int, v: Int) {
        _error.value = null
        _chainActive.value = false
        _chainSurah.value = null
        scope.launch {
            bundleRepository.load()
            val currentSettings = settingsRepository.settings.first()
            val items = withContext(Dispatchers.IO) {
                buildItemsForVerse(s, v, currentSettings)
            }
            withController { mc ->
                triedFallback = false
                mc.setMediaItems(items)
                mc.prepare()
                mc.play()
            }
        }
    }

    fun startChain(surah: Int, from: Int = 1) {
        _error.value = null
        chainMode = null
        _translationLang.value = null
        scope.launch {
            bundleRepository.load()
            val meta = bundleRepository.surah(surah) ?: return@launch
            val currentSettings = settingsRepository.settings.first()
            val startVerse = from.coerceIn(1, meta.ayahs)
            val items = withContext(Dispatchers.IO) {
                buildList {
                    if (ChainLogic.needsPrelude(surah, startVerse)) {
                        addAll(buildItemsForPrelude(surah, meta.nameEn, currentSettings))
                    }
                    for (v in startVerse..meta.ayahs) {
                        addAll(buildItemsForVerse(surah, v, currentSettings))
                    }
                }
            }
            _chainActive.value = true
            _chainSurah.value = surah
            withController { mc ->
                triedFallback = false
                mc.setMediaItems(items)
                mc.prepare()
                mc.play()
            }
        }
    }

    fun startTranslationChain(surah: Int, lang: String) {
        _error.value = null
        chainMode = lang
        _translationLang.value = lang
        scope.launch {
            bundleRepository.load()
            val meta = bundleRepository.surah(surah) ?: return@launch
            val isUr = lang == "ur"
            val items = withContext(Dispatchers.IO) {
                buildList {
                    if (surah != 1 && surah != 9) {
                        val bisTitle = if (isUr) "شروع اللہ کا نام لے کر" else "Bismillah — In the name of Allah"
                        if (isUr) {
                            add(urduNarrationItemFor(surah, 0, "prelude:ur:$surah", bisTitle))
                        } else {
                            add(englishNarrationItemFor(surah, 0, "prelude:en:$surah", bisTitle))
                        }
                    }
                    for (v in 1..meta.ayahs) {
                        val title = "$surah. ${if (isUr) "آیت" else "Verse"} $v"
                        if (isUr) {
                            add(urduNarrationItemFor(surah, v, "narr:ur:$surah:$v", title))
                        } else {
                            add(englishNarrationItemFor(surah, v, "narr:en:$surah:$v", title))
                        }
                    }
                }
            }
            if (items.isEmpty()) return@launch
            _chainActive.value = true
            _chainSurah.value = surah
            _prelude.value = (surah != 1 && surah != 9)
            withController { mc ->
                triedFallback = false
                mc.setMediaItems(items)
                mc.prepare()
                mc.play()
            }
        }
    }

    fun togglePlayPause() {
        withController { mc ->
            if (mc.isPlaying) {
                mc.pause()
            } else {
                mc.play()
            }
        }
    }

    fun stopAll() {
        chainMode = null
        _translationLang.value = null
        _chainActive.value = false
        _chainSurah.value = null
        _prelude.value = false
        _pendingAdvance.value = null
        withController { mc ->
            mc.stop()
            mc.clearMediaItems()
        }
    }

    fun previousVerse() {
        withController { mc ->
            val curIdx = mc.currentMediaItemIndex
            if (curIdx <= 0) {
                mc.seekTo(0L)
                return@withController
            }
            if (mc.currentPosition > 3000L) {
                mc.seekTo(0L)
                return@withController
            }
            val curItem = mc.getMediaItemAt(curIdx)
            val curVerse = AudioService.mediaIdToVerse(curItem.mediaId)?.second
            var targetIdx = curIdx - 1
            while (targetIdx >= 0) {
                val prevItem = mc.getMediaItemAt(targetIdx)
                val prevVerse = AudioService.mediaIdToVerse(prevItem.mediaId)?.second
                if (prevVerse != curVerse) {
                    while (targetIdx > 0) {
                        val beforeItem = mc.getMediaItemAt(targetIdx - 1)
                        if (AudioService.mediaIdToVerse(beforeItem.mediaId)?.second == prevVerse) {
                            targetIdx--
                        } else {
                            break
                        }
                    }
                    mc.seekTo(targetIdx, 0L)
                    return@withController
                }
                targetIdx--
            }
            mc.seekTo(0, 0L)
        }
    }

    fun nextVerse() {
        withController { mc ->
            val curIdx = mc.currentMediaItemIndex
            val total = mc.mediaItemCount
            if (curIdx < 0 || curIdx >= total - 1) return@withController
            val curItem = mc.getMediaItemAt(curIdx)
            val curVerse = AudioService.mediaIdToVerse(curItem.mediaId)?.second
            for (idx in (curIdx + 1) until total) {
                val nextItem = mc.getMediaItemAt(idx)
                val nextVerse = AudioService.mediaIdToVerse(nextItem.mediaId)?.second
                if (nextVerse != curVerse) {
                    mc.seekTo(idx, 0L)
                    return@withController
                }
            }
            if (mc.hasNextMediaItem()) {
                mc.seekToNextMediaItem()
            }
        }
    }

    fun setAutoAdvance(on: Boolean) {
        scope.launch {
            settingsRepository.setAudioAutoAdvance(on)
        }
    }

    fun toggleAutoAdvance() {
        setAutoAdvance(!_autoAdvance.value)
    }

    fun cancelChain() {
        chainMode = null
        _chainActive.value = false
        _chainSurah.value = null
        _prelude.value = false
    }

    fun clearError() {
        _error.value = null
    }

    fun clearPendingAdvance() {
        _pendingAdvance.value = null
    }

    fun requestOpenVerse(s: Int, v: Int) {
        if (s in 1..114) {
            _openVerseRequest.value = VerseRef(s, v.coerceAtLeast(1))
        }
    }

    fun clearOpenVerseRequest() {
        _openVerseRequest.value = null
    }

    private fun itemFor(
        s: Int,
        v: Int,
        mediaId: String,
        titleOverride: String? = null
    ): MediaItem {
        val file: File? = downloads.localFile(s, v)
        val uri: Uri = if (file != null) {
            Uri.fromFile(file)
        } else {
            Uri.parse(AudioUrls.streamUrl(s, v))
        }
        val title = titleOverride ?: verseTitle(s, v)
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setDisplayTitle(title)
                    .setArtist(RECITER)
                    .setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .build()
            )
            .build()
    }

    private fun englishNarrationItemFor(
        s: Int,
        v: Int,
        mediaId: String,
        titleOverride: String? = null
    ): MediaItem {
        val file: File? = downloads.localFile(s, v, "en")
        val uri: Uri = if (file != null) Uri.fromFile(file) else Uri.parse(AudioUrls.englishNarrationUrl(s, v))
        val title = titleOverride ?: verseTitle(s, v)
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setDisplayTitle(title)
                    .setArtist("English narration · Brian")
                    .setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .build()
            )
            .build()
    }

    private fun urduNarrationItemFor(
        s: Int,
        v: Int,
        mediaId: String,
        titleOverride: String? = null
    ): MediaItem {
        val file: File? = downloads.localFile(s, v, "ur")
        val uri: Uri = if (file != null) Uri.fromFile(file) else Uri.parse(AudioUrls.urduNarrationUrl(s, v))
        val title = titleOverride ?: verseTitle(s, v)
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setDisplayTitle(title)
                    .setArtist("Urdu narration · Jalandhari")
                    .setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .build()
            )
            .build()
    }

    private fun buildItemsForVerse(s: Int, v: Int, settings: AppSettings): List<MediaItem> = buildList {
        val playArabic = settings.showAr || (!settings.showEn && !settings.showUr)
        if (playArabic) {
            add(itemFor(s, v, "verse:$s:$v"))
        }
        val playEn = if (settings.arabicOnly) (settings.followTranslation && settings.followEn) else settings.showEn
        if (playEn) {
            add(englishNarrationItemFor(s, v, "narr:en:$s:$v"))
        }
        val playUr = if (settings.arabicOnly) (settings.followTranslation && settings.followUr) else settings.showUr
        if (playUr) {
            add(urduNarrationItemFor(s, v, "narr:ur:$s:$v"))
        }
    }

    private fun buildItemsForPrelude(surah: Int, surahName: String, settings: AppSettings): List<MediaItem> = buildList {
        val playArabic = settings.showAr || (!settings.showEn && !settings.showUr)
        if (playArabic) {
            add(itemFor(1, 1, "prelude:$surah", "Bismillah — $surahName"))
        }
        val playEn = if (settings.arabicOnly) (settings.followTranslation && settings.followEn) else settings.showEn
        if (playEn) {
            add(englishNarrationItemFor(surah, 0, "prelude:en:$surah", "Bismillah — $surahName"))
        }
        val playUr = if (settings.arabicOnly) (settings.followTranslation && settings.followUr) else settings.showUr
        if (playUr) {
            add(urduNarrationItemFor(surah, 0, "prelude:ur:$surah", "Bismillah — $surahName"))
        }
    }

    private fun verseTitle(s: Int, v: Int): String {
        val nameEn = bundleRepository.surah(s)?.nameEn ?: "Surah $s"
        return "$s. $nameEn · verse $v"
    }

    private fun onEnded() {
        val chain = _chainActive.value
        val surah = _chainSurah.value
        val cur = _currentVerse.value
        if (!chain || surah == null) {
            if (!chain && cur != null && cur.v >= 1 && _autoAdvance.value) {
                scope.launch {
                    bundleRepository.load()
                    val advance = ChainLogic.afterVerse(cur.s, cur.v, { bundleRepository.surah(it)?.ayahs ?: 0 }, true)
                    when (advance) {
                        is ChainLogic.Advance.NextVerse -> playVerse(advance.s, advance.v)
                        is ChainLogic.Advance.NextSurah -> {
                            _pendingAdvance.value = PendingAdvance(advance.s, false)
                            playVerse(advance.s, 1)
                        }
                        ChainLogic.Advance.End -> {}
                    }
                }
            } else {
                _prelude.value = false
            }
            return
        }

        _prelude.value = false
        val mode = chainMode
        val nextSurah = ChainLogic.chainContinuation(surah, _autoAdvance.value)
        if (nextSurah != null) {
            _pendingAdvance.value = PendingAdvance(nextSurah, true)
            if (mode != null) {
                startTranslationChain(nextSurah, mode)
            } else {
                startChain(nextSurah, 1)
            }
            return
        }

        chainMode = null
        _translationLang.value = null
        _chainActive.value = false
        _chainSurah.value = null
        runCatching {
            controller?.stop()
            controller?.clearMediaItems()
        }
    }

    private fun failChain() {
        chainMode = null
        _translationLang.value = null
        _chainActive.value = false
        _chainSurah.value = null
        _prelude.value = false
        _error.value = ERROR_COPY
        runCatching {
            controller?.stop()
            controller?.clearMediaItems()
        }
    }

    private fun failQuietly() {
        chainMode = null
        _translationLang.value = null
        _chainActive.value = false
        _chainSurah.value = null
        _prelude.value = false
        _error.value = ERROR_COPY
        runCatching {
            controller?.stop()
            controller?.clearMediaItems()
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val mc = controller
                if (mc != null && mc.duration > 0) {
                    val pct = ((mc.currentPosition * 100f) / mc.duration).coerceIn(0f, 100f)
                    _progressPct.value = pct
                }
                delay(PROGRESS_POLL_MS)
            }
        }
    }
}
