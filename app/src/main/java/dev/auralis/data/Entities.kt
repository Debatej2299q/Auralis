package dev.auralis.data

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val SOURCE_LOCAL = "LOCAL"
const val SOURCE_DOWNLOAD = "DOWNLOAD"

/**
 * One playable song. Local device tracks (MediaStore content:// uris) and
 * yt-dlp downloads (file:// uris) live in the same table so playlists and
 * favourites can freely mix them.
 */
@Entity(
    tableName = "tracks",
    indices = [Index(value = ["uri"], unique = true), Index(value = ["title"]), Index(value = ["favorite"])],
)
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long = 0L,
    val artworkUri: String? = null,
    val source: String = SOURCE_LOCAL,
    val filePath: String? = null,
    val sizeBytes: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val favorite: Boolean = false,
    val playCount: Int = 0,
) {
    val mediaUri: Uri get() = Uri.parse(uri)
    val isDownload: Boolean get() = source == SOURCE_DOWNLOAD
    val subtitle: String
        get() = when {
            artist.isBlank() || artist == "<unknown>" -> album
            album.isBlank() || album == "<unknown>" -> artist
            else -> "$artist · $album"
        }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("trackId")],
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
)

@Entity(tableName = "history", indices = [Index("trackId")])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackId: Long,
    val playedAt: Long = System.currentTimeMillis(),
)

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val trackCount: Int,
    val coverUri: String?,
    val durationMs: Long,
)

data class ListenStats(
    val plays: Int = 0,
    val uniqueTracks: Int = 0,
    val listenedMs: Long = 0L,
)
