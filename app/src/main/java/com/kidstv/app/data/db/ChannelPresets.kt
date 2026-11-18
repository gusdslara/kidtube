package com.kidstv.app.data.db

object ChannelPresets {

    data class ChannelIconOption(
        val key: String,
        val label: String,
        val defaultColorHex: Long
    )

    val ICON_OPTIONS: List<ChannelIconOption> = listOf(
        ChannelIconOption("tv", "TV", 0xFF38BDF8),
        ChannelIconOption("star", "Favoritos", 0xFFF59E0B),
        ChannelIconOption("rocket", "Aventura", 0xFF818CF8),
        ChannelIconOption("music", "Música", 0xFFEC4899),
        ChannelIconOption("pets", "Animais", 0xFF22C55E),
        ChannelIconOption("book", "Histórias", 0xFFA855F7),
        ChannelIconOption("palette", "Arte", 0xFFF97316),
        ChannelIconOption("science", "Ciência", 0xFF14B8A6),
        ChannelIconOption("sports", "Esportes", 0xFFEF4444),
        ChannelIconOption("bedtime", "Dormir", 0xFF6366F1)
    )

    fun colorForKey(iconKey: String): Long =
        ICON_OPTIONS.firstOrNull { it.key == iconKey }?.defaultColorHex ?: 0xFF38BDF8
}
