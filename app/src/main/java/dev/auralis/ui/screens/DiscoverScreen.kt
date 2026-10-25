package dev.auralis.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.auralis.MainViewModel
import dev.auralis.core.formatBytes
import dev.auralis.core.formatDuration
import dev.auralis.data.Track
import dev.auralis.online.AudioFormat
import dev.auralis.online.AudioQuality
import dev.auralis.online.DownloadStatus
import dev.auralis.online.DownloadTask
import dev.auralis.online.EngineState
import dev.auralis.online.YtResult
import dev.auralis.ui.components.Artwork
import dev.auralis.ui.components.EmptyState
import dev.auralis.ui.components.GlassCard
import dev.auralis.ui.components.PageHeader
import dev.auralis.ui.components.SectionHeader
import dev.auralis.ui.components.TrackRow
import dev.auralis.ui.sheets.DownloadSheet
import dev.auralis.ui.theme.Mood
import dev.auralis.ui.theme.TextPrimary
import dev.auralis.ui.theme.TextSecondary

@Composable
fun DiscoverScreen(vm: MainViewModel) {
    val discover by vm.discover.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val mood by vm.mood.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader(
                title = "Discover",
                subtitle = "Search YouTube · download with yt-dlp",
                trailing = { ConnectionChip(online = discover.online) },
            )
        }

        if (!discover.online) {
            item { OfflineCard() }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search a song, artist, anything", color = TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, null, tint = TextSecondary, modifier = Modifier.size(19.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = fieldColors(mood),
                )
                Button(
                    onClick = {
                        vm.refreshNetwork()
                        vm.searchOnline(query)
                    },
                    enabled = discover.online && !discover.searching && query.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = mood.accent,
                        contentColor = Color(0xFF120B1E),
                        disabledContainerColor = Color.White.copy(alpha = 0.06f),
                        disabledContentColor = TextSecondary,
                    ),
                ) {
                    if (discover.searching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF120B1E),
                        )
                    } else {
                        Text("Search YouTube", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste a YouTube link", color = TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Link, null, tint = TextSecondary, modifier = Modifier.size(19.dp))
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                url = readClipboard(context)
                                if (url.isNotBlank()) vm.lookupUrl(url)
                            },
                        ) {
                            Text("Paste", style = MaterialTheme.typography.labelMedium, color = mood.accent)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = fieldColors(mood),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { vm.lookupUrl(url) },
                        enabled = discover.online && !discover.previewBusy && url.isNotBlank(),
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            contentColor = TextPrimary,
                            disabledContainerColor = Color.White.copy(alpha = 0.04f),
                            disabledContentColor = TextSecondary,
                        ),
                    ) {
                        if (discover.previewBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = mood.accent)
                        } else {
                            Text("Fetch link", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        discover.error?.let { error ->
            item {
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                        .padding(12.dp),
                )
            }
        }

        if (discover.tasks.isNotEmpty()) {
            item {
                SectionHeader("Downloads") {
                    Text(
                        "${discover.activeDownloads} active",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
            items(discover.tasks, key = { "task-${it.id}" }) { task ->
                DownloadCard(
                    task = task,
                    accent = mood.accent,
                    onCancel = { vm.cancelDownload(task.id) },
                    onDismiss = { vm.dismissDownload(task.id) },
                )
            }
            item {
                Text(
                    "Clear finished",
                    style = MaterialTheme.typography.labelMedium,
                    color = mood.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { vm.clearFinishedDownloads() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        if (discover.results.isNotEmpty()) {
            item {
                SectionHeader("Search results") {
                    Text(
                        "${discover.results.size} found",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
            items(discover.results, key = { "yt-${it.id}" }) { result ->
                YtResultRow(
                    result = result,
                    accent = mood.accent,
                    onDownload = { vm.openPreview(result) },
                )
            }
        }

        if (discover.results.isEmpty() && !discover.searching && discover.error == null) {
            item {
                GlassCard {
                    Text("Two ways to get music", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Search YouTube for a track, or paste any link. Auralis extracts the audio on your phone with yt-dlp - pick M4A, MP3, Opus or FLAC and a bitrate before saving.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        if (library.downloads.isNotEmpty()) {
            item {
                SectionHeader("Downloaded to your library")
            }
            items(library.downloads, key = { "dl-${it.id}" }) { track ->
                DownloadedRow(track = track, mood = mood, onPlay = { vm.playTrack(track, library.downloads) })
            }
        }

        item {
            EngineCard(
                engine = discover.engine,
                busy = discover.engineBusy,
                online = discover.online,
                onUpdate = { vm.updateEngine() },
            )
        }

        item {
            Text(
                "No servers. No accounts. Search results and audio travel straight between your phone and YouTube through the local yt-dlp engine.",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    val preview = discover.preview
    if (preview != null) {
        DownloadSheet(
            result = preview,
            initialFormat = AudioFormat.from(settings.downloadFormat),
            initialQuality = AudioQuality.from(settings.downloadQuality),
            onDismiss = { vm.clearPreview() },
            onDownload = { format, quality ->
                vm.setDownloadFormat(format.id)
                vm.setDownloadQuality(quality.id)
                vm.download(preview, format, quality)
            },
        )
    }
}

@Composable
private fun ConnectionChip(online: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (online) Color(0xFF2BD98A).copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            if (online) Icons.Rounded.Wifi else Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = if (online) Color(0xFF2BD98A) else TextSecondary,
            modifier = Modifier.size(13.dp),
        )
        Text(
            if (online) "Online" else "Offline",
            style = MaterialTheme.typography.labelMedium,
            color = if (online) Color(0xFF2BD98A) else TextSecondary,
        )
    }
}

@Composable
private fun OfflineCard() {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.CloudOff, null, tint = TextSecondary, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("You're offline", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Search and downloads pause until Wi-Fi or data comes back. Your library, playlists and moods keep working.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun YtResultRow(result: YtResult, accent: Color, onDownload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onDownload() }
            .padding(vertical = 7.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(model = result.thumbnail, size = 58.dp, corner = 14.dp, accent = accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${result.artist} · ${formatDuration(result.durationMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDownload) {
            Icon(Icons.Rounded.Download, contentDescription = "Download", tint = accent, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun DownloadCard(
    task: DownloadTask,
    accent: Color,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val active = task.status == DownloadStatus.Downloading ||
        task.status == DownloadStatus.Converting ||
        task.status == DownloadStatus.Preparing ||
        task.status == DownloadStatus.Queued
    GlassCard(contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(task.thumbnail, 44.dp, corner = 11.dp, accent = accent)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when (task.status) {
                        DownloadStatus.Queued -> "Queued · ${task.format.label}"
                        DownloadStatus.Preparing -> "Preparing yt-dlp…"
                        DownloadStatus.Downloading -> "Downloading ${(task.progress * 100).toInt()}% · ${task.format.label}"
                        DownloadStatus.Converting -> "Converting to ${task.format.label}…"
                        DownloadStatus.Done -> "Saved · ${formatBytes(task.bytes)}"
                        DownloadStatus.Failed -> task.message ?: "Failed"
                        DownloadStatus.Cancelled -> "Cancelled"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (task.status == DownloadStatus.Failed) MaterialTheme.colorScheme.error else TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (active) "Cancel" else "Dismiss",
                style = MaterialTheme.typography.labelMedium,
                color = if (active) accent else TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { if (active) onCancel() else onDismiss() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
        if (active) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.07f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(task.progress.coerceIn(0.01f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent),
                )
            }
        }
    }
}

@Composable
private fun DownloadedRow(track: Track, mood: Mood, onPlay: () -> Unit) {
    TrackRow(
        track = track,
        isCurrent = false,
        accent = mood.accent,
        onClick = onPlay,
    )
}

@Composable
private fun EngineCard(
    engine: EngineState,
    busy: Boolean,
    online: Boolean,
    onUpdate: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("yt-dlp engine", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    when (engine) {
                        is EngineState.Ready -> "Ready · ${engine.version}"
                        is EngineState.Preparing -> engine.stage
                        is EngineState.Failed -> engine.message
                        EngineState.Idle -> "Starts automatically on first search"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (engine is EngineState.Failed) MaterialTheme.colorScheme.error else TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.14f))
                    .clickable(enabled = online && !busy) { onUpdate() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = accent)
                } else {
                    Icon(Icons.Rounded.Sync, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                }
                Text("Update", style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Pulls the latest binary from github.com/yt-dlp/yt-dlp. First run unpacks a small Python runtime, so give it a moment.",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun fieldColors(mood: Mood) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = mood.accent.copy(alpha = 0.6f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
    focusedContainerColor = Color.White.copy(alpha = 0.04f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = mood.accent,
)

private fun readClipboard(context: Context): String {
    return try {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
    } catch (_: Throwable) {
        ""
    }
}
