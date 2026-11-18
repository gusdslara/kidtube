package com.kidstv.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class VideoSourceType {
    YOUTUBE,
    LOCAL_FILE
}

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val channelOrAuthor: String? = null,
    val sourceType: VideoSourceType,
    val youtubeId: String? = null,
    val youtubeUrl: String? = null,
    val localFilePath: String? = null,
    val thumbnailPathOrUrl: String? = null,
    val fileSizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val addedFrom: String = "MOBILE",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val channelNumber: Int = 1,
    val iconKey: String = "tv",
    val colorHex: Long = 0xFF38BDF8,
    val lastVideoId: Long? = null,
    val lastPositionMs: Long = 0L,
    val lastWatchedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_videos",
    primaryKeys = ["playlistId", "videoId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("videoId"), Index("playlistId")]
)
data class PlaylistVideoCrossRef(
    val playlistId: Long,
    val videoId: Long,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "watch_history",
    foreignKeys = [
        ForeignKey(
            entity = VideoEntity::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["videoId"], unique = true)]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val videoId: Long,
    val watchedAt: Long = System.currentTimeMillis(),
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L
)

data class PlaylistWithVideos(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistVideoCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "videoId"
        )
    )
    val videos: List<VideoEntity>
)

data class WatchHistoryWithVideo(
    @Embedded val history: WatchHistoryEntity,
    @Relation(
        parentColumn = "videoId",
        entityColumn = "id"
    )
    val video: VideoEntity
)
