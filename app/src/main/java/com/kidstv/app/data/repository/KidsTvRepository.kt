package com.kidstv.app.data.repository

import android.net.Uri
import com.kidstv.app.data.db.ChannelPresets
import com.kidstv.app.data.db.HistoryDao
import com.kidstv.app.data.db.PlaylistDao
import com.kidstv.app.data.db.PlaylistEntity
import com.kidstv.app.data.db.PlaylistVideoCrossRef
import com.kidstv.app.data.db.PlaylistWithVideos
import com.kidstv.app.data.db.VideoDao
import com.kidstv.app.data.db.VideoEntity
import com.kidstv.app.data.db.VideoSourceType
import com.kidstv.app.data.db.WatchHistoryEntity
import com.kidstv.app.data.db.WatchHistoryWithVideo
import com.kidstv.app.data.security.ParentalPrefs
import com.kidstv.app.data.storage.DeviceStorageManager
import com.kidstv.app.data.storage.StorageSnapshot
import com.kidstv.app.util.YouTubeUrlParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.File

class KidsTvRepository(
    private val videoDao: VideoDao,
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao,
    val storageManager: DeviceStorageManager,
    val parentalPrefs: ParentalPrefs
) {

    val allVideos: Flow<List<VideoEntity>> = videoDao.observeAll()

    val allPlaylists: Flow<List<PlaylistWithVideos>> = combine(
        playlistDao.observeAllWithVideos(),
        playlistDao.observeAllCrossRefs()
    ) { playlists, refs ->
        val refsByPlaylist = refs.groupBy { it.playlistId }
        playlists.map { pwv ->
            val orderMap = refsByPlaylist[pwv.playlist.id]
                ?.associate { it.videoId to it.position }
                .orEmpty()
            val sortedVideos = pwv.videos.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
            pwv.copy(videos = sortedVideos)
        }
    }

    val recentHistory: Flow<List<WatchHistoryWithVideo>> = historyDao.observeRecent()
    val localVideosSizeBytes: Flow<Long> = videoDao.observeLocalVideosSizeBytes()

    fun searchVideos(query: String): Flow<List<VideoEntity>> =
        if (query.isBlank()) videoDao.observeAll() else videoDao.search(query.trim())

    fun getStorageSnapshot(): StorageSnapshot =
        storageManager.getSnapshot(parentalPrefs.state.value.storageQuotaMb)

    suspend fun getVideoById(id: Long): VideoEntity? = videoDao.getById(id)

    suspend fun getAllVideosOnce(): List<VideoEntity> = videoDao.getAllOnce()

    suspend fun getAllPlaylistsOnce(): List<PlaylistWithVideos> {
        val playlists = playlistDao.getAllWithVideosOnce()
        return playlists.map { pwv ->
            val refs = playlistDao.getCrossRefsForPlaylist(pwv.playlist.id)
            val orderMap = refs.associate { it.videoId to it.position }
            pwv.copy(videos = pwv.videos.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE })
        }
    }

    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity? =
        playlistDao.getPlaylistById(playlistId)

    suspend fun addYouTubeVideo(
        rawUrl: String,
        customTitle: String? = null,
        playlistId: Long? = null,
        addedFrom: String = "MOBILE"
    ): Result<VideoEntity> {
        val videoId = YouTubeUrlParser.extractVideoId(rawUrl)
            ?: return Result.failure(IllegalArgumentException("URL ou ID do YouTube inválido."))

        val metadata = YouTubeUrlParser.fetchMetadata(videoId)
        val resolvedTitle = customTitle?.trim()?.takeIf { it.isNotEmpty() }
            ?: metadata?.title
            ?: "Vídeo YouTube ($videoId)"

        val entity = VideoEntity(
            title = resolvedTitle,
            channelOrAuthor = metadata?.author ?: "YouTube",
            sourceType = VideoSourceType.YOUTUBE,
            youtubeId = videoId,
            youtubeUrl = YouTubeUrlParser.buildWatchUrl(videoId),
            thumbnailPathOrUrl = metadata?.thumbnailUrl ?: YouTubeUrlParser.buildThumbnailUrl(videoId),
            addedFrom = addedFrom
        )
        val insertedId = videoDao.insert(entity)
        val saved = entity.copy(id = insertedId)
        if (playlistId != null) {
            addVideoToPlaylist(playlistId, insertedId)
        }
        return Result.success(saved)
    }

    suspend fun addLocalVideoFromUri(
        uri: Uri,
        customTitle: String? = null,
        playlistId: Long? = null,
        addedFrom: String = "MOBILE"
    ): Result<VideoEntity> {
        val quotaMb = parentalPrefs.state.value.storageQuotaMb
        val importedResult = storageManager.importFromUri(uri, customTitle, quotaMb)
        return importedResult.mapCatching { imported ->
            val entity = VideoEntity(
                title = imported.title,
                channelOrAuthor = "Dispositivo",
                sourceType = VideoSourceType.LOCAL_FILE,
                localFilePath = imported.filePath,
                thumbnailPathOrUrl = imported.thumbnailPath,
                fileSizeBytes = imported.fileSizeBytes,
                durationMs = imported.durationMs,
                addedFrom = addedFrom
            )
            val insertedId = videoDao.insert(entity)
            val saved = entity.copy(id = insertedId)
            if (playlistId != null) {
                addVideoToPlaylist(playlistId, insertedId)
            }
            saved
        }
    }

    suspend fun addUploadedVideoFile(
        tempFile: File,
        originalFileName: String,
        customTitle: String?,
        playlistId: Long?
    ): Result<VideoEntity> {
        val quotaMb = parentalPrefs.state.value.storageQuotaMb
        val importedResult = storageManager.ingestUploadedFile(tempFile, originalFileName, customTitle, quotaMb)
        return importedResult.mapCatching { imported ->
            val entity = VideoEntity(
                title = imported.title,
                channelOrAuthor = "Enviado pelo celular",
                sourceType = VideoSourceType.LOCAL_FILE,
                localFilePath = imported.filePath,
                thumbnailPathOrUrl = imported.thumbnailPath,
                fileSizeBytes = imported.fileSizeBytes,
                durationMs = imported.durationMs,
                addedFrom = "TV_QR_WEB"
            )
            val insertedId = videoDao.insert(entity)
            val saved = entity.copy(id = insertedId)
            if (playlistId != null) {
                addVideoToPlaylist(playlistId, insertedId)
            }
            saved
        }
    }

    suspend fun deleteVideo(videoId: Long) {
        val existing = videoDao.getById(videoId)
        if (existing != null) {
            storageManager.deleteLocalAssets(existing.localFilePath, existing.thumbnailPathOrUrl)
            videoDao.deleteById(videoId)
        }
    }

    suspend fun createPlaylist(
        name: String,
        description: String = "",
        channelNumber: Int? = null,
        iconKey: String = "tv"
    ): Long? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        val nextNumber = channelNumber?.takeIf { it > 0 } ?: (playlistDao.getMaxChannelNumber() + 1)
        val colorHex = ChannelPresets.colorForKey(iconKey)
        return playlistDao.insertPlaylist(
            PlaylistEntity(
                name = clean,
                description = description.trim(),
                channelNumber = nextNumber,
                iconKey = iconKey,
                colorHex = colorHex
            )
        )
    }

    suspend fun updateChannelInfo(
        playlistId: Long,
        name: String,
        description: String = "",
        channelNumber: Int,
        iconKey: String
    ) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val colorHex = ChannelPresets.colorForKey(iconKey)
        playlistDao.updateChannelInfo(
            playlistId = playlistId,
            name = clean,
            description = description.trim(),
            channelNumber = channelNumber.coerceAtLeast(1),
            iconKey = iconKey,
            colorHex = colorHex
        )
    }

    suspend fun saveChannelPlaybackState(
        playlistId: Long,
        videoId: Long?,
        positionMs: Long
    ) {
        playlistDao.updateChannelPlaybackState(
            playlistId = playlistId,
            videoId = videoId,
            positionMs = positionMs.coerceAtLeast(0L),
            watchedAt = System.currentTimeMillis()
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addVideoToPlaylist(playlistId: Long, videoId: Long) {
        val nextPos = playlistDao.getMaxPositionInPlaylist(playlistId) + 1
        playlistDao.addVideoToPlaylist(
            PlaylistVideoCrossRef(playlistId = playlistId, videoId = videoId, position = nextPos)
        )
    }

    suspend fun moveVideoInPlaylist(playlistId: Long, videoId: Long, moveUp: Boolean) {
        val refs = playlistDao.getCrossRefsForPlaylist(playlistId)
        if (refs.size < 2) return
        // Normalize positions first to 0..N-1
        val normalized = refs.mapIndexed { idx, ref -> ref.copy(position = idx) }.toMutableList()
        val currentIdx = normalized.indexOfFirst { it.videoId == videoId }
        if (currentIdx < 0) return
        val targetIdx = if (moveUp) currentIdx - 1 else currentIdx + 1
        if (targetIdx !in normalized.indices) return
        val swapped = normalized[targetIdx]
        normalized[targetIdx] = normalized[currentIdx].copy(position = targetIdx)
        normalized[currentIdx] = swapped.copy(position = currentIdx)
        normalized.forEach { ref ->
            playlistDao.updateVideoPosition(playlistId, ref.videoId, ref.position)
        }
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoId: Long) {
        playlistDao.removeVideoFromPlaylist(playlistId, videoId)
    }

    suspend fun recordWatchProgress(videoId: Long, positionMs: Long, durationMs: Long) {
        val existing = historyDao.getForVideo(videoId)
        val entry = WatchHistoryEntity(
            id = existing?.id ?: 0L,
            videoId = videoId,
            watchedAt = System.currentTimeMillis(),
            lastPositionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(existing?.durationMs ?: 0L)
        )
        historyDao.upsert(entry)
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }
}
