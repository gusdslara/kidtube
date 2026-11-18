package com.kidstv.app.util

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YouTubeUrlParser {

    private val PATTERNS = listOf(
        Regex("""(?:youtube\.com/watch\?(?:.*&)?v=)([A-Za-z0-9_-]{11})"""),
        Regex("""(?:youtu\.be/)([A-Za-z0-9_-]{11})"""),
        Regex("""(?:youtube\.com/shorts/)([A-Za-z0-9_-]{11})"""),
        Regex("""(?:youtube\.com/embed/)([A-Za-z0-9_-]{11})"""),
        Regex("""(?:youtube\.com/live/)([A-Za-z0-9_-]{11})"""),
        Regex("""^([A-Za-z0-9_-]{11})$""")
    )

    fun extractVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        for (pattern in PATTERNS) {
            val match = pattern.find(trimmed)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    fun buildWatchUrl(videoId: String): String =
        "https://www.youtube.com/watch?v=$videoId"

    fun buildThumbnailUrl(videoId: String): String =
        "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

    suspend fun fetchMetadata(videoId: String): YouTubeMetadata? = withContext(Dispatchers.IO) {
        val watchUrl = URLEncoder.encode(buildWatchUrl(videoId), "UTF-8")
        val endpoint = "https://www.youtube.com/oembed?url=$watchUrl&format=json"
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
            }
            if (connection.responseCode in 200..299) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val title = json.optString("title").takeIf { it.isNotBlank() }
                val author = json.optString("author_name").takeIf { it.isNotBlank() }
                val thumb = json.optString("thumbnail_url").takeIf { it.isNotBlank() }
                    ?: buildThumbnailUrl(videoId)
                YouTubeMetadata(
                    title = title ?: "Vídeo do YouTube ($videoId)",
                    author = author,
                    thumbnailUrl = thumb
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}

data class YouTubeMetadata(
    val title: String,
    val author: String?,
    val thumbnailUrl: String
)
