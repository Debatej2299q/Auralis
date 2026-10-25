package dev.auralis.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Done(val found: Int, val added: Int) : ScanState
    data class Failed(val message: String) : ScanState
}

class MusicRepository(
    private val context: Context,
    private val database: MusicDatabase,
) {

    val dao: MusicDao = database.dao()

    val tracks: Flow<List<Track>> = dao.observeTracks()
    val likedTracks: Flow<List<Track>> = dao.observeLiked()
    val recentlyAdded: Flow<List<Track>> = dao.observeRecentlyAdded()
    val mostPlayed: Flow<List<Track>> = dao.observeMostPlayed()
    val downloadTracks: Flow<List<Track>> = dao.observeDownloads()
    val playlists: Flow<List<PlaylistSummary>> = dao.observePlaylists()
    val trackCount: Flow<Int> = dao.observeTrackCount()
    val downloadBytes: Flow<Long> = dao.observeDownloadBytes()

    val stats: Flow<ListenStats> = combine(
        dao.observePlayCount(),
        dao.observeUniqueTracks(),
        dao.observeListenedMs(),
    ) { plays, unique, listened ->
        ListenStats(plays = plays, uniqueTracks = unique, listenedMs = listened)
    }

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    /** Where yt-dlp drops the audio it downloads. App-private, zero permissions needed. */
    val downloadsDir: File
        get() {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            return File(base, "Auralis").apply { mkdirs() }
        }

    private val artworkDir: File
        get() = File(context.cacheDir, "artwork").apply { mkdirs() }

    suspend fun importDeviceAudio(): Int = withContext(Dispatchers.IO) {
        _scanState.value = ScanState.Scanning
        try {
            val found = LocalLibraryScanner(context).scan()
            if (found.isEmpty()) {
                _scanState.value = ScanState.Done(0, 0)
                return@withContext 0
            }
            val ids = dao.insertTracks(found)
            val added = ids.count { it > 0L }
            _scanState.value = ScanState.Done(found.size, added)
            added
        } catch (t: Throwable) {
            _scanState.value = ScanState.Failed(t.message ?: "Could not read your music")
            0
        }
    }

    suspend fun addDownloaded(track: Track): Long = withContext(Dispatchers.IO) {
        val existing = dao.findTrackByUri(track.uri)
        if (existing != null) {
            dao.updateTrack(
                existing.copy(
                    artworkUri = track.artworkUri ?: existing.artworkUri,
                    sizeBytes = track.sizeBytes,
                    filePath = track.filePath,
                )
            )
            existing.id
        } else {
            val id = dao.insertTracks(listOf(track)).firstOrNull() ?: -1L
            if (id > 0L) id else dao.findTrackByUri(track.uri)?.id ?: -1L
        }
    }

    suspend fun toggleFavorite(track: Track) {
        dao.setFavorite(track.id, !track.favorite)
    }

    suspend fun recordPlay(track: Track) {
        dao.bumpPlayCount(track.id)
        dao.insertHistory(HistoryEntity(trackId = track.id))
    }

    /**
     * Best artwork we can find. Downloaded songs already ship a jpg next to
     * the audio; local songs get their embedded cover extracted once and cached.
     */
    suspend fun artworkFor(track: Track): String? = withContext(Dispatchers.IO) {
        val cached = track.artworkUri
        if (!cached.isNullOrBlank() && artworkExists(cached)) return@withContext cached

        val file = File(artworkDir, "${track.id}.jpg")
        if (file.exists() && file.length() > 1024L) {
            return@withContext Uri.fromFile(file).toString()
        }

        val bytes = extractEmbeddedArtwork(track) ?: return@withContext cached
        try {
            file.writeBytes(bytes)
            val uri = Uri.fromFile(file).toString()
            dao.setArtwork(track.id, uri)
            uri
        } catch (_: Throwable) {
            cached
        }
    }

    private fun artworkExists(uriString: String): Boolean = when {
        uriString.startsWith("content://") -> true
        uriString.startsWith("file://") -> File(Uri.parse(uriString).path ?: "").exists()
        uriString.startsWith("http") -> true
        else -> File(uriString).exists()
    }

    private fun extractEmbeddedArtwork(track: Track): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            val path = track.filePath
            if (!path.isNullOrBlank() && File(path).exists()) {
                retriever.setDataSource(path)
            } else {
                retriever.setDataSource(context, Uri.parse(track.uri))
            }
            retriever.embeddedPicture
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }

    suspend fun deleteTrack(track: Track) {
        if (track.isDownload) {
            try {
                track.filePath?.let { File(it).delete() }
            } catch (_: Throwable) {
            }
        }
        dao.deleteTrack(track.id)
    }

    suspend fun createPlaylist(name: String): Long = dao.insertPlaylist(PlaylistEntity(name = name.trim().ifBlank { "New playlist" }))

    suspend fun addToPlaylist(playlistId: Long, trackId: Long): Boolean =
        dao.appendToPlaylist(playlistId, trackId)

    suspend fun renamePlaylist(id: Long, name: String) =
        dao.renamePlaylist(id, name.trim().ifBlank { "Untitled playlist" })

    suspend fun deletePlaylist(id: Long) = dao.deletePlaylistEverywhere(id)

    suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) =
        dao.removeFromPlaylist(playlistId, trackId)

    suspend fun reorderPlaylist(playlistId: Long, orderedIds: List<Long>) =
        dao.reorderPlaylist(playlistId, orderedIds)

    /** Reads a sidecar .lrc file that sits next to the audio file. */
    suspend fun lyricsFor(track: Track): String? = withContext(Dispatchers.IO) {
        val path = track.filePath ?: runCatching { Uri.parse(track.uri).path }.getOrNull()
        if (path.isNullOrBlank()) return@withContext null
        val audio = File(path)
        val base = audio.nameWithoutExtension
        val folder = audio.parentFile ?: return@withContext null
        val candidates = listOf(
            File(folder, "$base.lrc"),
            File(folder, "$base.LRC"),
            File(folder, "${audio.name}.lrc"),
        )
        candidates.firstOrNull { it.exists() }?.let { file ->
            runCatching { file.readText() }.getOrNull()
        }
    }

    suspend fun clearHistory() = dao.clearHistory()
}
