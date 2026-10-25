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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.auralis.MainViewModel
import dev.auralis.core.formatLongDuration
import dev.auralis.ui.components.Artwork
import dev.auralis.ui.components.EmptyState
import dev.auralis.ui.components.TrackRow
import dev.auralis.ui.sheets.TextInputDialog
import dev.auralis.ui.theme.TextPrimary
import dev.auralis.ui.theme.TextSecondary

@Composable
fun PlaylistScreen(
    vm: MainViewModel,
    playlistId: Long,
    onBack: () -> Unit,
) {
    val tracksFlow = remember(playlistId) { vm.playlistTracks(playlistId) }
    val summaryFlow = remember(playlistId) { vm.playlistSummary(playlistId) }
    val tracks by tracksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val summary by summaryFlow.collectAsStateWithLifecycle(initialValue = null)
    val player by vm.playerState.collectAsStateWithLifecycle()
    val mood by vm.mood.collectAsStateWithLifecycle()

    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.weight(1f))
                Box {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Playlist menu", tint = TextSecondary)
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename playlist") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                showRename = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete playlist") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                showDelete = true
                            },
                        )
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(
                    model = summary?.coverUri,
                    size = 124.dp,
                    corner = 28.dp,
                    accent = mood.accent,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        summary?.name ?: "Playlist",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 2,
                    )
                    Text(
                        "${tracks.size} songs · ${formatLongDuration(summary?.durationMs ?: 0L)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPill("Play", Icons.Rounded.PlayArrow, filled = true, colors = mood.colors) {
                    vm.playAll(tracks)
                }
                ActionPill("Shuffle", Icons.Rounded.Shuffle, filled = false, colors = mood.colors) {
                    vm.playAll(tracks, shuffle = true)
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }

        if (tracks.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.PlayArrow,
                    title = "This playlist is empty",
                    body = "Open a song's menu and choose \"Add to playlist\".",
                )
            }
        } else {
            itemsIndexed(tracks, key = { _, track -> "pl-${track.id}" }) { index, track ->
                Box {
                    TrackRow(
                        track = track,
                        isCurrent = track.id == player.track?.id,
                        accent = mood.accent,
                        onClick = { vm.playTrack(track, tracks) },
                        onMore = { menuFor = track.id },
                    )
                    DropdownMenu(
                        expanded = menuFor == track.id,
                        onDismissRequest = { menuFor = null },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Move up") },
                            enabled = index > 0,
                            onClick = {
                                menuFor = null
                                val ids = tracks.map { it.id }.toMutableList()
                                ids.add(index - 1, ids.removeAt(index))
                                vm.reorderPlaylist(playlistId, ids)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Move down") },
                            enabled = index < tracks.lastIndex,
                            onClick = {
                                menuFor = null
                                val ids = tracks.map { it.id }.toMutableList()
                                ids.add(index + 1, ids.removeAt(index))
                                vm.reorderPlaylist(playlistId, ids)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from playlist") },
                            onClick = {
                                menuFor = null
                                vm.removeFromPlaylist(playlistId, track.id)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showRename) {
        TextInputDialog(
            title = "Rename playlist",
            initial = summary?.name ?: "",
            confirmLabel = "Save",
            onDismiss = { showRename = false },
            onConfirm = { vm.renamePlaylist(playlistId, it) },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = Color(0xFF12121B),
            title = { Text("Delete playlist?", color = TextPrimary) },
            text = { Text("The songs stay in your library.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        vm.deletePlaylist(playlistId)
                        onBack()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }
}
