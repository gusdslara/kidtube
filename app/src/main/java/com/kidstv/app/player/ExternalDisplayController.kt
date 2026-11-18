package com.kidstv.app.player

import android.app.Presentation
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.provider.Settings
import android.view.Display
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kidstv.app.data.db.VideoEntity
import com.kidstv.app.data.db.VideoSourceType
import com.kidstv.app.ui.theme.KidsTvTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExternalDisplayMonitor(context: Context) {

    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val _externalDisplay = MutableStateFlow(findFirstPresentationDisplay())
    val externalDisplay: StateFlow<Display?> = _externalDisplay.asStateFlow()

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = refresh()
        override fun onDisplayRemoved(displayId: Int) = refresh()
        override fun onDisplayChanged(displayId: Int) = refresh()
    }

    init {
        displayManager.registerDisplayListener(listener, null)
    }

    fun refresh() {
        _externalDisplay.value = findFirstPresentationDisplay()
    }

    fun release() {
        displayManager.unregisterDisplayListener(listener)
    }

    private fun findFirstPresentationDisplay(): Display? =
        displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).firstOrNull()

    companion object {
        fun openMiracastSettings(context: Context): Boolean {
            // Vendors expose different entry points. A missing shortcut does not
            // tell us whether the device supports Miracast.
            val actions = listOf(
                "android.settings.WIFI_DISPLAY_SETTINGS",
                Settings.ACTION_CAST_SETTINGS,
                Settings.ACTION_SETTINGS
            )
            for (action in actions) {
                try {
                    context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return true
                } catch (_: ActivityNotFoundException) {
                    // Try the next settings entry point.
                } catch (_: SecurityException) {
                    // Some vendor screens cannot be launched by other apps.
                }
            }
            return false
        }
    }
}

class TvMirrorPresentation(
    outerContext: Context,
    display: Display,
    private val lifecycleOwner: LifecycleOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val video: VideoEntity?,
    private val initialPositionMs: Long = 0L,
    private val onNextVideo: () -> Unit,
    private val onProgress: (Long, Long) -> Unit,
    private val onEnded: () -> Unit
) : Presentation(outerContext, display) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(context)
        window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(lifecycleOwner)
            decor.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
        }
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            setContent {
                KidsTvTheme {
                    PresentationPlayerContent(
                        video = video,
                        initialPositionMs = initialPositionMs,
                        onNextVideo = onNextVideo,
                        onProgress = onProgress,
                        onEnded = onEnded
                    )
                }
            }
        }
        root.addView(composeView)
        setContentView(root)
    }
}

@Composable
private fun PresentationPlayerContent(
    video: VideoEntity?,
    initialPositionMs: Long,
    onNextVideo: () -> Unit,
    onProgress: (Long, Long) -> Unit,
    onEnded: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (video == null) {
            Text(
                text = "Escolha um vídeo ou canal na lista do celular para assistir na TV",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(24.dp)
            )
        } else if (video.sourceType == VideoSourceType.YOUTUBE && !video.youtubeId.isNullOrBlank()) {
            YouTubePlayerView(
                youtubeId = video.youtubeId,
                initialPositionMs = initialPositionMs,
                kidLockActive = true,
                isFullscreen = true,
                onToggleFullscreen = {},
                onNextVideo = onNextVideo,
                onProgress = onProgress,
                onEnded = onEnded,
                modifier = Modifier.fillMaxSize()
            )
        } else if (video.sourceType == VideoSourceType.LOCAL_FILE && !video.localFilePath.isNullOrBlank()) {
            LocalVideoPlayerView(
                filePath = video.localFilePath,
                initialPositionMs = initialPositionMs,
                isFullscreen = true,
                onToggleFullscreen = {},
                onProgress = onProgress,
                onEnded = onEnded,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
