package com.kidstv.app.ui.tv

import android.app.Activity
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kidstv.app.data.db.PlaylistEntity
import com.kidstv.app.data.db.VideoSourceType
import com.kidstv.app.data.security.KioskLockController
import com.kidstv.app.player.LocalVideoPlayerView
import com.kidstv.app.player.YouTubePlayerView
import com.kidstv.app.ui.KidsTvViewModel
import com.kidstv.app.ui.components.AddVideoDialog
import com.kidstv.app.ui.components.CableTvGuideContent
import com.kidstv.app.ui.components.ChannelEditorDialog
import com.kidstv.app.ui.components.ChannelLogoBadge
import com.kidstv.app.ui.components.ChannelNumberJumpDialog
import com.kidstv.app.ui.components.FeedVideoCard
import com.kidstv.app.ui.components.HorizontalVideoCard
import com.kidstv.app.ui.components.KidTubeLogo
import com.kidstv.app.ui.components.PinDialog
import com.kidstv.app.ui.components.StorageStatusCard
import com.kidstv.app.ui.components.formatChannelNumber
import com.kidstv.app.ui.theme.KidsColors
import com.kidstv.app.util.Formatters
import kotlinx.coroutines.delay

private enum class TvSection(val label: String) {
    CHANNELS("Modo TV (Canais)"),
    FEED("Início"),
    SEARCH("Buscar"),
    PLAYLISTS("Playlists"),
    HISTORY("Histórico"),
    QR_SETUP("Configurar c/ Celular"),
    PARENTS("Trava e Pais")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TvAppShell(
    viewModel: KidsTvViewModel,
    onPickLocalVideos: (customTitle: String?, playlistId: Long?) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val parental by viewModel.parentalState.collectAsState()
    val videos by viewModel.filteredVideos.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val history by viewModel.history.collectAsState()
    val storage by viewModel.storageSnapshot.collectAsState()
    val currentVideo by viewModel.currentVideo.collectAsState()
    val activeChannelId by viewModel.activeChannelId.collectAsState()
    val initialSeekPos by viewModel.initialSeekPositionMs.collectAsState()
    val activeQueue by viewModel.activeQueue.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val qrState by viewModel.tvQrState.collectAsState()
    val feedback by viewModel.feedbackMessage.collectAsState()

    var currentSection by remember { mutableStateOf(TvSection.CHANNELS) }
    var showBottomQueue by remember { mutableStateOf(true) }
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var pendingSectionAfterUnlock by remember { mutableStateOf<TvSection?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showDialpadDialog by remember { mutableStateOf(false) }
    var typedChannelDigits by remember { mutableStateOf("") }

    val activeChannel = playlists.firstOrNull { it.playlist.id == activeChannelId }

    LaunchedEffect(currentSection) {
        if (currentSection == TvSection.QR_SETUP) {
            viewModel.startTvQrServer()
        }
    }

    // Auto-tune after typing channel digits on TV remote
    LaunchedEffect(typedChannelDigits) {
        if (typedChannelDigits.isNotEmpty()) {
            delay(1100)
            val num = typedChannelDigits.toIntOrNull()
            typedChannelDigits = ""
            if (num != null && num > 0) {
                viewModel.tuneChannelByNumber(num)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopTvQrServer() }
    }

    BackHandler(enabled = currentVideo != null || parental.isKidLockActive) {
        when {
            currentVideo != null -> viewModel.closePlayer()
            parental.isKidLockActive -> showUnlockDialog = true
        }
    }

    val remoteKeyModifier = Modifier.onPreviewKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val native = keyEvent.nativeKeyEvent
        when (native.keyCode) {
            AndroidKeyEvent.KEYCODE_CHANNEL_UP -> {
                viewModel.nextChannel()
                true
            }
            AndroidKeyEvent.KEYCODE_CHANNEL_DOWN -> {
                viewModel.previousChannel()
                true
            }
            in AndroidKeyEvent.KEYCODE_0..AndroidKeyEvent.KEYCODE_9 -> {
                val digit = (native.keyCode - AndroidKeyEvent.KEYCODE_0).toString()
                if (typedChannelDigits.length < 3) typedChannelDigits += digit
                true
            }
            in AndroidKeyEvent.KEYCODE_NUMPAD_0..AndroidKeyEvent.KEYCODE_NUMPAD_9 -> {
                val digit = (native.keyCode - AndroidKeyEvent.KEYCODE_NUMPAD_0).toString()
                if (typedChannelDigits.length < 3) typedChannelDigits += digit
                true
            }
            else -> false
        }
    }

    Box(modifier = Modifier.fillMaxSize().then(remoteKeyModifier)) {
        // TV Fullscreen Player + Cable TV OSD + Bottom Horizontal Programming Strip
        if (currentVideo != null) {
            val video = currentVideo!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (video.sourceType == VideoSourceType.YOUTUBE && !video.youtubeId.isNullOrBlank()) {
                    YouTubePlayerView(
                        youtubeId = video.youtubeId,
                        initialPositionMs = initialSeekPos,
                        kidLockActive = parental.isKidLockActive,
                        isFullscreen = true,
                        onToggleFullscreen = { showBottomQueue = !showBottomQueue },
                        onNextVideo = { viewModel.playNextInQueue() },
                        onProgress = { pos, dur -> viewModel.onVideoProgress(video.id, pos, dur) },
                        onEnded = { viewModel.playNextInQueue() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!video.localFilePath.isNullOrBlank()) {
                    LocalVideoPlayerView(
                        filePath = video.localFilePath,
                        initialPositionMs = initialSeekPos,
                        isFullscreen = true,
                        onToggleFullscreen = { showBottomQueue = !showBottomQueue },
                        onProgress = { pos, dur -> viewModel.onVideoProgress(video.id, pos, dur) },
                        onEnded = { viewModel.playNextInQueue() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Top Cable TV OSD Bar
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (activeChannel != null) {
                                ChannelLogoBadge(
                                    channelNumber = activeChannel.playlist.channelNumber,
                                    iconKey = activeChannel.playlist.iconKey,
                                    colorHex = activeChannel.playlist.colorHex,
                                    size = 44.dp
                                )
                            }
                            Column {
                                Text(
                                    text = if (activeChannel != null) {
                                        "CH ${formatChannelNumber(activeChannel.playlist.channelNumber)} • ${activeChannel.playlist.name}"
                                    } else {
                                        video.title
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = if (activeChannel != null) {
                                        if (initialSeekPos > 1000L) {
                                            "${video.title} • Continuando de ${Formatters.formatDuration(initialSeekPos)}"
                                        } else {
                                            video.title
                                        }
                                    } else {
                                        video.channelOrAuthor ?: "KidTube"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KidsColors.Primary
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (playlists.any { it.videos.isNotEmpty() }) {
                                OutlinedButton(onClick = { viewModel.previousChannel() }) {
                                    Icon(Icons.Rounded.SkipPrevious, contentDescription = null)
                                    Text("Canal -")
                                }
                                OutlinedButton(onClick = { showDialpadDialog = true }) {
                                    Icon(Icons.Rounded.Dialpad, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Nº Canal")
                                }
                                OutlinedButton(onClick = { viewModel.nextChannel() }) {
                                    Text("Canal +")
                                    Icon(Icons.Rounded.SkipNext, contentDescription = null)
                                }
                            }
                            OutlinedButton(onClick = { showBottomQueue = !showBottomQueue }) {
                                Text(if (showBottomQueue) "Ocultar guia" else "Programação")
                            }
                            OutlinedButton(onClick = { viewModel.closePlayer() }) {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Guia")
                            }
                        }
                    }
                }

                // Bottom Horizontal Programming Shelf
                if (showBottomQueue && activeQueue.isNotEmpty()) {
                    Surface(
                        color = Color(0xCC0B0F17),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = if (activeChannel != null) {
                                    "Programação do Canal ${formatChannelNumber(activeChannel.playlist.channelNumber)}"
                                } else {
                                    "Mais para assistir"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = KidsColors.Primary,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(activeQueue, key = { it.id }) { item ->
                                    HorizontalVideoCard(
                                        video = item,
                                        isSelected = item.id == video.id,
                                        onClick = {
                                            if (activeChannel != null) {
                                                viewModel.tuneChannel(activeChannel, explicitVideo = item, explicitStartMs = 0L)
                                            } else {
                                                viewModel.playVideo(item, activeQueue)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // TV Main 10-foot UI
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KidsColors.Background)
            ) {
                Surface(
                    color = KidsColors.Surface,
                    modifier = Modifier
                        .width(236.dp)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            KidTubeLogo(emblemSize = 34.dp)
                        }

                        TvSection.entries.forEach { section ->
                            val icon = when (section) {
                                TvSection.CHANNELS -> Icons.Rounded.LiveTv
                                TvSection.FEED -> Icons.Rounded.Home
                                TvSection.SEARCH -> Icons.Rounded.Search
                                TvSection.PLAYLISTS -> Icons.AutoMirrored.Rounded.PlaylistPlay
                                TvSection.HISTORY -> Icons.Rounded.History
                                TvSection.QR_SETUP -> Icons.Rounded.QrCode2
                                TvSection.PARENTS -> if (parental.isKidLockActive) Icons.Rounded.Lock else Icons.Rounded.Settings
                            }
                            val selected = currentSection == section
                            Button(
                                onClick = {
                                    val needsPin = parental.isKidLockActive &&
                                        (section == TvSection.QR_SETUP || section == TvSection.PARENTS)
                                    if (needsPin) {
                                        pendingSectionAfterUnlock = section
                                        showUnlockDialog = true
                                    } else {
                                        currentSection = section
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) KidsColors.Primary else KidsColors.SurfaceVariant,
                                    contentColor = if (selected) Color(0xFF041E2E) else KidsColors.TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(icon, contentDescription = null)
                                    Text(section.label, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (parental.isKidLockActive) {
                                    pendingSectionAfterUnlock = null
                                    showUnlockDialog = true
                                } else if (!parental.hasPinConfigured) {
                                    showSetupPinDialog = true
                                } else if (activity != null) {
                                    viewModel.activateKidLock(activity)
                                    if (currentSection == TvSection.PARENTS || currentSection == TvSection.QR_SETUP) {
                                        currentSection = TvSection.CHANNELS
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (parental.isKidLockActive) KidsColors.Warning else KidsColors.PrimaryContainer,
                                contentColor = if (parental.isKidLockActive) Color.Black else KidsColors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (parental.isKidLockActive) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (parental.isKidLockActive) "Destravar (PIN)" else "Travar TV")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    if (!feedback.isNullOrBlank()) {
                        Surface(
                            color = KidsColors.PrimaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(feedback!!, color = Color.White)
                                OutlinedButton(onClick = { viewModel.clearFeedback() }) { Text("OK") }
                            }
                        }
                    }

                    when (currentSection) {
                        TvSection.CHANNELS -> {
                            CableTvGuideContent(
                                channels = playlists,
                                activeChannelId = activeChannelId,
                                canManage = !parental.isKidLockActive,
                                onTuneChannel = { ch -> viewModel.tuneChannel(ch) },
                                onTuneProgram = { ch, vid ->
                                    viewModel.tuneChannel(ch, explicitVideo = vid, explicitStartMs = 0L)
                                },
                                onPreviousChannel = { viewModel.previousChannel() },
                                onNextChannel = { viewModel.nextChannel() },
                                onOpenDialpad = { showDialpadDialog = true },
                                onCreateChannel = { showCreateChannelDialog = true },
                                onEditChannel = { pl -> editingChannel = pl },
                                onMoveProgram = { plId, vidId, up -> viewModel.moveVideoInPlaylist(plId, vidId, up) },
                                onRemoveProgram = { plId, vidId -> viewModel.removeVideoFromPlaylist(plId, vidId) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        TvSection.FEED -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Feed de Vídeos", style = MaterialTheme.typography.headlineSmall)
                                if (!parental.isKidLockActive) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedButton(onClick = { currentSection = TvSection.QR_SETUP }) {
                                            Icon(Icons.Rounded.QrCode2, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Configurar pelo Celular (QR)")
                                        }
                                        Button(onClick = { showAddDialog = true }) {
                                            Icon(Icons.Rounded.Add, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Adicionar aqui")
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            if (allVideos.isEmpty()) {
                                TvEmptyState(onOpenQr = { currentSection = TvSection.QR_SETUP })
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 260.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(allVideos, key = { it.id }) { video ->
                                        FeedVideoCard(
                                            video = video,
                                            playlists = playlists,
                                            canManage = !parental.isKidLockActive,
                                            onPlay = { viewModel.playVideo(video, allVideos) },
                                            onAddToPlaylist = { plId -> viewModel.assignVideoToPlaylist(plId, video.id) },
                                            onDelete = { viewModel.deleteVideo(video.id) }
                                        )
                                    }
                                }
                            }
                        }

                        TvSection.SEARCH -> {
                            Text("Pesquisar na TV", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                label = { Text("Digite para filtrar vídeos...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 260.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(videos, key = { it.id }) { video ->
                                    FeedVideoCard(
                                        video = video,
                                        playlists = playlists,
                                        canManage = false,
                                        onPlay = { viewModel.playVideo(video, videos) }
                                    )
                                }
                            }
                        }

                        TvSection.PLAYLISTS -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Playlists / Canais", style = MaterialTheme.typography.headlineSmall)
                                if (!parental.isKidLockActive) {
                                    Button(onClick = { showCreateChannelDialog = true }) { Text("Novo Canal / Playlist") }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(playlists, key = { it.playlist.id }) { pwv ->
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            ChannelLogoBadge(
                                                channelNumber = pwv.playlist.channelNumber,
                                                iconKey = pwv.playlist.iconKey,
                                                colorHex = pwv.playlist.colorHex,
                                                size = 38.dp
                                            )
                                            Text(
                                                text = "CH ${formatChannelNumber(pwv.playlist.channelNumber)} • ${pwv.playlist.name} (${pwv.videos.size})",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            if (pwv.videos.isNotEmpty()) {
                                                OutlinedButton(onClick = { viewModel.tuneChannel(pwv) }) {
                                                    Text("Sintonizar canal")
                                                }
                                            }
                                        }
                                        if (pwv.videos.isEmpty()) {
                                            Text("Sem vídeos nesta programação.", style = MaterialTheme.typography.bodySmall)
                                        } else {
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                items(pwv.videos, key = { it.id }) { video ->
                                                    HorizontalVideoCard(
                                                        video = video,
                                                        isSelected = false,
                                                        onClick = {
                                                            viewModel.tuneChannel(pwv, explicitVideo = video, explicitStartMs = 0L)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        TvSection.HISTORY -> {
                            Text("Histórico de Assistidos", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(14.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 260.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(history, key = { it.history.id }) { item ->
                                    FeedVideoCard(
                                        video = item.video,
                                        playlists = playlists,
                                        canManage = false,
                                        onPlay = { viewModel.playVideo(item.video, allVideos) }
                                    )
                                }
                            }
                        }

                        TvSection.QR_SETUP -> {
                            TvQrConfigurationScreen(
                                hasPinConfigured = parental.hasPinConfigured,
                                qrState = qrState,
                                storage = storage,
                                onRetry = { viewModel.startTvQrServer() }
                            )
                        }

                        TvSection.PARENTS -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Configurações dos Pais (TV)", style = MaterialTheme.typography.headlineSmall)
                                StorageStatusCard(
                                    snapshot = storage,
                                    onAdjustQuotaMb = { viewModel.updateStorageQuotaMb(it) }
                                )
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, KidsColors.Border),
                                    colors = CardDefaults.cardColors(containerColor = KidsColors.Surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Senha e Trava da TV", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = if (KioskLockController.isDeviceOwner(context)) {
                                                "Device Owner ativo: trava completa da Android TV habilitada."
                                            } else {
                                                "Trava por Fixação de Tela (LockTask) + bloqueio do botão Voltar e dos menus dos pais."
                                            },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            OutlinedButton(onClick = { showSetupPinDialog = true }) {
                                                Text(if (parental.hasPinConfigured) "Alterar senha" else "Criar senha")
                                            }
                                            Button(onClick = { activity?.let { viewModel.activateKidLock(it) } }) {
                                                Text("Ativar Trava na TV")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cable TV Numeric Channel Entry OSD (top-right corner when pressing 0-9 on remote)
        if (typedChannelDigits.isNotEmpty()) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, KidsColors.Primary),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(28.dp)
            ) {
                Text(
                    text = "CH ${typedChannelDigits.padStart(2, '-')}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = KidsColors.Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }

    if (showAddDialog) {
        AddVideoDialog(
            playlists = playlists,
            storageSnapshot = storage,
            onDismiss = { showAddDialog = false },
            onAddYouTube = { url, title, plId -> viewModel.addYouTubeVideo(url, title, plId) },
            onPickLocalVideos = { title, plId -> onPickLocalVideos(title, plId) }
        )
    }

    if (showCreateChannelDialog) {
        val suggested = (playlists.maxOfOrNull { it.playlist.channelNumber } ?: 0) + 1
        ChannelEditorDialog(
            existing = null,
            suggestedChannelNumber = suggested,
            onDismiss = { showCreateChannelDialog = false },
            onSave = { name, number, iconKey ->
                viewModel.createPlaylist(name = name, channelNumber = number, iconKey = iconKey)
            }
        )
    }

    editingChannel?.let { target ->
        ChannelEditorDialog(
            existing = target,
            suggestedChannelNumber = target.channelNumber,
            onDismiss = { editingChannel = null },
            onSave = { name, number, iconKey ->
                viewModel.updateChannelInfo(
                    playlistId = target.id,
                    name = name,
                    channelNumber = number,
                    iconKey = iconKey
                )
            }
        )
    }

    if (showDialpadDialog) {
        ChannelNumberJumpDialog(
            channels = playlists,
            onDismiss = { showDialpadDialog = false },
            onTuneNumber = { number -> viewModel.tuneChannelByNumber(number) }
        )
    }

    if (showSetupPinDialog) {
        PinDialog(
            isFirstTimeSetup = !parental.hasPinConfigured,
            requireCurrentPin = parental.hasPinConfigured,
            title = if (parental.hasPinConfigured) "Alterar senha dos pais" else "Criar senha dos pais",
            subtitle = "Esta senha protege a TV e o acesso pelo QR Code do celular.",
            onDismiss = { showSetupPinDialog = false },
            onConfirmSetup = { pin ->
                val ok = viewModel.setupParentPin(pin)
                if (ok) showSetupPinDialog = false
                ok
            },
            onVerifyPin = { viewModel.verifyParentPin(it) }
        )
    }

    if (showUnlockDialog) {
        PinDialog(
            isFirstTimeSetup = false,
            title = "Senha dos pais",
            subtitle = "Digite a senha para destravar a TV.",
            onDismiss = { showUnlockDialog = false; pendingSectionAfterUnlock = null },
            onVerifyPin = { pin ->
                val ok = activity?.let { viewModel.deactivateKidLockWithPin(it, pin) }
                    ?: viewModel.verifyParentPin(pin)
                if (ok) {
                    showUnlockDialog = false
                    pendingSectionAfterUnlock?.let { currentSection = it }
                    pendingSectionAfterUnlock = null
                }
                ok
            }
        )
    }
}

@Composable
private fun TvEmptyState(onOpenQr: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, KidsColors.Border),
        colors = CardDefaults.cardColors(containerColor = KidsColors.Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Nenhum vídeo na TV ainda", style = MaterialTheme.typography.titleLarge)
            Text(
                "Use o celular para enviar vídeos ou colar URLs do YouTube rapidamente via QR Code.",
                style = MaterialTheme.typography.bodyMedium,
                color = KidsColors.TextSecondary
            )
            Button(onClick = onOpenQr) {
                Icon(Icons.Rounded.QrCode2, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Configurar pelo Celular (QR Code)")
            }
        }
    }
}

@Composable
private fun TvQrConfigurationScreen(
    hasPinConfigured: Boolean,
    qrState: com.kidstv.app.ui.TvQrSessionState,
    storage: com.kidstv.app.data.storage.StorageSnapshot,
    onRetry: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 620.dp
        val qrSize = if (wideLayout) 260.dp else 210.dp
        if (wideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvQrCardBox(qrState = qrState, sizeDp = qrSize)
                TvQrInfoColumn(
                    hasPinConfigured = hasPinConfigured,
                    qrState = qrState,
                    storage = storage,
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TvQrCardBox(qrState = qrState, sizeDp = qrSize)
                TvQrInfoColumn(
                    hasPinConfigured = hasPinConfigured,
                    qrState = qrState,
                    storage = storage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TvQrCardBox(qrState: com.kidstv.app.ui.TvQrSessionState, sizeDp: androidx.compose.ui.unit.Dp) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.size(sizeDp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (qrState.qrBitmap != null) {
                Image(
                    bitmap = qrState.qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code de configuração",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                Text(
                    text = qrState.error ?: "Gerando QR Code...",
                    color = Color.Black,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TvQrInfoColumn(
    hasPinConfigured: Boolean,
    qrState: com.kidstv.app.ui.TvQrSessionState,
    storage: com.kidstv.app.data.storage.StorageSnapshot,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Configurar TV pelo Celular", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = if (!hasPinConfigured) {
                "1. Aponte a câmera do celular para o QR Code.\n" +
                    "2. Como é a primeira vez, o celular vai pedir para criar a senha dos pais.\n" +
                    "3. Depois você poderá colar URLs do YouTube, enviar vídeos do celular e gerenciar canais."
            } else {
                "1. Leia o QR Code no celular (mesmo Wi-Fi da TV).\n" +
                    "2. Digite a senha dos pais para abrir o painel.\n" +
                    "3. Adicione vídeos do YouTube ou envie arquivos do celular controlando o espaço da TV."
            },
            style = MaterialTheme.typography.bodyMedium
        )

        if (qrState.url != null) {
            Surface(
                color = KidsColors.Surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, KidsColors.Border)
            ) {
                Text(
                    text = qrState.url,
                    style = MaterialTheme.typography.titleMedium,
                    color = KidsColors.Primary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            Button(onClick = onRetry) { Text("Tentar novamente") }
        }

        StorageStatusCard(snapshot = storage)
        Text(
            text = "Espaço disponível para novos uploads: ${Formatters.formatBytes(storage.remainingAllowedBytes)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
