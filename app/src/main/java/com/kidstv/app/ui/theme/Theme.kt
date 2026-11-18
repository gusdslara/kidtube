package com.kidstv.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object KidsColors {
    val Background = Color(0xFF0B0F17)
    val Surface = Color(0xFF131A26)
    val SurfaceVariant = Color(0xFF1C2636)
    val Border = Color(0xFF2B394F)
    val Primary = Color(0xFF38BDF8)
    val PrimaryContainer = Color(0xFF0C4A6E)
    val Secondary = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
}

private val DarkScheme = darkColorScheme(
    primary = KidsColors.Primary,
    onPrimary = Color(0xFF041E2E),
    primaryContainer = KidsColors.PrimaryContainer,
    onPrimaryContainer = KidsColors.TextPrimary,
    secondary = KidsColors.Secondary,
    onSecondary = Color(0xFF042611),
    background = KidsColors.Background,
    onBackground = KidsColors.TextPrimary,
    surface = KidsColors.Surface,
    onSurface = KidsColors.TextPrimary,
    surfaceVariant = KidsColors.SurfaceVariant,
    onSurfaceVariant = KidsColors.TextSecondary,
    outline = KidsColors.Border,
    error = KidsColors.Danger,
    onError = Color.White
)

private val KidsTypography = Typography(
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = KidsColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = KidsColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = KidsColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        color = KidsColors.TextPrimary
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        color = KidsColors.TextSecondary
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
)

@Composable
fun KidsTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = KidsTypography,
        content = content
    )
}
