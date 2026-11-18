package com.kidstv.app.player

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kidstv.app.util.Formatters

class YouTubeJsBridge(
    private val onProgress: (posMs: Long, durationMs: Long) -> Unit,
    private val onStateChanged: (isPlaying: Boolean) -> Unit,
    private val onError: (errorCode: Int) -> Unit,
    private val onEnded: () -> Unit
) {
    @JavascriptInterface
    fun onPlayerState(stateCode: Int) {
        when (stateCode) {
            1 -> onStateChanged(true)
            2, -1 -> onStateChanged(false)
            0 -> {
                onStateChanged(false)
                onEnded()
            }
        }
    }

    @JavascriptInterface
    fun onTimeUpdate(currentSec: Double, durationSec: Double) {
        onProgress((currentSec * 1000.0).toLong(), (durationSec * 1000.0).toLong())
    }

    @JavascriptInterface
    fun onPlayerError(code: Int) {
        onError(code)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(
    youtubeId: String,
    initialPositionMs: Long = 0L,
    kidLockActive: Boolean,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onNextVideo: () -> Unit,
    onProgress: (posMs: Long, durationMs: Long) -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember(youtubeId, initialPositionMs) { mutableStateOf(true) }
    var currentMs by remember(youtubeId, initialPositionMs) { mutableLongStateOf(initialPositionMs) }
    var durationMs by remember(youtubeId, initialPositionMs) { mutableLongStateOf(0L) }
    var playerErrorCode by remember(youtubeId, initialPositionMs) { mutableStateOf<Int?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var showOverlayControls by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val startSeconds = remember(youtubeId, initialPositionMs) {
        (initialPositionMs / 1000L).toInt().coerceAtLeast(0)
    }

    val bridge = remember(youtubeId, initialPositionMs) {
        YouTubeJsBridge(
            onProgress = { pos, dur ->
                currentMs = pos
                durationMs = dur
                onProgress(pos, dur)
            },
            onStateChanged = { playing -> isPlaying = playing },
            onError = { code -> playerErrorCode = code },
            onEnded = onEnded
        )
    }

    LaunchedEffect(youtubeId, startSeconds, reloadNonce, webViewRef) {
        val wv = webViewRef ?: return@LaunchedEffect
        playerErrorCode = null
        wv.loadDataWithBaseURL(
            "https://www.youtube.com",
            buildYouTubeIframeHtml(youtubeId, startSeconds),
            "text/html",
            "utf-8",
            null
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webViewRef = null
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val uri = request?.url
                            val host = request?.url?.host.orEmpty()
                            if (uri?.scheme == "intent") {
                                return true
                            }
                            val allowed = host == "youtube.com" || host.endsWith(".youtube.com") ||
                                host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com") ||
                                host == "googlevideo.com" || host.endsWith(".googlevideo.com")
                            if (!allowed) return true
                            // Keep the top-level player embedded while allowing the iframe itself to play.
                            return request?.isForMainFrame == true && uri?.path?.startsWith("/watch") == true
                        }
                    }
                    addJavascriptInterface(bridge, "KidsTvBridge")
                    webViewRef = this
                }
            }
        )

        // Kid-safe touch shield over iframe so taps never jump to YouTube suggestions or external links
        if (kidLockActive && playerErrorCode == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showOverlayControls = !showOverlayControls
                    }
            )
        }

        if (playerErrorCode != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE60B0F17))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (playerErrorCode == 101 || playerErrorCode == 150 || playerErrorCode == 153) {
                            "Este vídeo bloqueou reprodução fora do YouTube (código $playerErrorCode)."
                        } else {
                            "Não foi possível carregar o vídeo do YouTube (código $playerErrorCode)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { reloadNonce++ }) {
                            Text("Tentar de novo", color = Color.White)
                        }
                        Button(onClick = onNextVideo) {
                            Text("Próximo")
                        }
                    }
                }
            }
        } else if (showOverlayControls || kidLockActive) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val cmd = if (isPlaying) "pauseVideo()" else "playVideo()"
                                webViewRef?.evaluateJavascript("if(player){player.$cmd;}", null)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onNextVideo) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Próximo vídeo",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "${Formatters.formatDuration(currentMs)} / ${Formatters.formatDuration(durationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    IconButton(onClick = onToggleFullscreen) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                            contentDescription = "Tela cheia",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun buildYouTubeIframeHtml(videoId: String, startSeconds: Int): String = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<meta name="referrer" content="strict-origin-when-cross-origin">
<style>
  html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
  #player { width: 100%; height: 100%; }
</style>
</head>
<body>
<div id="player"></div>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
  var player;
  function onYouTubeIframeAPIReady() {
    player = new YT.Player('player', {
      videoId: '$videoId',
      playerVars: {
        autoplay: 1,
        playsinline: 1,
        enablejsapi: 1,
        origin: 'https://www.youtube.com',
        start: $startSeconds,
        rel: 0,
        modestbranding: 1,
        fs: 0,
        iv_load_policy: 3
      },
      events: {
        onReady: function(e) {
          if ($startSeconds > 0) e.target.seekTo($startSeconds, true);
          e.target.playVideo();
        },
        onStateChange: function(e) {
          if (window.KidsTvBridge) window.KidsTvBridge.onPlayerState(e.data);
        },
        onError: function(e) {
          if (window.KidsTvBridge) window.KidsTvBridge.onPlayerError(e.data || -1);
        }
      }
    });
  }
  setInterval(function() {
    if (player && player.getCurrentTime && player.getDuration && window.KidsTvBridge) {
      window.KidsTvBridge.onTimeUpdate(player.getCurrentTime() || 0, player.getDuration() || 0);
    }
  }, 1500);
</script>
</body>
</html>
""".trimIndent()
