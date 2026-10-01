package org.justquran.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val FONT_SCALE_MIN = 70
const val FONT_SCALE_MAX = 160
const val FONT_SCALE_DEFAULT = 100

private val Context.settingsStore by preferencesDataStore(name = "settings")

enum class ThemeChoice(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeChoice = when (key) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}

enum class FontKey {
    AR, EN, UR, TR
}

data class AppSettings(
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val fontAr: Int = FONT_SCALE_DEFAULT,
    val fontEn: Int = FONT_SCALE_DEFAULT,
    val fontUr: Int = FONT_SCALE_DEFAULT,
    val fontTr: Int = FONT_SCALE_DEFAULT,
    val showAr: Boolean = true,
    val showEn: Boolean = true,
    val showUr: Boolean = true,
    val showTr: Boolean = true,
    val verseByVerse: Boolean = true,
    val alwaysCreamPage: Boolean = false,
    val audioAutoAdvance: Boolean = true,
    val followTranslation: Boolean = false,
    val followEn: Boolean = true,
    val followUr: Boolean = true,
    val volumeKeysTurnPages: Boolean = true,
    val mushafPaged: Boolean = true,
    val bookMode: Boolean = false,
    val bookHintSeen: Boolean = false,
    val lastRead: String? = null
) {
    val arabicOnly: Boolean
        get() = showAr && !showEn && !showUr && !showTr

    fun isArabicOnly(): Boolean = arabicOnly

    val folioMode: Boolean
        get() = !mushafPaged

    fun fontScale(key: FontKey): Int = when (key) {
        FontKey.AR -> fontAr
        FontKey.EN -> fontEn
        FontKey.UR -> fontUr
        FontKey.TR -> fontTr
    }
}

class SettingsRepository(private val appContext: Context) {

    private object K {
        val THEME = stringPreferencesKey("theme")
        val FONT_AR = intPreferencesKey("font_ar")
        val FONT_EN = intPreferencesKey("font_en")
        val FONT_UR = intPreferencesKey("font_ur")
        val FONT_TR = intPreferencesKey("font_tr")
        val SHOW_AR = booleanPreferencesKey("showAr")
        val SHOW_EN = booleanPreferencesKey("showEn")
        val SHOW_UR = booleanPreferencesKey("showUr")
        val SHOW_TR = booleanPreferencesKey("showTr")
        val VERSE_BY_VERSE = booleanPreferencesKey("verseByVerse")
        val ALWAYS_CREAM = booleanPreferencesKey("alwaysCreamPage")
        val AUDIO_AUTO = booleanPreferencesKey("audioAutoAdvance")
        val FOLLOW_TRANS = booleanPreferencesKey("followTranslation")
        val FOLLOW_EN = booleanPreferencesKey("followEn")
        val FOLLOW_UR = booleanPreferencesKey("followUr")
        val VOL_TURN = booleanPreferencesKey("volumeKeysTurnPages")
        val MUSHAF_PAGED = booleanPreferencesKey("mushafPaged")
        val BOOK_MODE = booleanPreferencesKey("bookMode")
        val BOOK_HINT_SEEN = booleanPreferencesKey("bookHintSeen")
        val BOOK_LANG = stringPreferencesKey("book_lang")
        val PREV_EN = booleanPreferencesKey("preMushafEn")
        val PREV_UR = booleanPreferencesKey("preMushafUr")
        val PREV_TR = booleanPreferencesKey("preMushafTr")
        val PREV_VBV = booleanPreferencesKey("preMushafVbv")
        val LAST_READ = stringPreferencesKey("last_read")
    }

    val settings: Flow<AppSettings> = appContext.settingsStore.data.map { prefs ->
        AppSettings(
            theme = ThemeChoice.fromKey(prefs[K.THEME]),
            fontAr = prefs[K.FONT_AR]?.takeIf { it in FONT_SCALE_MIN..FONT_SCALE_MAX } ?: FONT_SCALE_DEFAULT,
            fontEn = prefs[K.FONT_EN]?.takeIf { it in FONT_SCALE_MIN..FONT_SCALE_MAX } ?: FONT_SCALE_DEFAULT,
            fontUr = prefs[K.FONT_UR]?.takeIf { it in FONT_SCALE_MIN..FONT_SCALE_MAX } ?: FONT_SCALE_DEFAULT,
            fontTr = prefs[K.FONT_TR]?.takeIf { it in FONT_SCALE_MIN..FONT_SCALE_MAX } ?: FONT_SCALE_DEFAULT,
            showAr = prefs[K.SHOW_AR] ?: true,
            showEn = prefs[K.SHOW_EN] ?: true,
            showUr = prefs[K.SHOW_UR] ?: true,
            showTr = prefs[K.SHOW_TR] ?: true,
            verseByVerse = prefs[K.VERSE_BY_VERSE] ?: true,
            alwaysCreamPage = prefs[K.ALWAYS_CREAM] ?: false,
            audioAutoAdvance = prefs[K.AUDIO_AUTO] ?: true,
            followTranslation = prefs[K.FOLLOW_TRANS] ?: false,
            followEn = prefs[K.FOLLOW_EN] ?: true,
            followUr = prefs[K.FOLLOW_UR] ?: true,
            volumeKeysTurnPages = prefs[K.VOL_TURN] ?: true,
            mushafPaged = prefs[K.MUSHAF_PAGED] ?: true,
            bookMode = prefs[K.BOOK_MODE] ?: false,
            bookHintSeen = prefs[K.BOOK_HINT_SEEN] ?: false,
            lastRead = prefs[K.LAST_READ]
        )
    }

    val bookLang: Flow<String> = appContext.settingsStore.data.map { prefs ->
        prefs[K.BOOK_LANG] ?: "en"
    }

