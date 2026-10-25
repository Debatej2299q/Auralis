package dev.auralis.online

import android.content.Context
import android.net.Uri
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dev.auralis.core.NetworkMonitor
import dev.auralis.core.formatBytes
import dev.auralis.data.MusicRepository
import dev.auralis.data.SOURCE_DOWNLOAD
import dev.auralis.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

enum class AudioFormat(
    val id: String,
    val label: String,
    val ext: String,
    val blurb: String,
) {
    M4A("m4a", "M4A", "m4a", "AAC · best all-rounder"),
    MP3("mp3", "MP3", "mp3", "Plays with everything"),
    OPUS("opus", "OPUS", "opus", "Tiny files, modern"),
    FLAC("flac", "FLAC", "flac", "Lossless · large files");

    companion object {
        fun from(id: String): AudioFormat = entries.firstOrNull { it.id == id } ?: M4A
    }
}

data class AudioQuality(val id: String, val label: String, val kbps: Int) {
    companion object {
        val options = listOf(
            AudioQuality("0", "Best available", 0),
            AudioQuality("320", "320 kbps", 320),
            AudioQuality("256", "256 kbps", 256),
            AudioQuality("192", "192 kbps", 192),
            AudioQuality("128", "128 kbps", 128),
        )

        fun from(id: String): AudioQuality = options.firstOrNull { it.id == id } ?: options.first()
    }
}

enum class DownloadStatus { Queued, Preparing, Downloading, Converting, Done, Failed, Cancelled }

data class DownloadTask(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
    val url: String,
    val format: AudioFormat,
    val kbps: Int,
    val durationMs: Long = 0L,
    val progress: Float = 0f,
    val etaSeconds: Long = -1L,
    val status: DownloadStatus = DownloadStatus.Queued,
    val message: String? = null,
    val trackId: Long? = null,
    val bytes: Long = 0L,
)

/**
 * Sequential download queue driven by the local yt-dlp process.
 * Downloads land in the app's private Music folder, so no storage
 * permissions are ever required.
 */
