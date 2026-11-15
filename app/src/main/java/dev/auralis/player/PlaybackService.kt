package dev.auralis.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.auralis.MainActivity
import dev.auralis.R

/**
 * Hosts the media session so playback keeps going with the screen off and
 * gets proper notification / lock-screen / headset controls.
 *
 * Auralis drives its single ExoPlayer directly instead of talking to the
 * session through a [androidx.media3.session.MediaController]. Because of
 * that, [onGetSession] is never called by a client and media3 would not know
 * the session exists - which means no notification and no media panel in the
 * shade. Registering the session with [addSession] is what enables both.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val mainHandler = Handler(Looper.getMainLooper())

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
        val session = MediaSession.Builder(this, sharedPlayer)
            .setSessionActivity(sessionActivity)
            .build()
        mediaSession = session
        addSession(session)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        // media3 connects its internal notification controller asynchronously.
        // If playback was already running when the service came up, no player
        // event is left to trigger the notification, so nudge it once.
        val session = mediaSession
        if (session != null) {
            mainHandler.postDelayed(
                {
                    if (mediaSession === session) refresh(session)
                },
                NOTIFICATION_KICK_DELAY_MS,
            )
        }
        return result
    }

    private fun refresh(session: MediaSession) {
        if (session.player.mediaItemCount == 0) return
        runCatching {
            onUpdateNotification(session, session.player.playWhenReady)
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        mediaSession?.let { session ->
            runCatching { removeSession(session) }
            session.player.pause()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "auralis_playback"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_KICK_DELAY_MS = 700L
    }
}
