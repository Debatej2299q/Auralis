package dev.auralis.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer

/**
 * A mood translated into sound. Values are deliberately gentle - we want
 * "lo-fi warmth", not a destroyed mix.
 */
data class AudioProfile(
    val bassMb: Short = 0,
    val midMb: Short = 0,
    val trebleMb: Short = 0,
    val bassStrength: Int = 0,
    val spatial: Int = 0,
    val reverb: Int = 0,
    val gainMb: Int = 0,
) {
    companion object {
        val Flat = AudioProfile()
    }
}

/**
 * Wires Android's audio effects onto the ExoPlayer audio session.
 * Every call is defensive: some OEMs simply do not expose an effect.
 */
class AudioTuner {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null
    private var loudness: LoudnessEnhancer? = null

    private var sessionId = 0
    private var current = AudioProfile.Flat

    @Synchronized
    fun attach(newSessionId: Int) {
        if (newSessionId == sessionId && newSessionId != 0) {
            apply(current)
            return
        }
        release()
        sessionId = newSessionId
        if (newSessionId == 0) return

        equalizer = runCatching { Equalizer(0, newSessionId) }.getOrNull()
        bassBoost = runCatching { BassBoost(0, newSessionId) }.getOrNull()
        virtualizer = runCatching { Virtualizer(0, newSessionId) }.getOrNull()
        reverb = runCatching { PresetReverb(0, newSessionId) }.getOrNull()
        loudness = runCatching { LoudnessEnhancer(newSessionId) }.getOrNull()

        apply(current)
    }

    @Synchronized
    fun apply(profile: AudioProfile) {
        current = profile

        equalizer?.let { eq ->
            try {
                eq.enabled = true
                val range = eq.bandLevelRange
                val bands = eq.numberOfBands.toInt()
                for (index in 0 until bands) {
                    val band = index.toShort()
                    val hz = eq.getCenterFreq(band) / 1000f
                    val gain = when {
                        hz < 250f -> profile.bassMb
                        hz < 1200f -> profile.midMb
                        hz < 3500f -> (((profile.midMb.toInt() + profile.trebleMb.toInt()) / 2).toShort())
                        else -> profile.trebleMb
                    }
                    eq.setBandLevel(band, gain.coerceIn(range[0], range[1]))
                }
            } catch (_: Throwable) {
            }
        }

        bassBoost?.let { effect ->
            try {
                effect.enabled = profile.bassStrength > 0
                if (profile.bassStrength > 0) {
                    effect.setStrength(profile.bassStrength.coerceIn(0, 1000).toShort())
                }
            } catch (_: Throwable) {
            }
        }

        virtualizer?.let { effect ->
            try {
                effect.enabled = profile.spatial > 0
                if (profile.spatial > 0) {
                    effect.setStrength(profile.spatial.coerceIn(0, 1000).toShort())
                }
            } catch (_: Throwable) {
            }
        }

        reverb?.let { effect ->
            try {
                if (profile.reverb <= 0) {
                    effect.enabled = false
                } else {
                    effect.preset = when (profile.reverb) {
                        1 -> PresetReverb.PRESET_SMALLROOM
                        2 -> PresetReverb.PRESET_MEDIUMROOM
                        else -> PresetReverb.PRESET_LARGEROOM
                    }
                    effect.enabled = true
                }
            } catch (_: Throwable) {
            }
        }

        loudness?.let { effect ->
            try {
                effect.enabled = profile.gainMb > 0
                if (profile.gainMb > 0) effect.setTargetGain(profile.gainMb)
            } catch (_: Throwable) {
            }
        }
    }

    @Synchronized
    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { reverb?.release() }
        runCatching { loudness?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        reverb = null
        loudness = null
        sessionId = 0
    }
}
