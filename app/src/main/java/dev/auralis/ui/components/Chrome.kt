package dev.auralis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.auralis.data.Track
import dev.auralis.ui.theme.TextPrimary
import dev.auralis.ui.theme.TextSecondary

data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomTabs = listOf(
    BottomTab("home", "Home", Icons.Rounded.Home),
    BottomTab("library", "Library", Icons.Rounded.LibraryMusic),
    BottomTab("discover", "Discover", Icons.Rounded.Explore),
    BottomTab("studio", "Studio", Icons.Rounded.Tune),
)

val topLevelRoutes = bottomTabs.map { it.route }.toSet()

@Composable
fun MiniPlayer(
    track: Track,
    artwork: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progress: Float,
    accent: Color,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF15151F), Color(0xFF0D0D15))))
            .clickable { onOpen() },
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(
                model = artwork ?: track.artworkUri,
                size = 46.dp,
                corner = 12.dp,
                accent = accent,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    text = track.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(42.dp)) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accent,
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play or pause",
                        tint = accent,
                        modifier = Modifier.size(27.dp),
                    )
                }
            }
            IconButton(onClick = onNext, modifier = Modifier.size(42.dp)) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.06f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(accent),
            )
        }
    }
}

@Composable
fun AuralisBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = Color(0xFF0A0A11),
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(23.dp),
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    selectedTextColor = accent,
                    indicatorColor = accent.copy(alpha = 0.15f),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                ),
            )
        }
    }
}

@Composable
fun QuickTile(
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .clickable { onClick() }
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1)
        }
    }
}
