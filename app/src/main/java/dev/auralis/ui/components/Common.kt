package dev.auralis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.auralis.core.formatDuration
import dev.auralis.data.Track
import dev.auralis.ui.theme.Mood
import dev.auralis.ui.theme.TextPrimary
import dev.auralis.ui.theme.TextSecondary

/** Soft animated colour field that sits behind every screen. */
@Composable
fun AmbientBackground(mood: Mood, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 42_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF05050A), Color(0xFF080810), Color(0xFF05050A)),
            ),
        )
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(mood.glow.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(w * (0.12f + drift * 0.16f), h * 0.06f),
                radius = w * 0.85f,
            ),
            radius = w * 0.85f,
            center = Offset(w * (0.12f + drift * 0.16f), h * 0.06f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(mood.colors.first().copy(alpha = 0.20f), Color.Transparent),
                center = Offset(w * (0.92f - drift * 0.20f), h * 0.62f),
                radius = w * 0.80f,
            ),
            radius = w * 0.80f,
            center = Offset(w * (0.92f - drift * 0.20f), h * 0.62f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(mood.colors.last().copy(alpha = 0.16f), Color.Transparent),
                center = Offset(w * 0.28f, h * (0.95f - drift * 0.08f)),
                radius = w * 0.75f,
            ),
            radius = w * 0.75f,
            center = Offset(w * 0.28f, h * (0.95f - drift * 0.08f)),
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.045f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.075f), shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun Artwork(
    model: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.42f), Color(0xFF1B1B26), Color(0xFF0C0C14)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (model.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(size * 0.34f),
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .crossfade(260)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        trailing?.invoke()
    }
}

@Composable
fun Eyebrow(text: String, color: Color = TextSecondary, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun MoodCard(
    mood: Mood,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .width(168.dp)
            .height(106.dp)
            .clip(shape)
            .background(Brush.linearGradient(mood.colors))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.14f),
                shape = shape,
            )
            .clickable { onClick() },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    ),
                ),
        )
        Icon(
            imageVector = mood.icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .padding(14.dp)
                .size(22.dp),
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = mood.colors.first(),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            Text(
                mood.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                mood.tagline,
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrent) accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Artwork(
            model = track.artworkUri,
            size = 50.dp,
            corner = 13.dp,
            accent = accent,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) accent else TextPrimary,
            )
            Text(
                text = track.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        if (onMore != null) {
            IconButton(onClick = onMore, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun PillSelector(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, label) ->
            val isSelected = id == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else Color.White.copy(alpha = 0.04f),
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        else Color.White.copy(alpha = 0.07f),
                        RoundedCornerShape(50),
                    )
                    .clickable { onSelect(id) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
                )
            }
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    mood: Mood,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Brush.horizontalGradient(mood.colors.take(2)))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color(0xFF120B1E), modifier = Modifier.size(18.dp))
        }
        Text(
            text = text,
            color = Color(0xFF120B1E),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(17.dp))
        }
        Text(text, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            GhostButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        trailing?.invoke()
    }
}
