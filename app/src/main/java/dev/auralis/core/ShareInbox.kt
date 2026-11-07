package dev.auralis.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiny hand-off between the Activity (which receives ACTION_SEND intents)
 * and the ViewModel (which owns the download queue).
 *
 * A StateFlow is used on purpose: when the process is cold-started by the
 * share sheet the intent arrives before the Compose tree and the ViewModel
 * exist, so the pending link has to survive until something collects it.
 */
object ShareInbox {

    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    /** Called from MainActivity.onCreate / onNewIntent. */
    fun submit(text: CharSequence?) {
        val clean = text?.toString()?.trim().orEmpty()
        if (clean.isNotEmpty()) _pending.value = clean
    }

    /** Called once the ViewModel has taken ownership of the payload. */
    fun consume() {
        _pending.value = null
    }

    /**
     * Pulls the first http(s) link out of a share payload. YouTube shares look
     * like "Song name - Artist https://youtu.be/ID", so the link is never the
     * whole text. Trailing punctuation from prose is dropped.
     */
    fun extractLink(text: String): String? =
        URL.find(text)?.value?.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}', '"', '\'')

    private val URL = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
}
