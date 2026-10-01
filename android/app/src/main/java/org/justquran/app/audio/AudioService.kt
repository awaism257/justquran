package org.justquran.app.audio

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.justquran.app.MainActivity

class AudioService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build()

            val player = ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build()

            mediaSession = MediaSession.Builder(this, player).build()
            player.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateSessionActivity(mediaItem?.mediaId)
                }
            })
            updateSessionActivity(null)
        } catch (_: Throwable) {
            mediaSession = null
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun updateSessionActivity(mediaId: String?) {
        try {
            val intent = Intent(this, MainActivity::class.java)
            val pair = mediaIdToVerse(mediaId)
            if (pair != null) {
                intent.putExtra(EXTRA_SURAH, pair.first)
                intent.putExtra(EXTRA_VERSE, pair.second)
            }
            val activity = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            mediaSession?.setSessionActivity(activity)
        } catch (_: Throwable) {
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            player?.stop()
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SURAH: String = "org.justquran.app.extra.SURAH"
        const val EXTRA_VERSE: String = "org.justquran.app.extra.VERSE"

        fun mediaIdToVerse(mediaId: String?): Pair<Int, Int>? {
            if (mediaId == null) return null
            val parts = mediaId.split(":")
            if ((parts[0] == "verse" || parts[0] == "narr") && parts.size >= 3) {
                val s = (if (parts.size == 4) parts[2] else parts[1]).toIntOrNull() ?: return null
                val v = (if (parts.size == 4) parts[3] else parts[2]).toIntOrNull() ?: return null
                return Pair(s, v)
            }
            if (parts[0] == "prelude" && parts.size >= 2) {
                val s = parts.last().toIntOrNull() ?: return null
                return Pair(s, 1)
            }
            return null
        }
    }
}
