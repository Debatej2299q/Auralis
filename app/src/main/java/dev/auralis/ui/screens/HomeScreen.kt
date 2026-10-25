package dev.auralis.ui.screens

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.auralis.MainViewModel
import dev.auralis.core.formatLongDuration
import dev.auralis.core.greeting
import dev.auralis.data.Track
import dev.auralis.ui.components.Artwork
import dev.auralis.ui.components.EmptyState
import dev.auralis.ui.components.Eyebrow
import dev.auralis.ui.components.GlassCard
import dev.auralis.ui.components.MoodCard
import dev.auralis.ui.components.QuickTile
import dev.auralis.ui.components.SectionHeader
import dev.auralis.ui.components.TrackRow
import dev.auralis.ui.sheets.TextInputDialog
import dev.auralis.ui.theme.MoodCatalog
import dev.auralis.ui.theme.TextPrimary
import dev.auralis.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenPlayer: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenStudio: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val library by vm.library.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val mood by vm.mood.collectAsStateWithLifecycle()

    var showCreatePlaylist by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        "Auralis",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                    )
                }
                IconButton(onClick = onOpenStudio) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Studio", tint = TextSecondary)
                }
            }
        }

        item {
            HeroCard(
                moodGradient = mood.colors,
                moodName = mood.name,
                moodTagline = mood.tagline,
                moodIcon = mood.icon,
                hasQueue = player.track != null,
                canPlay = library.tracks.isNotEmpty(),
                onShuffle = { vm.playAll(library.tracks, shuffle = true) },
                onOpenPlayer = onOpenPlayer,
            )
        }

        if (library.isEmpty) {
            item {
                GlassCard {
                    EmptyState(
                        icon = Icons.Rounded.Favorite,
                        title = if (library.permission) "Your library is empty" else "Let Auralis see your music",
                        body = if (library.permission) {
                            "Scan this device to pull in every song Android already knows about."
                        } else {
                            "Grant audio access to build your offline library. Nothing leaves your phone."
                        },
                        actionLabel = if (library.permission) "Scan my music" else "Allow access",
                        onAction = {
                            if (library.permission) vm.importLibrary() else onRequestPermission()
                        },
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Set the mood") {
                    Text(
                        "look + sound",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(MoodCatalog.all, key = { it.id }) { item ->
                        MoodCard(
                            mood = item,
                            selected = item.id == mood.id,
                            onClick = { vm.setMood(item.id) },
                        )
                    }
                }
                Text(
                    "Every mood repaints the app and gently retunes playback - speed, bass, air and space.",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(
                    icon = Icons.Rounded.Favorite,
                    title = "Liked",
                    value = "${library.liked.size} songs",
                    accent = mood.accent,
                    onClick = onOpenLiked,
                    modifier = Modifier.weight(1f),
                )
                QuickTile(
                    icon = Icons.Rounded.CloudDownload,
                    title = "Downloads",
                    value = "${library.downloads.size} tracks",
                    accent = mood.accent,
                    onClick = onOpenDiscover,
                    modifier = Modifier.weight(1f),
                )
                QuickTile(
                    icon = Icons.Rounded.Shuffle,
                    title = "Shuffle",
                    value = "Everything",
                    accent = mood.accent,
                    onClick = { vm.playAll(library.tracks, shuffle = true) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (library.recent.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Fresh in your library")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(library.recent.take(12), key = { "recent-${it.id}" }) { track ->
                            TrackTile(
                                track = track,
                                accent = mood.accent,
                                onClick = { vm.playTrack(track, library.recent) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Playlists") {
                    TextButton(onClick = { showCreatePlaylist = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New", style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (library.playlists.isEmpty()) {
                    GlassCard {
                        Column {
                            Eyebrow("Start a collection")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Make a playlist for slow mornings, gym sets or 3am drives.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(library.playlists, key = { "pl-${it.id}" }) { playlist ->
                            Column(
                                modifier = Modifier
                                    .width(148.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable { onOpenPlaylist(playlist.id) },
                            ) {
                                Artwork(
                                    model = playlist.coverUri,
                                    size = 148.dp,
                                    corner = 22.dp,
                                    accent = mood.accent,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${playlist.trackCount} songs · ${formatLongDuration(playlist.durationMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (library.mostPlayed.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("On repeat")
                    library.mostPlayed.take(5).forEach { track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == player.track?.id,
                            accent = mood.accent,
                            onClick = { vm.playTrack(track, library.mostPlayed) },
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${library.stats.plays} plays · ${formatLongDuration(library.stats.listenedMs)} listened",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Text(
                    "Everything stays on your phone",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                )
            }
        }
    }

    if (showCreatePlaylist) {
        TextInputDialog(
            title = "New playlist",
            placeholder = "Late night drive",
            confirmLabel = "Create",
            onDismiss = { showCreatePlaylist = false },
            onConfirm = { vm.createPlaylist(it) },
        )
    }
}

@Composable
private fun HeroCard(
    moodGradient: List<Color>,
    moodName: String,
    moodTagline: String,
    moodIcon: ImageVector,
    hasQueue: Boolean,
    canPlay: Boolean,
    onShuffle: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(198.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(moodGradient)),
    ) {
        Icon(
            imageVector = moodIcon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.16f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 34.dp, y = (-28).dp)
                .size(168.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "TONIGHT'S MOOD",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
                Spacer(Modifier.height(4.dp))
                Text(moodName, style = MaterialTheme.typography.displayMedium, color = Color.White)
                Text(
                    moodTagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DarkPill(
                    label = if (canPlay) "Shuffle play" else "No songs yet",
                    icon = Icons.Rounded.Shuffle,
                    enabled = canPlay,
                    onClick = onShuffle,
                )
                if (hasQueue) {
                    DarkPill(
                        label = "Now playing",
                        icon = Icons.Rounded.PlayArrow,
                        enabled = true,
                        onClick = onOpenPlayer,
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkPill(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF0A0A12).copy(alpha = if (enabled) 0.82f else 0.45f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun TrackTile(track: Track, accent: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
    ) {
        Artwork(model = track.artworkUri, size = 132.dp, corner = 20.dp, accent = accent)
        Spacer(Modifier.height(8.dp))
        Text(
            track.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            track.artist,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
