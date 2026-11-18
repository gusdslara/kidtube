package com.kidstv.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun formatsDurationProperly() {
        assertEquals("0:00", Formatters.formatDuration(0L))
        assertEquals("1:05", Formatters.formatDuration(65_000L))
        assertEquals("1:02:03", Formatters.formatDuration(3723_000L))
    }

    @Test
    fun formatsBytesProperly() {
        assertEquals("0 MB", Formatters.formatBytes(0L))
        assertEquals("512 KB", Formatters.formatBytes(512 * 1024L))
    }
}
