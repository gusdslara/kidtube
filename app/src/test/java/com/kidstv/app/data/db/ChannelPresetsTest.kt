package com.kidstv.app.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelPresetsTest {

    @Test
    fun resolvesColorsForKnownAndFallbackIconKeys() {
        assertTrue(ChannelPresets.ICON_OPTIONS.isNotEmpty())
        assertEquals(0xFFF59E0B, ChannelPresets.colorForKey("star"))
        assertEquals(0xFF38BDF8, ChannelPresets.colorForKey("unknown_key"))
    }
}
