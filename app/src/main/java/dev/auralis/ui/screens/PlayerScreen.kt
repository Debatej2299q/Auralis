package dev.auralis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.auralis.MainViewModel
import dev.auralis.core.formatDuration
import dev.auralis.data.Track
import dev.auralis.player.PlayerController
import dev.auralis.ui.components.Artwork
import dev.auralis.ui.sheets.AddToPlaylistSheet
import dev.auralis.ui.sheets.LyricsSheet
import dev.auralis.ui.sheets.QueueSheet
import dev.auralis.ui.sheets.SleepSheet
import dev.auralis.ui.sheets.TuneSheet
import dev.auralis.ui.theme.Ink
import dev.auralis.ui.theme.Mood
import dev.auralis.ui.theme.TextPrimary
import dev.auralis.ui.theme.TextSecondary

@Composable
fun PlayerScreen(vm: MainViewModel, onBack: () -> Unit) {
    val player by vm.playerState.collectAsStateWithLifecycle()
    val mood by vm.mood.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()

    val track = player.track
    var showQueue by remember { mutableStateOf(false) }
    var showTune by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<String?>(null) }
    var scrub by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(track?.id) {
        lyrics = null
        scrub = null
    }

    if (track == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Nothing playing yet", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Pick a song from your library",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(mood.accent.copy(alpha = 0.16f))
                        .clickable { onBack() }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text("Back", color = mood.accent, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        return
    }

    val art = player.artwork ?: track.artworkUri
    val context = LocalContext.current
    val isFavorite = library.tracks.firstOrNull { it.id == track.id }?.favorite == true

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artSize = minOf(maxWidth * 0.82f, maxHeight * 0.36f)

        if (!art.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(art).crossfade(400).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(64.dp),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Ink.copy(alpha = 0.86f),
                            Ink,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Close player",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "PLAYING NOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Text(
                        track.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = mood.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { showAdd = true }) {
                    Icon(
                        Icons.Rounded.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.weight(0.6f))

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(artSize + 46.dp)
                        .blur(46.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(mood.accent.copy(alpha = 0.42f), Color.Transparent),
                            ),
                            CircleShape,
                        ),
                )
                Artwork(
                    model = art,
                    size = artSize,
                    corner = 30.dp,
                    accent = mood.accent,
                )
            }

            Spacer(Modifier.height(26.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { vm.toggleFavorite(track) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isFavorite) mood.accent else TextSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            val progress = scrub ?: player.progress
            val totalMs = player.durationMs
            Slider(
                value = progress,
                onValueChange = { scrub = it },
                onValueChangeFinished = {
                    scrub?.let { vm.seekToFraction(it) }
                    scrub = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = mood.accent,
                    activeTrackColor = mood.accent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.14f),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    formatDuration((totalMs * progress).toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Text(
                    formatDuration(totalMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (player.shuffle) mood.accent else TextSecondary,
                        modifier = Modifier.size(21.dp),
                    )
                }
                IconButton(onClick = { vm.previous() }, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(mood.colors.take(2)))
                        .clickable { vm.togglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (player.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.5.dp,
                            color = Color(0xFF120B1E),
                        )
                    } else {
                        Icon(
                            imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play or pause",
                            tint = Color(0xFF120B1E),
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
                IconButton(onClick = { vm.next() }, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(onClick = { vm.cycleRepeat() }) {
                    val active = player.repeatMode != PlayerController.REPEAT_OFF
                    Icon(
                        imageVector = if (player.repeatMode == PlayerController.REPEAT_ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (active) mood.accent else TextSecondary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }

            Spacer(Modifier.weight(0.4f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PlayerAction(Icons.Rounded.Subject, "Lyrics", showLyrics, mood) {
                    showLyrics = true
                }
                PlayerAction(Icons.Rounded.QueueMusic, "Queue", showQueue, mood) {
                    showQueue = true
                }
                PlayerAction(Icons.Rounded.Tune, "Tune", showTune, mood) {
                    showTune = true
                }
                PlayerAction(
                    Icons.Rounded.Timer,
                    if (player.sleepRemaining > 0L) "${player.sleepRemaining / 60 + 1}m" else "Sleep",
                    player.sleepRemaining > 0L,
                    mood,
                ) {
                    showSleep = true
                }
            }
        }
    }

    if (showQueue) {
        QueueSheet(
            queue = player.queue,
            currentId = track.id,
            onDismiss = { showQueue = false },
            onSelect = { index ->
                vm.jumpTo(index)
                showQueue = false
            },
            onRemove = { index -> vm.removeQueueItem(index) },
            onMove = { from, to -> vm.moveQueueItem(from, to) },
        )
    }

    if (showTune) {
        TuneSheet(
            mood = mood,
            currentSpeed = player.speed,
            onDismiss = { showTune = false },
            onApply = { speed, bass, mid, treble, space, reverb ->
                vm.applyManualTune(speed, bass, mid, treble, space, reverb)
            },
        )
    }

    if (showSleep) {
        SleepSheet(
            remainingSeconds = player.sleepRemaining,
            onDismiss = { showSleep = false },
            onPick = { minutes ->
                vm.setSleepTimer(minutes)
                showSleep = false
            },
        )
    }

    if (showAdd) {
        AddToPlaylistSheet(
            track = track,
            playlists = library.playlists,
            onDismiss = { showAdd = false },
            onPick = { playlistId ->
                vm.addToPlaylist(playlistId, track.id)
                showAdd = false
            },
            onCreateNew = { showAdd = false },
        )
    }

    if (showLyrics) {
        LaunchedEffect(track.id) {
            if (lyrics == null) lyrics = vm.lyricsFor(track)
        }
        LyricsSheet(
            track = track,
            lyrics = lyrics,
            positionMs = player.positionMs,
            onDismiss = { showLyrics = false },
        )
    }
}

@Composable
private fun PlayerAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
    mood: Mood,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) mood.accent else TextSecondary,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) mood.accent else TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
