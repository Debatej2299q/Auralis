package dev.auralis.core

import android.content.Context
import android.text.format.DateUtils
import java.util.Locale

/** 245000 -> "4:05" */
fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "--:--"
    val total = ms / 1000L
    val h = total / 3600L
    val m = (total % 3600L) / 60L
    val s = total % 60L
    return if (h > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%d:%02d", m, s)
    }
}

/** 5412000 -> "1 hr 30 min" */
fun formatLongDuration(ms: Long): String {
    val minutes = ms / 60000L
    if (minutes < 1L) return "under a minute"
    val h = minutes / 60L
    val m = minutes % 60L
    return when {
        h > 0L && m > 0L -> "$h hr $m min"
        h > 0L -> "$h hr"
        else -> "$m min"
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) String.format(Locale.US, "%.2f GB", mb / 1024.0)
    else String.format(Locale.US, "%.1f MB", mb)
}

fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000f)
    n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1_000f)
    else -> n.toString()
}

fun relativeTime(context: Context, timeMs: Long): String {
    if (timeMs <= 0L) return "recently"
    return DateUtils.getRelativeTimeSpanString(
        timeMs,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}

fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 5 -> "Still awake?"
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 22 -> "Good evening"
        else -> "Late night session"
    }
}

/** Rough file size for a download at a given bitrate. */
fun estimateBytes(durationMs: Long, kbps: Int): Long {
    val rate = if (kbps <= 0) 256L else kbps.toLong()
    val seconds = if (durationMs <= 0L) 210L else durationMs / 1000L
    return seconds * rate * 1000L / 8L
}
