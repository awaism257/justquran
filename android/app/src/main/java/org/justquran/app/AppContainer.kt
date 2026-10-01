package org.justquran.app

import android.content.Context
import org.justquran.app.audio.AudioController
import org.justquran.app.data.AudioDownloadRepository
import org.justquran.app.data.BookmarksRepository
import org.justquran.app.data.BundleRepository
import org.justquran.app.data.SettingsRepository

class VolumeKeyBus {
    var handler: ((keyCode: Int) -> Boolean)? = null
}

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val bundleRepository = BundleRepository(appContext)
    val settingsRepository = SettingsRepository(appContext)
    val bookmarksRepository = BookmarksRepository(appContext)
    val audioDownloadRepository = AudioDownloadRepository(appContext)
    val bundleRepo get() = bundleRepository
    val settingsRepo get() = settingsRepository
    val bookmarksRepo get() = bookmarksRepository
    val audioDownloadRepo get() = audioDownloadRepository
    val audioController = AudioController(
        appContext,
        bundleRepository,
        settingsRepository,
        audioDownloadRepository
    )
    val volumeKeyBus = VolumeKeyBus()
}
