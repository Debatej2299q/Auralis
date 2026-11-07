package dev.auralis.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.auralis.MainActivity
import dev.auralis.R

/**
 * Hosts the media session so playback keeps going with the screen off and
 * gets proper notification / lock-screen / headset controls for free.
 *
 * The notification is a real foreground service notification: it shows the
 * artwork, title and artist, and carries previous / play-pause / next
 * actions that work without the app being on screen.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Dedicated, low-importance channel: visible controls, never a sound.
        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.notification_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()
            .apply { setSmallIcon(R.drawable.ic_notification) }
        setMediaNotificationProvider(provider)

        val sharedPlayer = PlayerController.get(this).player
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        mediaSession = MediaSession.Builder(this, sharedPlayer)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * Swiping the app away must not kill the music, but it should not leave an
     * idle foreground service behind either.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.isPlaying) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.player?.pause()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "auralis_playback"
        const val NOTIFICATION_ID = 1001
    }
}
