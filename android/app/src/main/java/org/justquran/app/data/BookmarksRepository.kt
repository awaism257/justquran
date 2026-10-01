package org.justquran.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bookmarksStore by preferencesDataStore(name = "bookmarks")

class BookmarksRepository(private val appContext: Context) {
    private val KEY = stringSetPreferencesKey("bookmarks")

    val bookmarks: Flow<Set<String>> = appContext.bookmarksStore.data.map { prefs ->
        prefs[KEY] ?: emptySet()
    }

    suspend fun toggle(s: Int, v: Int) {
        val key = "$s:$v"
        appContext.bookmarksStore.edit { prefs ->
            val cur = prefs[KEY] ?: emptySet()
            if (cur.contains(key)) {
                prefs[KEY] = cur - key
            } else {
                prefs[KEY] = cur + key
            }
        }
    }

    suspend fun add(s: Int, v: Int) {
        val key = "$s:$v"
        appContext.bookmarksStore.edit { prefs ->
            val cur = prefs[KEY] ?: emptySet()
            prefs[KEY] = cur + key
        }
    }

    suspend fun remove(id: String) {
        appContext.bookmarksStore.edit { prefs ->
            val cur = prefs[KEY] ?: emptySet()
            prefs[KEY] = cur - id
        }
    }
}
