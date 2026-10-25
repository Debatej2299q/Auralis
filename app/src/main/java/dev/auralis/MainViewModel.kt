package dev.auralis

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.auralis.core.AppSettings
import dev.auralis.core.estimateBytes
import dev.auralis.data.ListenStats
import dev.auralis.data.PlaylistSummary
import dev.auralis.data.ScanState
import dev.auralis.data.Track
import dev.auralis.online.AudioFormat
import dev.auralis.online.AudioQuality
import dev.auralis.online.DownloadStatus
import dev.auralis.online.DownloadTask
import dev.auralis.online.EngineState
import dev.auralis.online.YtResult
import dev.auralis.player.AudioProfile
import dev.auralis.player.PlayerController
import dev.auralis.ui.theme.Mood
import dev.auralis.ui.theme.MoodCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class LibraryState(
    val tracks: List<Track> = emptyList(),
    val liked: List<Track> = emptyList(),
    val recent: List<Track> = emptyList(),
    val mostPlayed: List<Track> = emptyList(),
    val downloads: List<Track> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val stats: ListenStats = ListenStats(),
    val scan: ScanState = ScanState.Idle,
    val downloadBytes: Long = 0L,
    val permission: Boolean = false,
    val loading: Boolean = true,
) {
    val isEmpty: Boolean get() = tracks.isEmpty() && !loading
}

