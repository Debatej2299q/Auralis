package dev.auralis.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF05050A)
val InkSoft = Color(0xFF0B0B12)
val InkCard = Color(0xFF12121B)
val TextPrimary = Color(0xFFF6F3FF)
val TextSecondary = Color(0xFF9C99B0)

private val AuralisTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.4).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 1.6.sp,
    ),
)

private val AuralisShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun AuralisTheme(mood: Mood, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = mood.accent,
        onPrimary = Color(0xFF0A0710),
        primaryContainer = mood.accent.copy(alpha = 0.18f),
        onPrimaryContainer = mood.accent,
        secondary = mood.glow,
        onSecondary = Color(0xFF0A0710),
        tertiary = mood.colors.first(),
        background = Ink,
        onBackground = TextPrimary,
        surface = InkSoft,
        onSurface = TextPrimary,
        surfaceVariant = InkCard,
        onSurfaceVariant = TextSecondary,
        outline = Color(0xFF2C2B3A),
        outlineVariant = Color(0xFF1D1C28),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF1A0606),
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = AuralisTypography,
        shapes = AuralisShapes,
        content = content,
    )
}
