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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.auralis.MainViewModel
import dev.auralis.core.formatLongDuration
import dev.auralis.data.Track
import dev.auralis.ui.components.Artwork
import dev.auralis.ui.components.EmptyState
import dev.auralis.ui.components.PageHeader
import dev.auralis.ui.components.PillSelector
import dev.auralis.ui.components.TrackRow
import dev.auralis.ui.sheets.AddToPlaylistSheet
import dev.auralis.ui.sheets.AuralisSheet
import dev.auralis.ui.sheets.TextInputDialog
import dev.auralis.ui.theme.TextPrimary
import dev.auralis.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    vm: MainViewModel,
    onOpenPlaylist: (Long) -> Unit,
    onRequestPermission: () -> Unit,
) {
    val library by vm.library.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val mood by vm.mood.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("songs") }
    var actionTrack by remember { mutableStateOf<Track?>(null) }
    var addTrack by remember { mutableStateOf<Track?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    val trimmed = query.trim().lowercase()
    val filteredSongs = remember(library.tracks, trimmed) {
        if (trimmed.isEmpty()) library.tracks else library.tracks.filter {
            it.title.lowercase().contains(trimmed) ||
                it.artist.lowercase().contains(trimmed) ||
                it.album.lowercase().contains(trimmed)
        }
    }
    val albums = remember(library.tracks, trimmed) {
        library.tracks
            .filter {
                trimmed.isEmpty() || it.album.lowercase().contains(trimmed) || it.title.lowercase().contains(trimmed)
            }
            .groupBy { it.album.ifBlank { "Unknown album" } }
            .map { (name, tracks) -> name to tracks.sortedBy { t -> t.title.lowercase() } }
            .sortedBy { it.first.lowercase() }
    }
    val artists = remember(library.tracks, trimmed) {
        library.tracks
            .filter { trimmed.isEmpty() || it.artist.lowercase().contains(trimmed) || it.title.lowercase().contains(trimmed) }
            .groupBy { it.artist.ifBlank { "Unknown artist" } }
            .map { (name, tracks) -> name to tracks }
            .sortedBy { it.first.lowercase() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader(
                title = "Library",
                subtitle = "${library.tracks.size} songs · ${library.downloads.size} downloaded",
                trailing = {
                    IconButton(
                        onClick = {
                            if (library.permission) vm.importLibrary() else onRequestPermission()
                        },
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Rescan", tint = TextSecondary)
                    }
                },
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search your library", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(19.dp))
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = mood.accent.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = mood.accent,
                ),
            )
        }

        item {
            PillSelector(
                options = listOf(
                    "songs" to "Songs",
                    "albums" to "Albums",
                    "artists" to "Artists",
                    "playlists" to "Playlists",
                ),
                selected = tab,
                onSelect = { tab = it },
            )
        }

        when (tab) {
            "songs" -> {
                if (filteredSongs.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.LibraryMusic,
                            title = if (library.permission) "No songs here yet" else "Audio access needed",
                            body = if (library.permission) {
                                "Scan your device, or download something from Discover."
                            } else {
                                "Auralis only reads the music files you already own."
                            },
                            actionLabel = if (library.permission) "Scan device" else "Allow access",
                            onAction = {
                                if (library.permission) vm.importLibrary() else onRequestPermission()
                            },
                        )
                    }
                } else {
                    items(filteredSongs, key = { "song-${it.id}" }) { track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == player.track?.id,
                            accent = mood.accent,
                            onClick = { vm.playTrack(track, filteredSongs) },
                            onMore = { actionTrack = track },
                        )
                    }
                }
            }

            "albums" -> {
                if (albums.isEmpty()) {
                    item { EmptyState(Icons.Rounded.LibraryMusic, "No albums yet", "Your albums appear here once songs are imported.") }
                } else {
                    items(albums.chunked(2), key = { row -> "album-row-${row.firstOrNull()?.first}" }) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { (name, tracks) ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { vm.playAll(tracks) },
                                ) {
                                    Artwork(
                                        model = tracks.firstOrNull()?.artworkUri,
                                        size = 152.dp,
                                        corner = 20.dp,
                                        accent = mood.accent,
                                    )
                                    Spacer(Modifier.height(7.dp))
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${tracks.size} songs",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                    )
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            "artists" -> {
                if (artists.isEmpty()) {
                    item { EmptyState(Icons.Rounded.LibraryMusic, "No artists yet", "Artists are grouped from your local files.") }
                } else {
                    items(artists, key = { "artist-${it.first}" }) { (name, tracks) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { vm.playAll(tracks) }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Artwork(
                                model = tracks.firstOrNull()?.artworkUri,
                                size = 54.dp,
                                corner = 27.dp,
                                accent = mood.accent,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${tracks.size} songs",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                )
                            }
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            "playlists" -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .clickable { showCreate = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(mood.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = mood.accent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Create a playlist", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                }
                items(library.playlists, key = { "playlist-${it.id}" }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenPlaylist(playlist.id) }
                            .padding(vertical = 7.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(playlist.coverUri, 54.dp, corner = 14.dp, accent = mood.accent)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
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
                            )
                        }
                    }
                }
            }
        }
    }

    val pendingAction = actionTrack
    if (pendingAction != null) {
        AuralisSheet(onDismiss = { actionTrack = null }) {
            val track = pendingAction
            Text(
                track.title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(track.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(14.dp))

            SheetAction(Icons.Rounded.PlayArrow, "Play now") {
                vm.playTrack(track, filteredSongs)
                actionTrack = null
            }
            SheetAction(Icons.Rounded.PlaylistAdd, "Add to playlist") {
                actionTrack = null
                addTrack = track
            }
            SheetAction(
                icon = if (track.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = if (track.favorite) "Remove from liked" else "Add to liked",
            ) {
                vm.toggleFavorite(track)
                actionTrack = null
            }
            SheetAction(Icons.Rounded.Delete, "Remove from library", danger = true) {
                vm.deleteTrack(track)
                actionTrack = null
            }
        }
    }

    val pendingAdd = addTrack
    if (pendingAdd != null) {
        AddToPlaylistSheet(
            track = pendingAdd,
            playlists = library.playlists,
            onDismiss = { addTrack = null },
            onPick = { playlistId ->
                vm.addToPlaylist(playlistId, pendingAdd.id)
                addTrack = null
            },
            onCreateNew = {
                addTrack = null
                showCreate = true
            },
        )
    }

    if (showCreate) {
        TextInputDialog(
            title = "New playlist",
            placeholder = "Playlist name",
            confirmLabel = "Create",
            onDismiss = { showCreate = false },
            onConfirm = { vm.createPlaylist(it) },
        )
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 13.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (danger) MaterialTheme.colorScheme.error else TextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (danger) MaterialTheme.colorScheme.error else TextPrimary,
        )
    }
}
