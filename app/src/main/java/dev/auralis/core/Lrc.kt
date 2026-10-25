package dev.auralis.core

data class LyricLine(val timeMs: Long, val text: String)

/**
 * Parses standard .lrc files. Multiple timestamps per line are supported,
 * e.g. [00:12.30][01:04.10]chorus
 */
fun parseLrc(raw: String): List<LyricLine> {
    if (raw.isBlank()) return emptyList()
    val stamp = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    val lines = mutableListOf<LyricLine>()

    raw.lineSequence().forEach { line ->
        val matches = stamp.findAll(line).toList()
        if (matches.isEmpty()) return@forEach
        val text = line.substring(matches.last().range.last + 1).trim()
        if (text.isEmpty()) return@forEach

        matches.forEach { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fractionRaw = match.groupValues[3]
            val millis = when (fractionRaw.length) {
                0 -> 0L
                1 -> fractionRaw.toLongOrNull()?.times(100L) ?: 0L
                2 -> fractionRaw.toLongOrNull()?.times(10L) ?: 0L
                else -> fractionRaw.take(3).toLongOrNull() ?: 0L
            }
            lines += LyricLine(
                timeMs = minutes * 60_000L + seconds * 1_000L + millis,
                text = text,
            )
        }
    }
    return lines.sortedBy { it.timeMs }
}
