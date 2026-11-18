package com.kidstv.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<VideoEntity>

    @Query(
        """
        SELECT * FROM videos
        WHERE title LIKE '%' || :query || '%'
           OR COALESCE(channelOrAuthor, '') LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
        """
    )
    fun search(query: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VideoEntity?

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM videos WHERE sourceType = 'LOCAL_FILE'")
    fun observeLocalVideosSizeBytes(): Flow<Long>

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM videos WHERE sourceType = 'LOCAL_FILE'")
    suspend fun getLocalVideosSizeBytesOnce(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: VideoEntity): Long

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY channelNumber ASC, createdAt ASC")
    fun observeAllWithVideos(): Flow<List<PlaylistWithVideos>>

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY channelNumber ASC, createdAt ASC")
    suspend fun getAllWithVideosOnce(): List<PlaylistWithVideos>

    @Query("SELECT * FROM playlists ORDER BY channelNumber ASC, createdAt ASC")
    suspend fun getAllPlaylistsOnce(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Query("SELECT COALESCE(MAX(channelNumber), 0) FROM playlists")
    suspend fun getMaxChannelNumber(): Int

    @Query("SELECT * FROM playlist_videos ORDER BY playlistId ASC, position ASC, addedAt ASC")
    fun observeAllCrossRefs(): Flow<List<PlaylistVideoCrossRef>>

    @Query("SELECT * FROM playlist_videos WHERE playlistId = :playlistId ORDER BY position ASC, addedAt ASC")
    suspend fun getCrossRefsForPlaylist(playlistId: Long): List<PlaylistVideoCrossRef>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_videos WHERE playlistId = :playlistId")
    suspend fun getMaxPositionInPlaylist(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query(
        """
        UPDATE playlists
        SET name = :name,
            description = :description,
            channelNumber = :channelNumber,
            iconKey = :iconKey,
            colorHex = :colorHex
        WHERE id = :playlistId
        """
    )
    suspend fun updateChannelInfo(
        playlistId: Long,
        name: String,
        description: String,
        channelNumber: Int,
        iconKey: String,
        colorHex: Long
    )

    @Query(
        """
        UPDATE playlists
        SET lastVideoId = :videoId,
            lastPositionMs = :positionMs,
            lastWatchedAt = :watchedAt
        WHERE id = :playlistId
        """
    )
    suspend fun updateChannelPlaybackState(
        playlistId: Long,
        videoId: Long?,
        positionMs: Long,
        watchedAt: Long
    )

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addVideoToPlaylist(crossRef: PlaylistVideoCrossRef)

    @Query("UPDATE playlist_videos SET position = :position WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun updateVideoPosition(playlistId: Long, videoId: Long, position: Int)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: Long)
}

@Dao
interface HistoryDao {
    @Transaction
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<WatchHistoryWithVideo>>

    @Query("SELECT * FROM watch_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getForVideo(videoId: Long): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}
