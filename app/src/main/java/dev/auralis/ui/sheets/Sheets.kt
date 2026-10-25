package dev.auralis.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.auralis.core.LyricLine
import dev.auralis.core.formatBytes
import dev.auralis.core.formatDuration
import dev.auralis.core.parseLrc
import dev.auralis.data.PlaylistSummary
import dev.auralis.data.Track
import dev.auralis.online.AudioFormat
import dev.auralis.online.AudioQuality
import dev.auralis.online.YtResult
import dev.auralis.ui.components.Artwork
import dev.auralis.ui.components.EmptyState
import dev.auralis.ui.components.PillSelector
import dev.auralis.ui.theme.Mood
import dev.auralis.ui.theme.TextPrimary
import dev.auralis.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuralisSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0E0E16),
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.20f)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            content = content,
        )
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String = "",
    placeholder: String = "",
    confirmLabel: String = "Save",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12121B),
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(placeholder, color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                        onDismiss()
                    }
                },
            ) { Text(confirmLabel, color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    track: Track?,
    playlists: List<PlaylistSummary>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    onCreateNew: () -> Unit,
) {
    if (track == null) return
    AuralisSheet(onDismiss) {
        Text("Add to playlist", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text(
            track.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.heightIn(max = 340.dp)) {
            items(playlists, key = { it.id }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPick(playlist.id) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(playlist.coverUri, 44.dp, corner = 12.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            "${playlist.trackCount} songs",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                    Icon(
                        Icons.Rounded.PlaylistAdd,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCreateNew() }
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("New playlist", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSheet(
    result: YtResult,
    initialFormat: AudioFormat,
    initialQuality: AudioQuality,
    onDismiss: () -> Unit,
    onDownload: (AudioFormat, AudioQuality) -> Unit,
) {
    var format by remember { mutableStateOf(initialFormat) }
    var quality by remember { mutableStateOf(initialQuality) }
    val accent = MaterialTheme.colorScheme.primary
    val size = remember(format, quality, result.durationMs) {
        dev.auralis.core.estimateBytes(result.durationMs, quality.kbps)
    }

    AuralisSheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(result.thumbnail, 64.dp, corner = 16.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${result.artist} · ${formatDuration(result.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Text("FORMAT", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        PillSelector(
            options = AudioFormat.entries.map { it.id to it.label },
            selected = format.id,
            onSelect = { id -> format = AudioFormat.from(id) },
        )
        Text(
            format.blurb,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(20.dp))
        Text("QUALITY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        PillSelector(
            options = AudioQuality.options.map { it.id to it.label },
            selected = quality.id,
            onSelect = { id -> quality = AudioQuality.from(id) },
        )

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Estimated size", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(formatBytes(size), style = MaterialTheme.typography.titleMedium, color = accent)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onDownload(format, quality) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Color(0xFF120B1E),
            ),
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Download to library", style = MaterialTheme.typography.labelLarge)
        }
        Text(
            "Runs locally through yt-dlp - no server, no account.",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Track>,
    currentId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    AuralisSheet(onDismiss) {
        Text("Up next", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text(
            "${queue.size} songs in the queue",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            itemsIndexed(queue) { index, track ->
                val isCurrent = track.id == currentId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(track.artworkUri, 40.dp, corner = 10.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            maxLines = 1,
                        )
                    }
                    IconButton(
                        onClick = { if (index > 0) onMove(index, index - 1) },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, "Move up", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { if (index < queue.lastIndex) onMove(index, index + 1) },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(Icons.Rounded.ArrowDownward, "Move down", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.Close, "Remove", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuneSheet(
    mood: Mood,
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onApply: (Float, Int, Int, Int, Int, Int) -> Unit,
) {
    var speed by remember { mutableFloatStateOf(currentSpeed.coerceIn(0.75f, 1.2f)) }
    var bass by remember { mutableFloatStateOf(mood.profile.bassMb.toFloat()) }
    var mid by remember { mutableFloatStateOf(mood.profile.midMb.toFloat()) }
    var treble by remember { mutableFloatStateOf(mood.profile.trebleMb.toFloat()) }
    var space by remember { mutableFloatStateOf(mood.profile.spatial.toFloat()) }
    var reverb by remember { mutableStateOf(mood.profile.reverb > 0) }
    val accent = MaterialTheme.colorScheme.primary

    fun push() {
        onApply(
            speed,
            bass.toInt(),
            mid.toInt(),
            treble.toInt(),
            space.toInt(),
            if (reverb) 2 else 0,
        )
    }

    AuralisSheet(onDismiss) {
        Text("Tune this vibe", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text(
            "Fine-tune ${mood.name}. Changes apply live.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(18.dp))

        TuneSlider("Speed", String.format(java.util.Locale.US, "%.2fx", speed), speed, 0.75f, 1.2f, accent) { value ->
            speed = value
            push()
        }
        TuneSlider("Bass", "${bass.toInt()} mb", bass, -600f, 800f, accent) { value ->
            bass = value
            push()
        }
        TuneSlider("Warmth", "${mid.toInt()} mb", mid, -600f, 600f, accent) { value ->
            mid = value
            push()
        }
        TuneSlider("Air", "${treble.toInt()} mb", treble, -800f, 600f, accent) { value ->
            treble = value
            push()
        }
        TuneSlider("Space", "${space.toInt()}", space, 0f, 1000f, accent) { value ->
            space = value
            push()
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .clickable {
                    reverb = !reverb
                    push()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Room reverb", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Adds a little depth",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            Text(
                if (reverb) "ON" else "OFF",
                style = MaterialTheme.typography.labelLarge,
                color = if (reverb) accent else TextSecondary,
            )
        }
    }
}

@Composable
private fun TuneSlider(
    label: String,
    valueLabel: String,
    value: Float,
    min: Float,
    max: Float,
    accent: Color,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = accent.copy(alpha = 0.18f),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSheet(
    remainingSeconds: Long,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    AuralisSheet(onDismiss) {
        Text("Sleep timer", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text(
            if (remainingSeconds > 0L) "Fading out in ${remainingSeconds / 60 + 1} min" else "Playback will fade out gently",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        val options = listOf(5, 10, 15, 30, 45, 60)
        options.forEach { minutes ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onPick(minutes) }
                    .padding(vertical = 13.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$minutes minutes", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onPick(0) }
                .padding(vertical = 13.dp, horizontal = 8.dp),
        ) {
            Text("Turn off", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    track: Track,
    lyrics: String?,
    positionMs: Long,
    onDismiss: () -> Unit,
) {
    val lines: List<LyricLine> = remember(lyrics) { parseLrc(lyrics ?: "") }
    val currentIndex = lines.indexOfLast { it.timeMs <= positionMs }
    val listState = rememberLazyListState()

    AuralisSheet(onDismiss) {
        Text(track.title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, maxLines = 1)
        Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(14.dp))
        if (lines.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Subject,
                title = "No lyrics found",
                body = "Drop a .lrc file next to the audio file, named exactly like the song, and it will appear here.",
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(lines.size) { index ->
                    val line = lines[index]
                    val active = index == currentIndex
                    Text(
                        text = line.text,
                        style = if (active) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                        color = if (active) MaterialTheme.colorScheme.primary else TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Synced from ${track.title}.lrc",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun DownloadRowPreview(task: dev.auralis.online.DownloadTask, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(task.thumbnail, 42.dp, corner = 10.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(task.format.label)
                    if (task.kbps > 0) append(" · ${task.kbps} kbps")
                    if (task.status == dev.auralis.online.DownloadStatus.Done) append(" · saved")
                },
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
        Text(
            when (task.status) {
                dev.auralis.online.DownloadStatus.Done -> "Done"
                dev.auralis.online.DownloadStatus.Failed -> "Failed"
                dev.auralis.online.DownloadStatus.Cancelled -> "Cancelled"
                dev.auralis.online.DownloadStatus.Queued -> "Queued"
                dev.auralis.online.DownloadStatus.Preparing -> "Preparing"
                dev.auralis.online.DownloadStatus.Converting -> "Converting"
                dev.auralis.online.DownloadStatus.Downloading -> "${(task.progress * 100).toInt()}%"
            },
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontSize = 11.sp,
        )
    }
}
