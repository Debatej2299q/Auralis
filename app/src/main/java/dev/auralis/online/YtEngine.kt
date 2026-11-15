package dev.auralis.online

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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
 * Classification of yt-dlp failures.
 *
 * YouTube regularly answers the very first request of a session with a 403
 * (or a rate-limit / timeout) while it is still handing out player
 * configuration; the identical request almost always succeeds a moment
 * later. Those failures are worth retrying silently. Permanent problems
 * (a private video, a bogus URL) are not.
 */
internal object YtFailures {

    private val permanent = listOf(
        "unsupported url",
        "is not a valid url",
        "invalid url",
        "private video",
        "video unavailable",
        "this video is unavailable",
        "removed by the uploader",
        "members-only",
        "account has been terminated",
        "confirm your age",
        "age-restricted",
    )

    fun isTransient(error: Throwable): Boolean {
        if (error is CancellationException) return false
        val text = generateSequence(error) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" | ")
            .lowercase()
        if (text.isBlank()) return true
        return permanent.none { text.contains(it) }
    }
}

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

    /** A writable cache keeps yt-dlp's player extraction warm between runs. */
    private val cacheDir: File by lazy {
        File(context.cacheDir, "yt-dlp").apply { runCatching { mkdirs() } }
    }

    fun currentVersion(): String? = runCatching { YoutubeDL.version(context) }.getOrNull()

    /** Shared with the download queue so both re-use the same warm cache. */
    internal fun cacheDirectory(): String? =
        cacheDir.takeIf { it.isDirectory }?.absolutePath

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

        val response = retrying { attempt ->
            val request = YoutubeDLRequest("ytsearch$limit:$clean").apply {
                addOption("--flat-playlist")
                addOption("--dump-json")
                addOption("--no-warnings")
                addOption("--ignore-errors")
                addNetworkOptions()
            }
            YoutubeDL.getInstance().execute(request, "search-$attempt-${System.nanoTime()}")
        }
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

        val response = retrying { attempt ->
            val request = YoutubeDLRequest(clean).apply {
                addOption("-J")
                addOption("--no-playlist")
                addOption("--no-warnings")
                addNetworkOptions()
            }
            YoutubeDL.getInstance().execute(request, "lookup-$attempt-${System.nanoTime()}")
        }
        val root = JSONObject(response.out)
        val entry = if (root.has("entries")) {
            root.optJSONArray("entries")?.optJSONObject(0)
        } else {
            root
        } ?: throw IllegalStateException("No song found at that link")
        parseObject(entry, clean)
            ?: throw IllegalStateException("That link did not contain an audio track")
    }

    /**
     * Runs [block] up to [ATTEMPTS] times, backing off between failures.
     *
     * The first YouTube call after the process starts is the one that gets
     * the occasional 403 while the engine negotiates a player client; by the
     * second attempt the same query goes through. Retrying here means the
     * user never sees that first error.
     */
    private suspend fun <T> retrying(block: suspend (Int) -> T): T {
        var failure: Throwable? = null
        for (attempt in 1..ATTEMPTS) {
            try {
                return block(attempt)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                failure = t
                if (attempt == ATTEMPTS || !YtFailures.isTransient(t)) break
                delay(BASE_BACKOFF_MS * attempt)
            }
        }
        throw failure ?: IllegalStateException("yt-dlp did not answer")
    }

    /** Retry / timeout flags shared by every yt-dlp invocation. */
    private fun YoutubeDLRequest.addNetworkOptions() {
        addOption("--socket-timeout", "25")
        addOption("--retries", "10")
        addOption("--fragment-retries", "10")
        addOption("--extractor-retries", "3")
        addOption("--retry-sleep", "1")
        if (cacheDir.isDirectory) addOption("--cache-dir", cacheDir.absolutePath)
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

    private companion object {
        const val ATTEMPTS = 3
        const val BASE_BACKOFF_MS = 600L
    }
}
