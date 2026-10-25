package dev.auralis.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.auralis.player.AudioProfile

/**
 * A mood is a look *and* a sound. Picking one repaints the app and gently
 * re-tunes playback (speed, bass, air, space) at the same time.
 */
data class Mood(
    val id: String,
    val name: String,
    val tagline: String,
    val colors: List<Color>,
    val accent: Color,
    val glow: Color,
    val profile: AudioProfile,
    val speed: Float,
    val icon: ImageVector,
) {
    val gradient get() = colors
}

object MoodCatalog {

    val midnight = Mood(
        id = "midnight",
        name = "Midnight",
        tagline = "Clean, deep, after hours",
        colors = listOf(Color(0xFF6E5BFF), Color(0xFFA87CFF), Color(0xFFFF6FB5)),
        accent = Color(0xFFA87CFF),
        glow = Color(0xFF6E5BFF),
        profile = AudioProfile.Flat,
        speed = 1f,
        icon = Icons.Rounded.Nightlight,
    )

    val slow = Mood(
        id = "slow",
        name = "Slow Motion",
        tagline = "0.85x, velvet bass",
        colors = listOf(Color(0xFF4E6BFF), Color(0xFF8FD3FF), Color(0xFFC6A8FF)),
        accent = Color(0xFF8FD3FF),
        glow = Color(0xFF4E6BFF),
        profile = AudioProfile(
            bassMb = 420,
            midMb = -80,
            trebleMb = -220,
            bassStrength = 260,
            reverb = 1,
        ),
        speed = 0.85f,
        icon = Icons.Rounded.SlowMotionVideo,
    )

    val lofi = Mood(
        id = "lofi",
        name = "Lo-Fi",
        tagline = "Tape-warm, rolled-off highs",
        colors = listOf(Color(0xFFD9A066), Color(0xFFE8C39E), Color(0xFF8B6B4A)),
        accent = Color(0xFFE8C39E),
        glow = Color(0xFFD9A066),
        profile = AudioProfile(
            bassMb = 260,
            midMb = 0,
            trebleMb = -700,
            spatial = 140,
            reverb = 1,
            gainMb = 60,
        ),
        speed = 0.92f,
        icon = Icons.Rounded.GraphicEq,
    )

    val neon = Mood(
        id = "neon",
        name = "Neon",
        tagline = "Synthwave punch",
        colors = listOf(Color(0xFFFF2D95), Color(0xFF00E5FF), Color(0xFFB14BFF)),
        accent = Color(0xFFFF5FAE),
        glow = Color(0xFF00E5FF),
        profile = AudioProfile(
            bassMb = 560,
            midMb = -120,
            trebleMb = 340,
            bassStrength = 500,
            spatial = 360,
        ),
        speed = 1f,
        icon = Icons.Rounded.Bolt,
    )

    val sunset = Mood(
        id = "sunset",
        name = "Sunset",
        tagline = "Golden hour, 0.96x",
        colors = listOf(Color(0xFFFF7A59), Color(0xFFFFB56B), Color(0xFFC9487B)),
        accent = Color(0xFFFFA36B),
        glow = Color(0xFFFF7A59),
        profile = AudioProfile(bassMb = 200, midMb = 60, trebleMb = 100, reverb = 1),
        speed = 0.96f,
        icon = Icons.Rounded.WbSunny,
    )

    val forest = Mood(
        id = "forest",
        name = "Forest",
        tagline = "Vocal clarity, calm air",
        colors = listOf(Color(0xFF47C08A), Color(0xFF9BE7C4), Color(0xFF2E7D5B)),
        accent = Color(0xFF6FE3AE),
        glow = Color(0xFF47C08A),
        profile = AudioProfile(bassMb = 0, midMb = 320, trebleMb = 90, spatial = 200),
        speed = 1f,
        icon = Icons.Rounded.Park,
    )

    val ocean = Mood(
        id = "ocean",
        name = "Ocean",
        tagline = "Wide, slow, weightless",
        colors = listOf(Color(0xFF38BDF8), Color(0xFF22D3EE), Color(0xFF4F7CFF)),
        accent = Color(0xFF5AD1FF),
        glow = Color(0xFF22D3EE),
        profile = AudioProfile(
            bassMb = 150,
            midMb = -60,
            trebleMb = -120,
            spatial = 480,
            reverb = 2,
        ),
        speed = 0.97f,
        icon = Icons.Rounded.Waves,
    )

    val vapor = Mood(
        id = "vapor",
        name = "Vapor",
        tagline = "Pastel haze, 1.02x",
        colors = listOf(Color(0xFFC084FC), Color(0xFFF0ABFC), Color(0xFF93C5FD)),
        accent = Color(0xFFE0A9FF),
        glow = Color(0xFFC084FC),
        profile = AudioProfile(
            bassMb = 120,
            midMb = -80,
            trebleMb = 200,
            spatial = 440,
            reverb = 2,
        ),
        speed = 1.02f,
        icon = Icons.Rounded.AutoAwesome,
    )

    val ember = Mood(
        id = "ember",
        name = "Ember",
        tagline = "1.08x, gym-ready",
        colors = listOf(Color(0xFFFF4D4D), Color(0xFFFF8A3D), Color(0xFFFFD166)),
        accent = Color(0xFFFF8A5C),
        glow = Color(0xFFFF4D4D),
        profile = AudioProfile(
            bassMb = 620,
            midMb = 140,
            trebleMb = 260,
            bassStrength = 560,
            gainMb = 70,
        ),
        speed = 1.08f,
        icon = Icons.Rounded.LocalFireDepartment,
    )

    val coffee = Mood(
        id = "coffee",
        name = "Coffee",
        tagline = "Warm room, soft top",
        colors = listOf(Color(0xFFC08552), Color(0xFFE6C9A8), Color(0xFF7A5230)),
        accent = Color(0xFFD9A87C),
        glow = Color(0xFFC08552),
        profile = AudioProfile(bassMb = 240, midMb = 100, trebleMb = -160, reverb = 1),
        speed = 1f,
        icon = Icons.Rounded.LocalCafe,
    )

    val all: List<Mood> = listOf(midnight, slow, lofi, neon, sunset, forest, ocean, vapor, ember, coffee)

    fun byId(id: String): Mood = all.firstOrNull { it.id == id } ?: midnight
}
