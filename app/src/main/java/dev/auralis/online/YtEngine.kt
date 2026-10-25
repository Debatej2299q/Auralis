package dev.auralis.online

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed interface EngineState {
    data object Idle : EngineState
    data class Preparing(val stage: String) : EngineState
    data class Ready(val version: String) : EngineState
    data class Failed(val message: String) : EngineState
}

data class YtResult(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnail: String?,
    val url: String,
)

/**
 * The on-device yt-dlp engine (github.com/yt-dlp/yt-dlp).
 *
 * The binary and a private Python runtime are unpacked on first use, then
 * every search / download runs as a local process on the phone. No server,
 * no API key, no account.
 */
class YtEngine(private val context: Context) {

    private val initMutex = Mutex()
    private val _state = MutableStateFlow<EngineState>(EngineState.Idle)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    fun currentVersion(): String? = runCatching { YoutubeDL.version(context) }.getOrNull()

    suspend fun ensureReady(): Boolean = withContext(Dispatchers.IO) {
        if (_state.value is EngineState.Ready) return@withContext true
        initMutex.withLock {
            if (_state.value is EngineState.Ready) {
                true
            } else {
                try {
                    _state.value = EngineState.Preparing("Unpacking the yt-dlp engine")
                    YoutubeDL.getInstance().init(context.applicationContext)
                    FFmpeg.getInstance().init(context.applicationContext)
                    val version = runCatching { YoutubeDL.version(context) }.getOrNull()
                    _state.value = EngineState.Ready(version?.takeIf { it.isNotBlank() } ?: "yt-dlp")
                    true
                } catch (t: Throwable) {
                    _state.value = EngineState.Failed(
                        t.message?.take(220) ?: "The yt-dlp engine could not start"
                    )
                    false
                }
            }
        }
    }

    suspend fun search(query: String, limit: Int = 15): List<YtResult> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isEmpty()) return@withContext emptyList()
        if (!ensureReady()) throw IllegalStateException(failureMessage())

        val request = YoutubeDLRequest("ytsearch$limit:$clean").apply {
            addOption("--flat-playlist")
            addOption("--dump-json")
            addOption("--no-warnings")
            addOption("--ignore-errors")
            addOption("--socket-timeout", "20")
            addOption("--extractor-retries", "2")
        }
        val response = YoutubeDL.getInstance().execute(request, "search-${System.nanoTime()}")
        response.out
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("{") }
            .mapNotNull { parseEntry(it) }
            .toList()
    }

    suspend fun lookup(url: String): YtResult = withContext(Dispatchers.IO) {
        val clean = url.trim()
        if (clean.isEmpty()) throw IllegalArgumentException("Paste a link first")
        if (!ensureReady()) throw IllegalStateException(failureMessage())

        val request = YoutubeDLRequest(clean).apply {
            addOption("-J")
            addOption("--no-playlist")
            addOption("--no-warnings")
            addOption("--socket-timeout", "20")
        }
        val response = YoutubeDL.getInstance().execute(request, "lookup-${System.nanoTime()}")
        val root = JSONObject(response.out)
        val entry = if (root.has("entries")) {
            root.optJSONArray("entries")?.optJSONObject(0)
        } else {
            root
        } ?: throw IllegalStateException("No song found at that link")
        parseObject(entry, clean)
            ?: throw IllegalStateException("That link did not contain an audio track")
    }

    /** Pulls the freshest binary from github.com/yt-dlp/yt-dlp releases. */
    suspend fun updateEngine(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!ensureReady()) throw IllegalStateException(failureMessage())
            val status = YoutubeDL.getInstance().updateYoutubeDL(context)
            _state.value = EngineState.Ready(
                runCatching { YoutubeDL.version(context) }.getOrNull() ?: "yt-dlp"
            )
            val message = if (status == YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE) {
                "yt-dlp is already up to date"
            } else {
                "yt-dlp updated from the official releases"
            }
            Result.success(message)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun failureMessage(): String = when (val s = _state.value) {
        is EngineState.Failed -> s.message
        else -> "The yt-dlp engine is not ready"
    }

    private fun parseEntry(line: String): YtResult? = runCatching {
        parseObject(JSONObject(line), null)
    }.getOrNull()

    private fun parseObject(json: JSONObject, fallbackUrl: String?): YtResult? {
        val id = json.optString("id").ifBlank { return null }
        val title = json.optString("title").ifBlank { "Unknown title" }
        val artist = json.optString("uploader")
            .ifBlank { json.optString("channel") }
            .ifBlank { json.optString("creator") }
            .ifBlank { "YouTube" }
        val seconds = json.optDouble("duration", 0.0)
        val durationMs = if (seconds.isFinite() && seconds > 0.0) (seconds * 1000.0).toLong() else 0L
        val url = fallbackUrl
            ?: json.optString("webpage_url").ifBlank { null }
            ?: json.optString("original_url").ifBlank { null }
            ?: "https://www.youtube.com/watch?v=$id"
        return YtResult(
            id = id,
            title = title,
            artist = artist,
            durationMs = durationMs,
            thumbnail = bestThumbnail(json),
            url = url,
        )
    }

    private fun bestThumbnail(json: JSONObject): String? {
        val thumbs: JSONArray? = json.optJSONArray("thumbnails")
        if (thumbs != null && thumbs.length() > 0) {
            val last = thumbs.optJSONObject(thumbs.length() - 1)
            val url = last?.optString("url").orEmpty()
            if (url.isNotBlank()) return url
        }
        val flat = json.optString("thumbnail")
        return flat.takeIf { it.isNotBlank() }
    }
}