data class PlayerUiState(
    val track: Track? = null,
    val queue: List<Track> = emptyList(),
    val artwork: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = 0,
    val speed: Float = 1f,
    val sleepRemaining: Long = 0L,
) {
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

data class DiscoverState(
    val online: Boolean = true,
    val engine: EngineState = EngineState.Idle,
    val searching: Boolean = false,
    val results: List<YtResult> = emptyList(),
    val error: String? = null,
    val preview: YtResult? = null,
    val previewBusy: Boolean = false,
    val engineBusy: Boolean = false,
    val tasks: List<DownloadTask> = emptyList(),
) {
    val activeDownloads: Int
        get() = tasks.count {
            it.status == DownloadStatus.Queued ||
                it.status == DownloadStatus.Preparing ||
                it.status == DownloadStatus.Downloading ||
                it.status == DownloadStatus.Converting
        }
}

private data class SearchUi(
    val searching: Boolean = false,
    val results: List<YtResult> = emptyList(),
    val error: String? = null,
    val preview: YtResult? = null,
    val previewBusy: Boolean = false,
    val engineBusy: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val graph = app as AuralisApp
    private val repo = graph.repository
    private val prefs = graph.preferences
    private val controller: PlayerController = graph.player
    private val engine = graph.ytEngine
    private val downloader = graph.downloads
    private val network = graph.network

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // ------------------------------------------------------------- library

    private val _library = MutableStateFlow(
        LibraryState(permission = hasAudioPermission()),
    )
    val library: StateFlow<LibraryState> = _library.asStateFlow()

    // -------------------------------------------------------------- player

    private val _artwork = MutableStateFlow<String?>(null)

    val playerState: StateFlow<PlayerUiState> = combine(
        combine(controller.current, controller.queue, _artwork) { track, queue, art ->
            Triple(track, queue, art)
        },
        combine(controller.isPlaying, controller.isBuffering, controller.position) { playing, buffering, position ->
            Triple(playing, buffering, position)
        },
        combine(controller.duration, controller.shuffle, controller.repeatMode) { duration, shuffle, repeat ->
            Triple(duration, shuffle, repeat)
        },
        combine(controller.speed, controller.sleepRemaining) { speed, sleep ->
            Pair(speed, sleep)
        },
    ) { media, playback, modes, extras ->
        PlayerUiState(
            track = media.first,
            queue = media.second,
            artwork = media.third,
            isPlaying = playback.first,
            isBuffering = playback.second,
            positionMs = playback.third,
            durationMs = modes.first.takeIf { it > 0L } ?: (media.first?.durationMs ?: 0L),
            shuffle = modes.second,
            repeatMode = modes.third,
            speed = extras.first,
            sleepRemaining = extras.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    // -------------------------------------------------------------- settings

    val settings: StateFlow<AppSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val mood: StateFlow<Mood> = settings
        .map { MoodCatalog.byId(it.moodId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MoodCatalog.midnight)

    // --------------------------------------------------------------- online

    private val _search = MutableStateFlow(SearchUi())

    val discover: StateFlow<DiscoverState> = combine(
        network.online,
        engine.state,
        downloader.tasks,
        _search,
    ) { online, engineState, tasks, search ->
        DiscoverState(
            online = online,
            engine = engineState,
            searching = search.searching,
            results = search.results,
            error = search.error,
            preview = search.preview,
            previewBusy = search.previewBusy,
            engineBusy = search.engineBusy,
            tasks = tasks,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverState())

    init {
        // library
        viewModelScope.launch {
            repo.tracks.collect { list ->
                _library.update { it.copy(tracks = list, loading = false) }
            }
        }
        viewModelScope.launch {
            repo.likedTracks.collect { list -> _library.update { it.copy(liked = list) } }
        }
        viewModelScope.launch {
            repo.recentlyAdded.collect { list -> _library.update { it.copy(recent = list) } }
        }
        viewModelScope.launch {
            repo.mostPlayed.collect { list -> _library.update { it.copy(mostPlayed = list) } }
        }
        viewModelScope.launch {
            repo.downloadTracks.collect { list -> _library.update { it.copy(downloads = list) } }
        }
        viewModelScope.launch {
            repo.playlists.collect { list -> _library.update { it.copy(playlists = list) } }
        }
        viewModelScope.launch {
            repo.stats.collect { stats -> _library.update { it.copy(stats = stats) } }
        }
        viewModelScope.launch {
            repo.scanState.collect { scan -> _library.update { it.copy(scan = scan) } }
        }
        viewModelScope.launch {
            repo.downloadBytes.collect { bytes -> _library.update { it.copy(downloadBytes = bytes) } }
        }

        // cover art for whatever is playing
        viewModelScope.launch {
            controller.current.collect { track ->
                _artwork.value = if (track == null) null else repo.artworkFor(track)
            }
        }

        // listening history
        viewModelScope.launch {
            controller.trackStarted.collect { track ->
                repo.recordPlay(track)
            }
        }

        viewModelScope.launch {
            controller.messages.collect { _messages.tryEmit(it) }
        }
        viewModelScope.launch {
            downloader.messages.collect { _messages.tryEmit(it) }
        }

        // mood = look + sound
        viewModelScope.launch {
            settings
                .map { it.moodId to it.autoTune }
                .distinctUntilChanged()
                .collect { (moodId, autoTune) ->
                    val selected = MoodCatalog.byId(moodId)
                    controller.applySound(
                        if (autoTune) selected.profile else AudioProfile.Flat,
                        if (autoTune) selected.speed else 1f,
                    )
                }
        }

        if (hasAudioPermission()) {
            importLibrary(silent = true)
        }
    }

    // ---------------------------------------------------------- permissions

    fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            getApplication<Application>(),
            permission,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requiredPermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            list += Manifest.permission.READ_MEDIA_AUDIO
            list += Manifest.permission.POST_NOTIFICATIONS
        } else {
            list += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return list.toTypedArray()
    }

    fun onPermissionResult() {
        _library.update { it.copy(permission = hasAudioPermission()) }
        if (hasAudioPermission()) importLibrary(silent = false)
    }

    fun importLibrary(silent: Boolean = false) {
        if (!hasAudioPermission()) {
            _library.update { it.copy(permission = false) }
            return
        }
        viewModelScope.launch {
            repo.importDeviceAudio()
            if (!silent) {
                when (val scan = repo.scanState.value) {
                    is ScanState.Done -> {
                        val message = when {
                            scan.found == 0 -> "No music files found on this device yet"
                            scan.added == 0 -> "Library is up to date - ${scan.found} songs"
                            else -> "${scan.added} new songs added to your library"
                        }
                        _messages.tryEmit(message)
                    }
                    is ScanState.Failed -> _messages.tryEmit(scan.message)
                    else -> Unit
                }
            }
            prefs.setLibraryReady(true)
        }
    }

    // --------------------------------------------------------------- player

    fun playTrack(track: Track, context: List<Track> = emptyList()) {
        val list = when {
            context.isNotEmpty() -> context
            _library.value.tracks.isNotEmpty() -> _library.value.tracks
            else -> listOf(track)
        }
        val index = list.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } ?: 0
        controller.play(list, index)
    }

    fun playAll(tracks: List<Track>, shuffle: Boolean = false) {
        if (tracks.isEmpty()) {
            _messages.tryEmit("Nothing to play yet")
            return
        }
        val start = if (shuffle) Random.nextInt(tracks.size) else 0
        controller.play(tracks, start, shuffle)
    }

    fun togglePlay() = controller.toggle()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekToFraction(fraction: Float) = controller.seekToFraction(fraction)
    fun toggleShuffle() = controller.toggleShuffle()
    fun cycleRepeat() = controller.cycleRepeat()
    fun setSpeed(speed: Float) = controller.setSpeed(speed)
    fun setSleepTimer(minutes: Int) = controller.sleepTimer(minutes)
    fun jumpTo(index: Int) = controller.jumpTo(index)
    fun moveQueueItem(from: Int, to: Int) = controller.moveQueueItem(from, to)
    fun removeQueueItem(index: Int) = controller.removeQueueItem(index)
    fun stopPlayback() = controller.stop()

    fun isFavorite(trackId: Long): Boolean =
        _library.value.tracks.firstOrNull { it.id == trackId }?.favorite == true

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repo.toggleFavorite(track)
            _messages.tryEmit(if (track.favorite) "Removed from liked songs" else "Added to liked songs")
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            repo.deleteTrack(track)
            _messages.tryEmit("\"${track.title}\" removed")
        }
    }

    fun applyManualTune(
        speed: Float,
        bassMb: Int,
        midMb: Int,
        trebleMb: Int,
        spatial: Int,
        reverb: Int,
    ) {
        controller.applySound(
            AudioProfile(
                bassMb = bassMb.toShort(),
                midMb = midMb.toShort(),
                trebleMb = trebleMb.toShort(),
                spatial = spatial,
                reverb = reverb,
            ),
            speed,
        )
    }

    // ------------------------------------------------------------ playlists

    fun playlistTracks(id: Long): Flow<List<Track>> = repo.dao.observePlaylistTracks(id)

    fun playlistSummary(id: Long): Flow<PlaylistSummary?> = repo.dao.observePlaylistSummary(id)

    fun createPlaylist(name: String, addTrackId: Long? = null) {
        viewModelScope.launch {
            val id = repo.createPlaylist(name)
            if (addTrackId != null) repo.addToPlaylist(id, addTrackId)
            _messages.tryEmit("Playlist created")
        }
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            val added = repo.addToPlaylist(playlistId, trackId)
            _messages.tryEmit(if (added) "Added to playlist" else "Already in that playlist")
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { repo.renamePlaylist(id, name) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repo.deletePlaylist(id)
            _messages.tryEmit("Playlist deleted")
        }
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch { repo.removeFromPlaylist(playlistId, trackId) }
    }

    fun reorderPlaylist(playlistId: Long, orderedIds: List<Long>) {
        viewModelScope.launch { repo.reorderPlaylist(playlistId, orderedIds) }
    }

    // ---------------------------------------------------------------- online

    fun refreshNetwork() {
        network.refresh()
    }

    fun searchOnline(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        _search.update { it.copy(searching = true, error = null, preview = null) }
        viewModelScope.launch {
            try {
                val results = engine.search(clean, limit = 18)
                _search.update {
                    it.copy(
                        searching = false,
                        results = results,
                        error = if (results.isEmpty()) "No results for \"$clean\"" else null,
                    )
                }
            } catch (t: Throwable) {
                _search.update {
                    it.copy(
                        searching = false,
                        error = t.message?.take(200) ?: "Search failed",
                    )
                }
            }
        }
    }

    fun lookupUrl(url: String) {
        val clean = url.trim()
        if (clean.isEmpty()) {
            _search.update { it.copy(error = "Paste a link first") }
            return
        }
        if (!network.refresh()) {
            _search.update { it.copy(error = "You're offline - connect to look up a link") }
            return
        }
        _search.update { it.copy(previewBusy = true, error = null) }
        viewModelScope.launch {
            try {
                val result = engine.lookup(clean)
                _search.update { it.copy(previewBusy = false, preview = result) }
            } catch (t: Throwable) {
                _search.update {
                    it.copy(
                        previewBusy = false,
                        error = t.message?.take(200) ?: "Could not read that link",
                    )
                }
            }
        }
    }

    fun clearPreview() {
        _search.update { it.copy(preview = null) }
    }

    fun openPreview(result: YtResult) {
        _search.update { it.copy(preview = result) }
    }

    fun clearSearch() {
        _search.update { it.copy(results = emptyList(), error = null) }
    }

    fun download(result: YtResult, format: AudioFormat, quality: AudioQuality) {
        if (downloader.enqueue(result, format, quality.kbps)) {
            _search.update { it.copy(preview = null) }
            _messages.tryEmit("Downloading \"${result.title}\"")
        }
    }

    fun cancelDownload(id: String) = downloader.cancel(id)

    fun dismissDownload(id: String) = downloader.dismiss(id)

    fun clearFinishedDownloads() = downloader.clearFinished()

    fun updateEngine() {
        _search.update { it.copy(engineBusy = true) }
        viewModelScope.launch {
            val result = engine.updateEngine()
            result.onSuccess { _messages.tryEmit(it) }
                .onFailure { _messages.tryEmit(it.message ?: "yt-dlp update failed") }
            _search.update { it.copy(engineBusy = false) }
        }
    }

    fun estimatedSize(durationMs: Long, kbps: Int): Long = estimateBytes(durationMs, kbps)

    // -------------------------------------------------------------- settings

    fun setMood(id: String) {
        viewModelScope.launch { prefs.setMood(id) }
    }

    fun setAutoTune(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoTune(enabled) }
    }

    fun setDownloadFormat(id: String) {
        viewModelScope.launch { prefs.setDownloadFormat(id) }
    }

    fun setDownloadQuality(id: String) {
        viewModelScope.launch { prefs.setDownloadQuality(id) }
    }

    fun setAmbient(enabled: Boolean) {
        viewModelScope.launch { prefs.setAmbient(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repo.clearHistory()
            _messages.tryEmit("Listening history cleared")
        }
    }

    fun clearArtworkCache() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                java.io.File(getApplication<Application>().cacheDir, "artwork").deleteRecursively()
            }
            _messages.tryEmit("Artwork cache cleared")
        }
    }

    suspend fun lyricsFor(track: Track): String? = repo.lyricsFor(track)
}
