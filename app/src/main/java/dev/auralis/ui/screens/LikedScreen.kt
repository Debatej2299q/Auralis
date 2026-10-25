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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.auralis.MainViewModel
import dev.auralis.core.formatLongDuration
import dev.auralis.ui.components.EmptyState
import dev.auralis.ui.components.TrackRow
import dev.auralis.ui.theme.TextPrimary
import dev.auralis.ui.theme.TextSecondary

@Composable
fun LikedScreen(vm: MainViewModel, onBack: () -> Unit) {
    val library by vm.library.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()
    val mood by vm.mood.collectAsStateWithLifecycle()

    val tracks = library.liked
    val totalMs = tracks.sumOf { it.durationMs }

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
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(mood.colors.take(2))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Liked songs", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(
                        "${tracks.size} songs · ${formatLongDuration(totalMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPill("Play", Icons.Rounded.PlayArrow, filled = true, mood.colors) {
                    vm.playAll(tracks)
                }
                ActionPill("Shuffle", Icons.Rounded.Shuffle, filled = false, mood.colors) {
                    vm.playAll(tracks, shuffle = true)
                }
            }
        }

        item { Spacer(Modifier.height(6.dp)) }

        if (tracks.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.FavoriteBorder,
                    title = "No liked songs yet",
                    body = "Tap the heart on any song and it lands here.",
                )
            }
        } else {
            items(tracks, key = { "liked-${it.id}" }) { track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id == player.track?.id,
                    accent = mood.accent,
                    onClick = { vm.playTrack(track, tracks) },
                    onMore = { vm.toggleFavorite(track) },
                )
            }
        }
    }
}

@Composable
internal fun ActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    filled: Boolean,
    colors: List<Color>,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(Brush.horizontalGradient(colors.take(2)))
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.06f))
                },
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (filled) Color(0xFF120B1E) else TextPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) Color(0xFF120B1E) else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
