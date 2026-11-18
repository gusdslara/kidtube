package com.kidstv.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kidstv.app.player.TvMirrorPresentation
import com.kidstv.app.ui.KidsTvViewModel
import com.kidstv.app.ui.mobile.MobileAppShell
import com.kidstv.app.ui.theme.KidsTvTheme
import com.kidstv.app.ui.tv.TvAppShell

class MainActivity : ComponentActivity() {

    private val viewModel: KidsTvViewModel by viewModels()

    private var pendingImportTitle: String? = null
    private var pendingImportPlaylistId: Long? = null
    private var activePresentation: TvMirrorPresentation? = null

    private val pickVideosLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importLocalVideos(
                uris = uris,
                customTitle = pendingImportTitle,
                playlistId = pendingImportPlaylistId
            )
        }
        pendingImportTitle = null
        pendingImportPlaylistId = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KidsTvTheme {
                val parental by viewModel.parentalState.collectAsState()
                val externalDisplay by viewModel.externalDisplay.collectAsState()
                val currentVideo by viewModel.currentVideo.collectAsState()
                val initialSeekPos by viewModel.initialSeekPositionMs.collectAsState()
                val isFullscreen by viewModel.isFullscreen.collectAsState()

                // Immersive bars when Kid Lock or Fullscreen is active
                LaunchedEffect(parental.isKidLockActive, isFullscreen) {
                    applySystemBarsVisibility(hide = parental.isKidLockActive || isFullscreen)
                }

                // Manage Presentation window when Miracast / External Display is connected + Force TV mode is ON
                LaunchedEffect(externalDisplay, currentVideo, initialSeekPos, parental.forceTvListOnlyMode) {
                    activePresentation?.dismiss()
                    activePresentation = null

                    val display = externalDisplay
                    if (!viewModel.isAndroidTv && display != null && parental.forceTvListOnlyMode) {
                        val pres = TvMirrorPresentation(
                            outerContext = this@MainActivity,
                            display = display,
                            lifecycleOwner = this@MainActivity,
                            savedStateRegistryOwner = this@MainActivity,
                            video = currentVideo,
                            initialPositionMs = initialSeekPos,
                            onNextVideo = { viewModel.playNextInQueue() },
                            onProgress = { pos, dur ->
                                currentVideo?.let { viewModel.onVideoProgress(it.id, pos, dur) }
                            },
                            onEnded = { viewModel.playNextInQueue() }
                        )
                        runCatching {
                            pres.show()
                            activePresentation = pres
                        }
                    }
                }

                val pickHandler: (String?, Long?) -> Unit = { title, plId ->
                    pendingImportTitle = title
                    pendingImportPlaylistId = plId
                    pickVideosLauncher.launch(arrayOf("video/*"))
                }

                if (viewModel.isAndroidTv) {
                    TvAppShell(viewModel = viewModel, onPickLocalVideos = pickHandler)
                } else {
                    MobileAppShell(viewModel = viewModel, onPickLocalVideos = pickHandler)
                }
            }
        }
    }

    override fun onPause() {
        viewModel.flushChannelProgressNow()
        super.onPause()
    }

    override fun onDestroy() {
        activePresentation?.dismiss()
        activePresentation = null
        super.onDestroy()
    }

    private fun applySystemBarsVisibility(hide: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hide) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
