package com.kidstv.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeUrlParserTest {

    @Test
    fun extractsVideoIdFromStandardWatchUrl() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlParser.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=12s")
        )
    }

    @Test
    fun extractsVideoIdFromShortLinkAndShorts() {
        assertEquals("abc123_XYZ0", YouTubeUrlParser.extractVideoId("https://youtu.be/abc123_XYZ0?si=test"))
        assertEquals("abc123_XYZ0", YouTubeUrlParser.extractVideoId("https://www.youtube.com/shorts/abc123_XYZ0"))
        assertEquals("abc123_XYZ0", YouTubeUrlParser.extractVideoId("https://www.youtube.com/embed/abc123_XYZ0"))
    }

    @Test
    fun rejectsInvalidUrls() {
        assertNull(YouTubeUrlParser.extractVideoId(""))
        assertNull(YouTubeUrlParser.extractVideoId("https://example.com/video.mp4"))
    }
}
