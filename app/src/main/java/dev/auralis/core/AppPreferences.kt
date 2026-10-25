package dev.auralis.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "auralis_settings")

data class AppSettings(
    val moodId: String = "midnight",
    val autoTune: Boolean = true,
    val downloadFormat: String = "m4a",
    val downloadQuality: String = "0",
    val libraryReady: Boolean = false,
    val showAmbient: Boolean = true,
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val mood = stringPreferencesKey("mood")
        val autoTune = booleanPreferencesKey("auto_tune")
        val format = stringPreferencesKey("download_format")
        val quality = stringPreferencesKey("download_quality")
        val libraryReady = booleanPreferencesKey("library_ready")
        val ambient = booleanPreferencesKey("ambient")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            moodId = prefs[Keys.mood] ?: "midnight",
            autoTune = prefs[Keys.autoTune] ?: true,
            downloadFormat = prefs[Keys.format] ?: "m4a",
            downloadQuality = prefs[Keys.quality] ?: "0",
            libraryReady = prefs[Keys.libraryReady] ?: false,
            showAmbient = prefs[Keys.ambient] ?: true,
        )
    }

    suspend fun setMood(id: String) {
        context.settingsDataStore.edit { it[Keys.mood] = id }
    }

    suspend fun setAutoTune(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.autoTune] = enabled }
    }

    suspend fun setDownloadFormat(format: String) {
        context.settingsDataStore.edit { it[Keys.format] = format }
    }

    suspend fun setDownloadQuality(quality: String) {
        context.settingsDataStore.edit { it[Keys.quality] = quality }
    }

    suspend fun setLibraryReady(ready: Boolean) {
        context.settingsDataStore.edit { it[Keys.libraryReady] = ready }
    }

    suspend fun setAmbient(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ambient] = enabled }
    }
}
