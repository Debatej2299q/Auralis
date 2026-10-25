package dev.auralis.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MusicDao {

    // ---------------------------------------------------------------- tracks

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    abstract fun observeTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE favorite = 1 ORDER BY title COLLATE NOCASE ASC")
    abstract fun observeLiked(): Flow<List<Track>>

    @Query("SELECT * FROM tracks ORDER BY addedAt DESC LIMIT 24")
    abstract fun observeRecentlyAdded(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE source = 'DOWNLOAD' ORDER BY addedAt DESC")
    abstract fun observeDownloads(): Flow<List<Track>>

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN (
            SELECT trackId, COUNT(*) AS c, MAX(playedAt) AS lastPlay
            FROM history GROUP BY trackId ORDER BY c DESC, lastPlay DESC LIMIT 20
        ) h ON t.id = h.trackId
        ORDER BY h.c DESC
        """
    )
    abstract fun observeMostPlayed(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    abstract suspend fun findTrack(id: Long): Track?

    @Query("SELECT * FROM tracks WHERE uri = :uri LIMIT 1")
    abstract suspend fun findTrackByUri(uri: String): Track?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTracks(tracks: List<Track>): List<Long>

    @Update
    abstract suspend fun updateTrack(track: Track)

    @Query("UPDATE tracks SET favorite = :favorite WHERE id = :id")
    abstract suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE tracks SET artworkUri = :artwork WHERE id = :id")
    abstract suspend fun setArtwork(id: Long, artwork: String?)

    @Query("UPDATE tracks SET playCount = playCount + 1 WHERE id = :id")
    abstract suspend fun bumpPlayCount(id: Long)

    @Query("DELETE FROM tracks WHERE id = :id")
    abstract suspend fun deleteTrack(id: Long)

    @Query("SELECT COUNT(*) FROM tracks")
    abstract fun observeTrackCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM tracks WHERE source = 'DOWNLOAD'")
    abstract fun observeDownloadBytes(): Flow<Long>

    // ------------------------------------------------------------- playlists

    @Query(
        """
        SELECT p.id AS id, p.name AS name, p.createdAt AS createdAt,
            (SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlistId = p.id) AS trackCount,
            (SELECT t.artworkUri FROM playlist_tracks pt
                INNER JOIN tracks t ON t.id = pt.trackId
                WHERE pt.playlistId = p.id ORDER BY pt.position LIMIT 1) AS coverUri,
            COALESCE((SELECT SUM(t.durationMs) FROM playlist_tracks pt
                INNER JOIN tracks t ON t.id = pt.trackId
                WHERE pt.playlistId = p.id), 0) AS durationMs
        FROM playlists p
        ORDER BY p.createdAt DESC
        """
    )
    abstract fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query(
        """
        SELECT p.id AS id, p.name AS name, p.createdAt AS createdAt,
            (SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlistId = p.id) AS trackCount,
            (SELECT t.artworkUri FROM playlist_tracks pt
                INNER JOIN tracks t ON t.id = pt.trackId
                WHERE pt.playlistId = p.id ORDER BY pt.position LIMIT 1) AS coverUri,
            COALESCE((SELECT SUM(t.durationMs) FROM playlist_tracks pt
                INNER JOIN tracks t ON t.id = pt.trackId
                WHERE pt.playlistId = p.id), 0) AS durationMs
        FROM playlists p WHERE p.id = :id
        """
    )
    abstract fun observePlaylistSummary(id: Long): Flow<PlaylistSummary?>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    abstract suspend fun findPlaylist(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    abstract suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id")
    abstract suspend fun clearPlaylist(id: Long)

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :id
        ORDER BY pt.position ASC
        """
    )
    abstract fun observePlaylistTracks(id: Long): Flow<List<Track>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :id ORDER BY position ASC")
    abstract suspend fun playlistEntries(id: Long): List<PlaylistTrackEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :id")
    abstract suspend fun nextPosition(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertPlaylistTrack(entry: PlaylistTrackEntity): Long

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id AND trackId = :trackId")
    abstract suspend fun removeFromPlaylist(id: Long, trackId: Long)

    @Update
    abstract suspend fun updatePlaylistEntries(entries: List<PlaylistTrackEntity>)

    // --------------------------------------------------------------- history

    @Insert
    abstract suspend fun insertHistory(entry: HistoryEntity)

    @Query("SELECT COUNT(*) FROM history")
    abstract fun observePlayCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT trackId) FROM history")
    abstract fun observeUniqueTracks(): Flow<Int>

    @Query("SELECT COALESCE(SUM(t.durationMs), 0) FROM history h INNER JOIN tracks t ON t.id = h.trackId")
    abstract fun observeListenedMs(): Flow<Long>

    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 1")
    abstract suspend fun lastHistory(): HistoryEntity?

    @Query("DELETE FROM history")
    abstract suspend fun clearHistory()

    // ------------------------------------------------------------ operations

    @Transaction
    open suspend fun appendToPlaylist(playlistId: Long, trackId: Long): Boolean {
        val position = nextPosition(playlistId)
        return insertPlaylistTrack(
            PlaylistTrackEntity(playlistId = playlistId, trackId = trackId, position = position)
        ) != -1L
    }

    @Transaction
    open suspend fun reorderPlaylist(playlistId: Long, orderedTrackIds: List<Long>) {
        clearPlaylist(playlistId)
        orderedTrackIds.forEachIndexed { index, trackId ->
            insertPlaylistTrack(
                PlaylistTrackEntity(playlistId = playlistId, trackId = trackId, position = index)
            )
        }
    }

    @Transaction
    open suspend fun deletePlaylistEverywhere(playlistId: Long) {
        clearPlaylist(playlistId)
        deletePlaylist(playlistId)
    }
}
