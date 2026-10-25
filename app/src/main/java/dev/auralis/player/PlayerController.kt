package dev.auralis.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dev.auralis.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * One ExoPlayer for the whole process, shared by the UI and the
 * MediaSessionService that owns the notification / lock-screen controls.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlayerController private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tuner = AudioTuner()

    val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _current = MutableStateFlow<Track?>(null)
    val current: StateFlow<Track?> = _current.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _sleepRemaining = MutableStateFlow(0L)
    val sleepRemaining: StateFlow<Long> = _sleepRemaining.asStateFlow()

    private val _trackStarted = MutableSharedFlow<Track>(extraBufferCapacity = 4)
    val trackStarted: SharedFlow<Track> = _trackStarted.asSharedFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var sleepJob: Job? = null

    init {
        player.setWakeMode(C.WAKE_MODE_LOCAL)
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) ensureService()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    val d = player.duration
                    if (d > 0L) _duration.value = d
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncCurrent()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffle.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                tuner.attach(audioSessionId)
            }

            override fun onPlayerError(error: PlaybackException) {
                _messages.tryEmit("Can't play this track: ${error.errorCodeName}")
            }
        })

        tuner.attach(player.audioSessionId)

        scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    _position.value = player.currentPosition.coerceAtLeast(0L)
                    val d = player.duration
                    if (d > 0L) _duration.value = d
                }
                delay(450L)
            }
        }
    }

    private fun syncCurrent() {
        val index = player.currentMediaItemIndex
        val track = _queue.value.getOrNull(index)
        _current.value = track
        _position.value = 0L
        _duration.value = track?.durationMs?.takeIf { it > 0L } ?: 0L
        if (track != null) _trackStarted.tryEmit(track)
    }

    fun play(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = _shuffle.value) {
        if (tracks.isEmpty()) return
        _queue.value = tracks
        val items = tracks.map { it.toMediaItem() }
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        player.setMediaItems(items, safeIndex, 0L)
        player.shuffleModeEnabled = shuffle
        player.prepare()
        player.play()
        _current.value = tracks[safeIndex]
        _position.value = 0L
        _duration.value = tracks[safeIndex].durationMs
        ensureService()
    }

    fun playSingle(track: Track) = play(listOf(track), 0, shuffle = false)

    fun toggle() {
        if (player.mediaItemCount == 0) return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
            ensureService()
        }
    }

    fun pause() {
        if (player.isPlaying) player.pause()
    }

    fun next() {
        if (player.mediaItemCount == 0) return
        player.seekToNextMediaItem()
    }

    fun previous() {
        if (player.mediaItemCount == 0) return
        if (player.currentPosition > 4_000L) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        _position.value = positionMs.coerceAtLeast(0L)
    }

    fun seekToFraction(fraction: Float) {
        val total = player.duration.takeIf { it > 0L } ?: _current.value?.durationMs ?: return
        seekTo((total * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun jumpTo(index: Int) {
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0L)
            player.play()
        }
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(value: Float) {
        val safe = value.coerceIn(0.5f, 2.0f)
        player.setPlaybackSpeed(safe)
        _speed.value = safe
    }

    fun applySound(profile: AudioProfile, speed: Float) {
        tuner.apply(profile)
        setSpeed(speed)
    }

    fun moveQueueItem(from: Int, to: Int) {
        if (from == to) return
        if (from !in _queue.value.indices || to !in _queue.value.indices) return
        val updated = _queue.value.toMutableList()
        updated.add(to, updated.removeAt(from))
        _queue.value = updated
        player.moveMediaItem(from, to)
    }

    fun removeQueueItem(index: Int) {
        if (index !in _queue.value.indices) return
        val updated = _queue.value.toMutableList()
        updated.removeAt(index)
        _queue.value = updated
        player.removeMediaItem(index)
        if (updated.isEmpty()) {
            _current.value = null
            _isPlaying.value = false
            _position.value = 0L
        }
    }

    fun sleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepJob = null
        player.volume = 1f
        if (minutes <= 0) {
            _sleepRemaining.value = 0L
            return
        }
        _sleepRemaining.value = minutes * 60L
        sleepJob = scope.launch {
            var remaining = minutes * 60L
            while (remaining > 0L) {
                delay(1000L)
                remaining -= 1L
                _sleepRemaining.value = remaining
            }
            // gentle 8 second fade so it never jars
            val steps = 40
            for (step in 1..steps) {
                player.volume = 1f - (step.toFloat() / steps.toFloat())
                delay(200L)
            }
            player.pause()
            player.volume = 1f
            _sleepRemaining.value = 0L
        }
    }

    fun stop() {
        player.pause()
        player.clearMediaItems()
        _queue.value = emptyList()
        _current.value = null
        _position.value = 0L
        _duration.value = 0L
    }

    fun ensureService() {
        // Idempotent: calling startService on a running service only delivers
        // another onStartCommand. Doing it every time means playback still
        // restores its notification after the service was stopped by the system.
        try {
            appContext.startService(Intent(appContext, PlaybackService::class.java))
        } catch (_: Throwable) {
            // Background-start restrictions; the player itself keeps working.
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val art = artworkUri
        val artUri = when {
            art.isNullOrBlank() -> null
            art.startsWith("http") || art.startsWith("content://") || art.startsWith("file://") -> Uri.parse(art)
            else -> Uri.fromFile(java.io.File(art))
        }
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artUri)
                    .build(),
            )
            .build()
    }

    companion object {
        val REPEAT_OFF: Int = Player.REPEAT_MODE_OFF
        val REPEAT_ALL: Int = Player.REPEAT_MODE_ALL
        val REPEAT_ONE: Int = Player.REPEAT_MODE_ONE

        @Volatile
        private var instance: PlayerController? = null

        fun get(context: Context): PlayerController =
            instance ?: synchronized(this) {
                instance ?: PlayerController(context.applicationContext).also { instance = it }
            }
    }
}
