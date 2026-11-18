package com.kidstv.app.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class StorageSnapshot(
    val totalDeviceBytes: Long,
    val freeDeviceBytes: Long,
    val appVideosBytes: Long,
    val quotaBytes: Long,
    val safetyReserveBytes: Long
) {
    val maxUsableByQuotaBytes: Long
        get() = quotaBytes.coerceAtLeast(0L)

    val freeByDeviceSafetyBytes: Long
        get() = (freeDeviceBytes - safetyReserveBytes).coerceAtLeast(0L)

    val remainingAllowedBytes: Long
        get() = minOf(
            (maxUsableByQuotaBytes - appVideosBytes).coerceAtLeast(0L),
            freeByDeviceSafetyBytes
        )

    val usedRatioOfQuota: Float
        get() = if (maxUsableByQuotaBytes <= 0L) 1f
        else (appVideosBytes.toFloat() / maxUsableByQuotaBytes.toFloat()).coerceIn(0f, 1f)
}

data class ImportedLocalVideo(
    val title: String,
    val filePath: String,
    val thumbnailPath: String?,
    val fileSizeBytes: Long,
    val durationMs: Long
)

class DeviceStorageManager(private val context: Context) {

    private val videosDir: File
        get() = File(context.filesDir, "videos").apply { mkdirs() }

    private val thumbsDir: File
        get() = File(context.filesDir, "thumbs").apply { mkdirs() }

    fun getSnapshot(quotaMb: Int, safetyReserveMb: Int = DEFAULT_SAFETY_RESERVE_MB): StorageSnapshot {
        val stat = StatFs(context.filesDir.absolutePath)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedByVideos = videosDir.listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L
        val quotaBytes = quotaMb.toLong() * 1024L * 1024L
        val reserveBytes = safetyReserveMb.toLong() * 1024L * 1024L
        return StorageSnapshot(
            totalDeviceBytes = totalBytes,
            freeDeviceBytes = freeBytes,
            appVideosBytes = usedByVideos,
            quotaBytes = quotaBytes,
            safetyReserveBytes = reserveBytes
        )
    }

    fun canFitNewFile(incomingBytes: Long, quotaMb: Int): Pair<Boolean, String?> {
        val snapshot = getSnapshot(quotaMb)
        if (incomingBytes <= 0L) return true to null
        if (incomingBytes > snapshot.freeByDeviceSafetyBytes) {
            return false to "Espaço insuficiente no aparelho (é necessário manter reserva de segurança de ${DEFAULT_SAFETY_RESERVE_MB} MB livres)."
        }
        if (snapshot.appVideosBytes + incomingBytes > snapshot.maxUsableByQuotaBytes) {
            return false to "O arquivo ultrapassa a cota de espaço configurada pelos pais para vídeos locais."
        }
        return true to null
    }

    suspend fun importFromUri(
        uri: Uri,
        customTitle: String?,
        quotaMb: Int
    ): Result<ImportedLocalVideo> = withContext(Dispatchers.IO) {
        try {
            val (displayName, knownSize) = queryDisplayNameAndSize(uri)
            if (knownSize > 0L) {
                val (fits, reason) = canFitNewFile(knownSize, quotaMb)
                if (!fits) return@withContext Result.failure(IllegalStateException(reason ?: "Sem espaço"))
            }

            val ext = displayName.substringAfterLast('.', "mp4").lowercase()
                .takeIf { it.length in 2..5 && it.all { ch -> ch.isLetterOrDigit() } } ?: "mp4"
            val destFile = File(videosDir, "vid_${UUID.randomUUID()}.$ext")

            var totalWritten = 0L
            val snapshotBefore = getSnapshot(quotaMb)
            val maxAllowed = snapshotBefore.remainingAllowedBytes

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        totalWritten += read
                        if (totalWritten > maxAllowed) {
                            destFile.delete()
                            return@withContext Result.failure(
                                IllegalStateException("Espaço/cota excedidos durante a importação do vídeo.")
                            )
                        }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("Não foi possível ler o arquivo selecionado."))

            val (durationMs, thumbPath) = extractMetadataAndThumb(destFile)
            val resolvedTitle = customTitle?.takeIf { it.isNotBlank() }
                ?: displayName.substringBeforeLast('.').ifBlank { "Vídeo do dispositivo" }

            Result.success(
                ImportedLocalVideo(
                    title = resolvedTitle,
                    filePath = destFile.absolutePath,
                    thumbnailPath = thumbPath,
                    fileSizeBytes = destFile.length(),
                    durationMs = durationMs
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ingestUploadedFile(
        tempFile: File,
        originalFileName: String,
        customTitle: String?,
        quotaMb: Int
    ): Result<ImportedLocalVideo> = withContext(Dispatchers.IO) {
        try {
            val size = tempFile.length()
            val (fits, reason) = canFitNewFile(size, quotaMb)
            if (!fits) {
                tempFile.delete()
                return@withContext Result.failure(IllegalStateException(reason ?: "Sem espaço disponível no aparelho"))
            }

            val ext = originalFileName.substringAfterLast('.', "mp4").lowercase()
                .takeIf { it.length in 2..5 && it.all { ch -> ch.isLetterOrDigit() } } ?: "mp4"
            val destFile = File(videosDir, "vid_${UUID.randomUUID()}.$ext")

            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()

            val (durationMs, thumbPath) = extractMetadataAndThumb(destFile)
            val resolvedTitle = customTitle?.takeIf { it.isNotBlank() }
                ?: originalFileName.substringBeforeLast('.').ifBlank { "Vídeo enviado pelo celular" }

            Result.success(
                ImportedLocalVideo(
                    title = resolvedTitle,
                    filePath = destFile.absolutePath,
                    thumbnailPath = thumbPath,
                    fileSizeBytes = destFile.length(),
                    durationMs = durationMs
                )
            )
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        }
    }

    fun deleteLocalAssets(localFilePath: String?, thumbnailPath: String?) {
        localFilePath?.let { path ->
            runCatching { File(path).takeIf { it.exists() }?.delete() }
        }
        thumbnailPath?.let { path ->
            if (!path.startsWith("http")) {
                runCatching { File(path).takeIf { it.exists() }?.delete() }
            }
        }
    }

    private fun queryDisplayNameAndSize(uri: Uri): Pair<String, Long> {
        var name = "video.mp4"
        var size = -1L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        return name to size
    }

    private fun extractMetadataAndThumb(videoFile: File): Pair<Long, String?> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val frameAtMs = if (durationMs > 4000L) 2000L else (durationMs / 2L).coerceAtLeast(0L)
            val bitmap = retriever.getFrameAtTime(frameAtMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val thumbFile = if (bitmap != null) {
                val out = File(thumbsDir, "${videoFile.nameWithoutExtension}.jpg")
                FileOutputStream(out).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                }
                bitmap.recycle()
                out.absolutePath
            } else {
                null
            }
            durationMs to thumbFile
        } catch (_: Exception) {
            0L to null
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        const val DEFAULT_QUOTA_MB = 4096 // 4 GB
        const val DEFAULT_SAFETY_RESERVE_MB = 512 // 512 MB always kept free on device
    }
}
