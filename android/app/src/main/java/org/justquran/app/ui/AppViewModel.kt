package org.justquran.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.justquran.app.AppContainer
import org.justquran.app.data.AppSettings
import org.justquran.app.data.FontKey
import org.justquran.app.data.SurahMeta
import org.justquran.app.data.ThemeChoice
import org.justquran.app.data.Verse

class AppViewModel(
    private val container: AppContainer
) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val bookmarks: StateFlow<Set<String>> = container.bookmarksRepository.bookmarks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _bundleReady = MutableStateFlow(false)
    val bundleReady: StateFlow<Boolean> = _bundleReady
    val bundleLoaded: StateFlow<Boolean> get() = _bundleReady

    init {
        viewModelScope.launch {
            try {
                container.settingsRepository.disableBookModeIfSet()
                container.settingsRepository.seedDefaultsIfNewInstall()
                container.bundleRepository.load()
            } catch (e: Exception) {
                android.util.Log.e("JustQuran", "Failed to initialize bundle or settings", e)
            } finally {
                _bundleReady.value = true
            }
        }
    }

    fun surahMeta(): List<SurahMeta> = container.bundleRepository.getSurahMetaOrEmpty()

    fun surah(n: Int): SurahMeta? = container.bundleRepository.surahOrNull(n)

    fun verses(surah: Int): List<Verse> = container.bundleRepository.verses(surah)

    fun verse(s: Int, v: Int): Verse? = container.bundleRepository.verseOrNull(s, v)

    fun allVerses(): List<Verse> = container.bundleRepository.getAllVersesOrEmpty()

    fun setTheme(choice: ThemeChoice): Job = viewModelScope.launch {
        container.settingsRepository.setTheme(choice)
    }

    fun setFontScale(key: FontKey, pct: Int): Job = viewModelScope.launch {
        container.settingsRepository.setFontScale(key, pct)
    }

    fun setShowAr(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setShowAr(v)
    }

    fun setShowEn(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setShowEn(v)
    }

    fun setShowUr(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setShowUr(v)
    }

    fun setShowTr(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setShowTr(v)
    }

    fun setVerseByVerse(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setVerseByVerse(v)
    }

    fun setAlwaysCreamPage(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setAlwaysCreamPage(v)
    }

    fun setAudioAutoAdvance(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setAudioAutoAdvance(v)
    }

    fun setFollowTranslation(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setFollowTranslation(v)
    }

    fun setFollowEn(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setFollowEn(v)
    }

    fun setFollowUr(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setFollowUr(v)
    }

    fun setVolumeKeysTurnPages(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setVolumeKeysTurnPages(v)
    }

    fun setMushafPaged(v: Boolean): Job = viewModelScope.launch {
        container.settingsRepository.setMushafPaged(v)
    }

    fun toggleArabicOnly() {
        val s = settings.value
        viewModelScope.launch {
            if (s.isArabicOnly()) {
                container.settingsRepository.leaveArabicOnly()
            } else {
                container.settingsRepository.enterArabicOnly()
            }
        }
    }

    fun setLastRead(s: Int, v: Int): Job = viewModelScope.launch {
        container.settingsRepository.setLastRead(s, v)
    }

    fun isBookmarked(s: Int, v: Int): Boolean = bookmarks.value.contains("$s:$v")

    fun toggleBookmark(s: Int, v: Int): Job = viewModelScope.launch {
        container.bookmarksRepository.toggle(s, v)
    }

    fun toggleBookmark(verse: Verse): Job = toggleBookmark(verse.s, verse.v)

    fun removeBookmark(s: Int, v: Int): Job = viewModelScope.launch {
        container.bookmarksRepository.remove("$s:$v")
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(container) as T
        }
    }
}