class DownloadManager(
    context: Context,
    private val repository: MusicRepository,
    private val engine: YtEngine,
    private val network: NetworkMonitor,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Mutex()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun enqueue(result: YtResult, format: AudioFormat, kbps: Int): Boolean {
        if (!network.isConnected()) {
            _messages.tryEmit("You're offline - downloads need a connection")
            return false
        }
        val task = DownloadTask(
            id = UUID.randomUUID().toString(),
            title = result.title,
            artist = result.artist,
            thumbnail = result.thumbnail,
            url = result.url,
            format = format,
            kbps = kbps,
            durationMs = result.durationMs,
        )
        _tasks.update { it + task }
        scope.launch { runTask(task.id) }
        return true
    }

    fun cancel(id: String) {
        update(id) { it.copy(status = DownloadStatus.Cancelled, message = "Cancelled") }
        runCatching { YoutubeDL.getInstance().destroyProcessById(id) }
    }

    fun dismiss(id: String) {
        val task = _tasks.value.firstOrNull { it.id == id } ?: return
        if (task.status == DownloadStatus.Downloading || task.status == DownloadStatus.Converting) {
            cancel(id)
        }
        _tasks.update { list -> list.filterNot { it.id == id } }
    }

    fun clearFinished() {
        _tasks.update { list ->
            list.filterNot {
                it.status == DownloadStatus.Done ||
                    it.status == DownloadStatus.Failed ||
                    it.status == DownloadStatus.Cancelled
            }
        }
    }

    private fun update(id: String, block: (DownloadTask) -> DownloadTask) {
        _tasks.update { list -> list.map { if (it.id == id) block(it) else it } }
    }

    private fun snapshot(id: String): DownloadTask? = _tasks.value.firstOrNull { it.id == id }

    private suspend fun runTask(taskId: String) {
        gate.withLock {
            val start = snapshot(taskId) ?: return@withLock
            if (start.status == DownloadStatus.Cancelled) return@withLock
            if (!engine.ensureReady()) {
                update(taskId) {
                    it.copy(status = DownloadStatus.Failed, message = engine.failureMessage())
                }
                return@withLock
            }

            update(taskId) { it.copy(status = DownloadStatus.Preparing, progress = 0.01f) }

            val info = runCatching { engine.lookup(start.url) }.getOrNull()
            val title = info?.title ?: start.title
            val artist = info?.artist ?: start.artist
            val duration = info?.durationMs?.takeIf { it > 0L } ?: start.durationMs
            val thumbUrl = info?.thumbnail ?: start.thumbnail

            update(taskId) {
                it.copy(
                    title = title,
                    artist = artist,
                    durationMs = duration,
                    thumbnail = thumbUrl,
                    status = DownloadStatus.Downloading,
                    progress = 0.02f,
                )
            }

            val videoId = videoIdOf(start.url)
            val dir = repository.downloadsDir
            val request = YoutubeDLRequest(start.url).apply {
                addOption("-f", "bestaudio/best")
                addOption("-x")
                addOption("--audio-format", start.format.ext)
                addOption("--audio-quality", if (start.kbps <= 0) "0" else "${start.kbps}K")
                addOption("-o", File(dir, "$videoId.%(ext)s").absolutePath)
                addOption("--write-thumbnail")
                addOption("--convert-thumbnails", "jpg")
                addOption("--no-playlist")
                addOption("--no-mtime")
                addOption("--newline")
                addOption("--no-warnings")
                addOption("--socket-timeout", "20")
                if (start.format == AudioFormat.M4A || start.format == AudioFormat.MP3) {
                    addOption("--embed-metadata")
                }
            }

            try {
                YoutubeDL.getInstance().execute(request, taskId) { progress, eta, line ->
                    val stage = when {
                        line.contains("[ExtractAudio]") || line.contains("[ffmpeg]") -> DownloadStatus.Converting
                        line.contains("[download]") -> DownloadStatus.Downloading
                        else -> null
                    }
                    update(taskId) { current ->
                        current.copy(
                            progress = if (progress > 0f) (progress / 100f).coerceIn(0.02f, 0.99f) else current.progress,
                            etaSeconds = eta,
                            status = stage ?: current.status,
                        )
                    }
                }

                val audio = locateAudio(dir, videoId, start.format.ext)
                if (audio == null || !audio.exists() || audio.length() < 1024L) {
                    update(taskId) {
                        it.copy(
                            status = DownloadStatus.Failed,
                            message = "yt-dlp finished but no audio file was produced",
                        )
                    }
                    return@withLock
                }

                val thumbnail = locateThumbnail(dir, videoId)
                val track = Track(
                    uri = Uri.fromFile(audio).toString(),
                    title = title,
                    artist = artist,
                    album = "yt-dlp",
                    durationMs = duration,
                    artworkUri = thumbnail?.let { Uri.fromFile(it).toString() },
                    source = SOURCE_DOWNLOAD,
                    filePath = audio.absolutePath,
                    sizeBytes = audio.length(),
                    addedAt = System.currentTimeMillis(),
                )
                val trackId = repository.addDownloaded(track)
                update(taskId) {
                    it.copy(
                        status = DownloadStatus.Done,
                        progress = 1f,
                        trackId = trackId.takeIf { value -> value > 0L },
                        bytes = audio.length(),
                        message = formatBytes(audio.length()),
                    )
                }
                _messages.tryEmit("Saved \"$title\" to your library")
            } catch (_: YoutubeDL.CanceledException) {
                update(taskId) { it.copy(status = DownloadStatus.Cancelled, message = "Cancelled") }
            } catch (t: Throwable) {
                update(taskId) {
                    it.copy(
                        status = DownloadStatus.Failed,
                        message = (t.message ?: "Download failed").lineSequence().firstOrNull()?.take(180)
                            ?: "Download failed",
                    )
                }
            }
        }
    }

    private fun locateAudio(dir: File, videoId: String, ext: String): File? {
        val direct = File(dir, "$videoId.$ext")
        if (direct.exists()) return direct
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("$videoId.") }
            ?.firstOrNull { it.extension.equals(ext, ignoreCase = true) }
    }

    private fun locateThumbnail(dir: File, videoId: String): File? =
        dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("$videoId.") }
            ?.firstOrNull { it.extension.equals("jpg", true) || it.extension.equals("jpeg", true) || it.extension.equals("webp", true) }

    private fun videoIdOf(url: String): String {
        val parsed = Uri.parse(url)
        val candidate = when {
            !parsed.getQueryParameter("v").isNullOrBlank() -> parsed.getQueryParameter("v")
            parsed.host?.contains("youtu.be") == true -> parsed.lastPathSegment
            parsed.pathSegments.contains("shorts") -> parsed.lastPathSegment
            parsed.pathSegments.contains("embed") -> parsed.lastPathSegment
            else -> null
        }
        return candidate?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,20}")) }
            ?: "audio_${System.currentTimeMillis()}"
    }
}
