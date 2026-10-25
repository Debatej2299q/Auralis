package dev.auralis.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads every music file Android's MediaStore knows about. */
class LocalLibraryScanner(private val context: Context) {

    suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        val found = ArrayList<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    if (idCol < 0) break
                    val id = cursor.getLong(idCol)
                    val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    if (duration in 0L..15_000L) continue

                    val fileName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val title = (if (titleCol >= 0) cursor.getString(titleCol) else null)
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() && it != "<unknown>" }
                        ?: fileName?.substringBeforeLast('.')
                        ?: "Unknown track"
                    val artist = (if (artistCol >= 0) cursor.getString(artistCol) else null)
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() && it != "<unknown>" }
                        ?: "Unknown artist"
                    val album = (if (albumCol >= 0) cursor.getString(albumCol) else null)
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() && it != "<unknown>" }
                        ?: "Unknown album"
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L

                    val contentUri = ContentUris.withAppendedId(collection, id).toString()
                    val artwork = if (albumId > 0L) {
                        ContentUris.withAppendedId(ALBUM_ART_URI, albumId).toString()
                    } else {
                        null
                    }

                    found += Track(
                        uri = contentUri,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        artworkUri = artwork,
                        source = SOURCE_LOCAL,
                        filePath = if (dataCol >= 0) cursor.getString(dataCol) else null,
                        sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                        addedAt = if (dateCol >= 0) cursor.getLong(dateCol) * 1000L else System.currentTimeMillis(),
                    )
                }
            }
        } catch (_: Throwable) {
            // Permission revoked halfway through, weird OEM provider, etc.
        }
        found
    }

    private companion object {
        val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
