package org.justquran.app

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.justquran.app.audio.AudioService
import org.justquran.app.data.AppSettings
import org.justquran.app.ui.nav.JustQuranNavHost
import org.justquran.app.ui.theme.JustQuranTheme

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer
    private var volumeKeyConsumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        CrashReporter.install(this)
        super.onCreate(savedInstanceState)
        container = AppContainer(this)
        handleAudioIntent(intent)
        enableEdgeToEdge()
        setContent {
            val settings by container.settingsRepository.settings.collectAsState(initial = AppSettings())
            JustQuranTheme(themeChoice = settings.theme) {
                JustQuranNavHost(container = container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAudioIntent(intent)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!::container.isInitialized || !isVolumeKey) {
            return super.dispatchKeyEvent(event)
        }
        val action = event.action
        if (action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount > 0) {
                return volumeKeyConsumed
            }
            val handler = container.volumeKeyBus.handler
            val consumed = handler?.invoke(event.keyCode) == true
            volumeKeyConsumed = consumed
            if (consumed) return true
            return super.dispatchKeyEvent(event)
        }
        if (action == KeyEvent.ACTION_UP && volumeKeyConsumed) {
            volumeKeyConsumed = false
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun handleAudioIntent(intent: Intent?) {
        if (intent == null) return
        val s = intent.getIntExtra(AudioService.EXTRA_SURAH, -1)
        val v = intent.getIntExtra(AudioService.EXTRA_VERSE, -1)
        if (s in 1..114) {
            container.audioController.requestOpenVerse(s, if (v < 1) 1 else v)
            intent.removeExtra(AudioService.EXTRA_SURAH)
            intent.removeExtra(AudioService.EXTRA_VERSE)
        }
    }
}