    suspend fun setTheme(choice: ThemeChoice) {
        appContext.settingsStore.edit { it[K.THEME] = choice.key }
    }

    suspend fun setFontScale(key: FontKey, pct: Int) {
        val clamped = pct.coerceIn(FONT_SCALE_MIN, FONT_SCALE_MAX)
        appContext.settingsStore.edit {
            when (key) {
                FontKey.AR -> it[K.FONT_AR] = clamped
                FontKey.EN -> it[K.FONT_EN] = clamped
                FontKey.UR -> it[K.FONT_UR] = clamped
                FontKey.TR -> it[K.FONT_TR] = clamped
            }
        }
    }

    suspend fun setShowAr(v: Boolean) { appContext.settingsStore.edit { it[K.SHOW_AR] = v } }
    suspend fun setShowEn(v: Boolean) { appContext.settingsStore.edit { it[K.SHOW_EN] = v } }
    suspend fun setShowUr(v: Boolean) { appContext.settingsStore.edit { it[K.SHOW_UR] = v } }
    suspend fun setShowTr(v: Boolean) { appContext.settingsStore.edit { it[K.SHOW_TR] = v } }
    suspend fun setVerseByVerse(v: Boolean) { appContext.settingsStore.edit { it[K.VERSE_BY_VERSE] = v } }
    suspend fun setAlwaysCreamPage(v: Boolean) { appContext.settingsStore.edit { it[K.ALWAYS_CREAM] = v } }
    suspend fun setAudioAutoAdvance(v: Boolean) { appContext.settingsStore.edit { it[K.AUDIO_AUTO] = v } }
    suspend fun setFollowTranslation(v: Boolean) { appContext.settingsStore.edit { it[K.FOLLOW_TRANS] = v } }
    suspend fun setFollowEn(v: Boolean) { appContext.settingsStore.edit { it[K.FOLLOW_EN] = v } }
    suspend fun setFollowUr(v: Boolean) { appContext.settingsStore.edit { it[K.FOLLOW_UR] = v } }
    suspend fun setVolumeKeysTurnPages(v: Boolean) { appContext.settingsStore.edit { it[K.VOL_TURN] = v } }
    suspend fun setMushafPaged(v: Boolean) { appContext.settingsStore.edit { it[K.MUSHAF_PAGED] = v } }
    suspend fun setBookMode(v: Boolean) { appContext.settingsStore.edit { it[K.BOOK_MODE] = v } }
    suspend fun setBookHintSeen(v: Boolean) { appContext.settingsStore.edit { it[K.BOOK_HINT_SEEN] = v } }
    suspend fun setBookLang(lang: String) { appContext.settingsStore.edit { it[K.BOOK_LANG] = lang } }

    suspend fun enterArabicOnly() {
        appContext.settingsStore.edit { prefs ->
            prefs[K.PREV_EN] = prefs[K.SHOW_EN] ?: true
            prefs[K.PREV_UR] = prefs[K.SHOW_UR] ?: true
            prefs[K.PREV_TR] = prefs[K.SHOW_TR] ?: true
            prefs[K.PREV_VBV] = prefs[K.VERSE_BY_VERSE] ?: true
            prefs[K.SHOW_AR] = true
            prefs[K.SHOW_EN] = false
            prefs[K.SHOW_UR] = false
            prefs[K.SHOW_TR] = false
            prefs[K.VERSE_BY_VERSE] = false
        }
    }

    suspend fun leaveArabicOnly() {
        appContext.settingsStore.edit { prefs ->
            prefs[K.SHOW_AR] = true
            prefs[K.SHOW_EN] = prefs[K.PREV_EN] ?: true
            prefs[K.SHOW_UR] = prefs[K.PREV_UR] ?: true
            prefs[K.SHOW_TR] = prefs[K.PREV_TR] ?: true
            prefs[K.VERSE_BY_VERSE] = prefs[K.PREV_VBV] ?: true
        }
    }

    suspend fun setLastRead(s: Int, v: Int) {
        appContext.settingsStore.edit { it[K.LAST_READ] = "$s:$v" }
    }

    suspend fun disableBookModeIfSet() {
        appContext.settingsStore.edit { prefs ->
            if (prefs[K.BOOK_MODE] == true) {
                prefs[K.BOOK_MODE] = false
            }
        }
    }

    suspend fun seedDefaultsIfNewInstall() {
        appContext.settingsStore.edit { prefs ->
            val ar = prefs[K.FONT_AR]
            if (ar == null || ar !in FONT_SCALE_MIN..FONT_SCALE_MAX) {
                prefs[K.FONT_AR] = FONT_SCALE_DEFAULT
            }
            val en = prefs[K.FONT_EN]
            if (en == null || en !in FONT_SCALE_MIN..FONT_SCALE_MAX) {
                prefs[K.FONT_EN] = FONT_SCALE_DEFAULT
            }
            val ur = prefs[K.FONT_UR]
            if (ur == null || ur !in FONT_SCALE_MIN..FONT_SCALE_MAX) {
                prefs[K.FONT_UR] = FONT_SCALE_DEFAULT
            }
            val tr = prefs[K.FONT_TR]
            if (tr == null || tr !in FONT_SCALE_MIN..FONT_SCALE_MAX) {
                prefs[K.FONT_TR] = FONT_SCALE_DEFAULT
            }
        }
    }

    companion object {
        fun parseLastRead(raw: String?): Pair<Int, Int>? {
            if (raw.isNullOrBlank()) return null
            val parts = raw.split(":")
            if (parts.size != 2) return null
            val s = parts[0].toIntOrNull() ?: return null
            val v = parts[1].toIntOrNull() ?: return null
            if (s !in 1..114 || v < 1) return null
            return Pair(s, v)
        }
    }
}
