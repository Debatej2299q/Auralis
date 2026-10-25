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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.auralis.BuildConfig
import dev.auralis.MainViewModel
import dev.auralis.core.formatBytes
import dev.auralis.core.formatLongDuration
import dev.auralis.ui.components.GlassCard
import dev.auralis.ui.components.PageHeader
import dev.auralis.ui.components.SectionHeader
import dev.auralis.ui.sheets.AuralisSheet
import dev.auralis.ui.sheets.SleepSheet
import dev.auralis.ui.theme.Mood
import dev.auralis.ui.theme.MoodCatalog
import dev.auralis.ui.theme.TextPrimary
import dev.auralis.ui.theme.TextSecondary
import dev.auralis.ui.theme.Ink

@Composable
fun StudioScreen(
    vm: MainViewModel,
    onRequestPermission: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val mood by vm.mood.collectAsStateWithLifecycle()
    val library by vm.library.collectAsStateWithLifecycle()
    val player by vm.playerState.collectAsStateWithLifecycle()

    var showSleep by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageHeader(
                title = "Studio",
                subtitle = "Look, sound and everything local",
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Mood")
                Text(
                    "Each mood changes the palette and the audio profile. Tap one to switch the whole vibe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                MoodCatalog.all.forEach { item ->
                    MoodChoiceRow(
                        mood = item,
                        selected = item.id == mood.id,
                        onClick = { vm.setMood(item.id) },
                    )
                }
            }
        }

        item {
            GlassCard {
                SettingToggle(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Auto-tune playback",
                    subtitle = "Apply the mood's speed, bass and space automatically",
                    checked = settings.autoTune,
                    onCheckedChange = { vm.setAutoTune(it) },
                )
                Spacer(Modifier.height(10.dp))
                SettingToggle(
                    icon = Icons.Rounded.Nightlight,
                    title = "Ambient background",
                    subtitle = "Soft drifting colour field behind the app",
                    checked = settings.showAmbient,
                    onCheckedChange = { vm.setAmbient(it) },
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Playback")
                GlassCard(contentPadding = 6.dp) {
                    SettingRow(
                        icon = Icons.Rounded.Tune,
                        title = "Current speed",
                        subtitle = "${"%.2f".format(player.speed)}x · ${mood.name}",
                        onClick = null,
                    )
                    SettingRow(
                        icon = Icons.Rounded.Timer,
                        title = "Sleep timer",
                        subtitle = if (player.sleepRemaining > 0L) {
                            "Fading out in ${player.sleepRemaining / 60 + 1} min"
                        } else {
                            "Fade out and pause automatically"
                        },
                        onClick = { showSleep = true },
                    )
                    SettingRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Library",
                        subtitle = "${library.tracks.size} songs · ${formatLongDuration(library.stats.listenedMs)} listened",
                        onClick = { if (library.permission) vm.importLibrary() else onRequestPermission() },
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("This week")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StudioStat("Plays", "${library.stats.plays}", Modifier.weight(1f))
                    StudioStat("Songs", "${library.stats.uniqueTracks}", Modifier.weight(1f))
                    StudioStat("Time", formatLongDuration(library.stats.listenedMs), Modifier.weight(1f))
                }
                if (library.mostPlayed.isNotEmpty()) {
                    GlassCard {
                        Text("Most played", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        library.mostPlayed.take(3).forEachIndexed { index, track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = mood.accent,
                                    modifier = Modifier.width(20.dp),
                                )
                                Text(
                                    track.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${track.playCount}x",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
                GlassCard(contentPadding = 6.dp) {
                    SettingRow(
                        icon = Icons.Rounded.Delete,
                        title = "Clear listening history",
                        subtitle = "Start your stats fresh",
                        onClick = { vm.clearHistory() },
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Storage")
                GlassCard(contentPadding = 6.dp) {
                    SettingRow(
                        icon = Icons.Rounded.Storage,
                        title = "Downloaded audio",
                        subtitle = "${library.downloads.size} tracks · ${formatBytes(library.downloadBytes)}",
                        onClick = null,
                    )
                    SettingRow(
                        icon = Icons.Rounded.Refresh,
                        title = "Rescan device music",
                        subtitle = "Pick up songs added since last scan",
                        onClick = { if (library.permission) vm.importLibrary() else onRequestPermission() },
                    )
                    SettingRow(
                        icon = Icons.Rounded.Delete,
                        title = "Clear artwork cache",
                        subtitle = "Covers re-extract automatically when needed",
                        onClick = { vm.clearArtworkCache() },
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("About")
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(mood.accent, mood.colors.last()))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Bolt, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Auralis", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                "v${BuildConfig.VERSION_NAME} · offline-first music player",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Your music never leaves your phone. The only online part is yt-dlp, which runs locally and talks straight to the source.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
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
}

@Composable
private fun MoodChoiceRow(mood: Mood, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(mood.colors.map { it.copy(alpha = 0.85f) }))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(mood.icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(mood.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                "${mood.tagline} · ${"%.2f".format(mood.speed)}x",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, null, tint = mood.colors.first(), modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun StudioStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@Composable
private fun SettingToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF120B1E),
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}
