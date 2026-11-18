package com.kidstv.app.ui.mobile

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kidstv.app.data.db.PlaylistEntity
import com.kidstv.app.data.db.PlaylistWithVideos
import com.kidstv.app.data.db.VideoEntity
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
import com.kidstv.app.ui.components.ChannelZapperBar
import com.kidstv.app.ui.components.FeedVideoCard
import com.kidstv.app.ui.components.HorizontalVideoCard
import com.kidstv.app.ui.components.KidTubeEmblem
import com.kidstv.app.ui.components.KidTubeLogo
import com.kidstv.app.ui.components.PinDialog
import com.kidstv.app.ui.components.StorageStatusCard
import com.kidstv.app.ui.components.formatChannelNumber
import com.kidstv.app.ui.theme.KidsColors
import com.kidstv.app.util.Formatters

private enum class MobileTab(val label: String) {
    FEED("Início"),
    CHANNELS("Canais"),
    PLAYLISTS("Listas"),
    HISTORY("Histórico"),
    PARENTS("Pais")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileAppShell(
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
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val externalDisplay by viewModel.externalDisplay.collectAsState()
    val discoveredTvs by viewModel.discoveredTvs.collectAsState()
    val feedback by viewModel.feedbackMessage.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    var currentTab by remember { mutableStateOf(MobileTab.FEED) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var showDialpadDialog by remember { mutableStateOf(false) }
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var pendingTabAfterUnlock by remember { mutableStateOf<MobileTab?>(null) }
    var showPairTvDialog by remember { mutableStateOf(false) }
    var showMirrorGuide by remember { mutableStateOf(false) }
    var mirrorSettingsUnavailable by remember { mutableStateOf(false) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }

    val activeChannel = playlists.firstOrNull { it.playlist.id == activeChannelId }
    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current
    val isLandscapeOrTablet = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        configuration.screenWidthDp >= 600

    LaunchedEffect(feedback) {
        val msg = feedback
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(externalDisplay) {
        if (externalDisplay != null && showMirrorGuide) {
            showMirrorGuide = false
            snackbarHostState.showSnackbar("TV conectada. Escolha um vídeo para assistir na tela grande.")
        }
    }

    LaunchedEffect(showMirrorGuide) {
        if (showMirrorGuide) mirrorSettingsUnavailable = false
    }

    BackHandler(enabled = parental.isKidLockActive || isFullscreen || currentVideo != null) {
        when {
            isFullscreen -> viewModel.setFullscreen(false)
            currentVideo != null -> viewModel.closePlayer()
            parental.isKidLockActive -> showUnlockDialog = true
        }
    }

    val playingOnPhone = currentVideo != null && !parental.forceTvListOnlyMode
    if (playingOnPhone && isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            MobileVideoPlayerBox(
                video = currentVideo!!,
                initialPositionMs = initialSeekPos,
                kidLockActive = parental.isKidLockActive,
                isFullscreen = true,
                onToggleFullscreen = { viewModel.setFullscreen(false) },
                onNextVideo = { viewModel.playNextInQueue() },
                onProgress = { pos, dur -> viewModel.onVideoProgress(currentVideo!!.id, pos, dur) },
                onEnded = { viewModel.playNextInQueue() },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Scaffold(
        containerColor = KidsColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = KidsColors.Surface),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KidTubeLogo(emblemSize = 28.dp)
                            if (parental.forceTvListOnlyMode) {
                                Surface(
                                    color = KidsColors.PrimaryContainer,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "Só TV",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KidsColors.Primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = "Pesquisar"
                            )
                        }
                        IconButton(onClick = { showMirrorGuide = true }) {
                            Icon(
                                imageVector = if (externalDisplay != null) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
                                contentDescription = "Conectar e espelhar na TV",
                                tint = if (externalDisplay != null) KidsColors.Secondary else KidsColors.TextPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                if (parental.isKidLockActive) {
                                    pendingTabAfterUnlock = null
                                    showUnlockDialog = true
                                } else if (!parental.hasPinConfigured) {
                                    showSetupPinDialog = true
                                } else if (activity != null) {
                                    viewModel.activateKidLock(activity)
                                    if (currentTab == MobileTab.PARENTS) currentTab = MobileTab.FEED
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (parental.isKidLockActive) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = if (parental.isKidLockActive) "Desbloquear" else "Travar modo criança",
                                tint = if (parental.isKidLockActive) KidsColors.Warning else KidsColors.TextPrimary
                            )
                        }
                    }
                )

                if (showSearchBar) {
                    Surface(color = KidsColors.Surface) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Buscar vídeos no catálogo...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Limpar")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!parental.isKidLockActive &&
                (currentTab == MobileTab.FEED || currentTab == MobileTab.PLAYLISTS || currentTab == MobileTab.CHANNELS)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (currentTab == MobileTab.PLAYLISTS || currentTab == MobileTab.CHANNELS) {
                            showCreateChannelDialog = true
                        } else {
                            showAddDialog = true
                        }
                    },
                    containerColor = KidsColors.Primary,
                    contentColor = Color(0xFF041E2E)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Adicionar")
                }
            }
        },
        bottomBar = {
            if (!isLandscapeOrTablet) {
                NavigationBar(containerColor = KidsColors.Surface) {
                    MobileTab.entries.forEach { tab ->
                        val icon = when (tab) {
                            MobileTab.FEED -> Icons.Rounded.Home
                            MobileTab.CHANNELS -> Icons.Rounded.LiveTv
                            MobileTab.PLAYLISTS -> Icons.AutoMirrored.Rounded.PlaylistPlay
                            MobileTab.HISTORY -> Icons.Rounded.History
                            MobileTab.PARENTS -> Icons.Rounded.Settings
                        }
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = {
                                if (tab == MobileTab.PARENTS && parental.isKidLockActive) {
                                    pendingTabAfterUnlock = MobileTab.PARENTS
                                    showUnlockDialog = true
                                } else {
                                    currentTab = tab
                                }
                            },
                            icon = { Icon(icon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    text = tab.label,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLandscapeOrTablet) {
                NavigationRail(
                    containerColor = KidsColors.Surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    MobileTab.entries.forEach { tab ->
                        val icon = when (tab) {
                            MobileTab.FEED -> Icons.Rounded.Home
                            MobileTab.CHANNELS -> Icons.Rounded.LiveTv
                            MobileTab.PLAYLISTS -> Icons.AutoMirrored.Rounded.PlaylistPlay
                            MobileTab.HISTORY -> Icons.Rounded.History
                            MobileTab.PARENTS -> Icons.Rounded.Settings
                        }
                        NavigationRailItem(
                            selected = currentTab == tab,
                            onClick = {
                                if (tab == MobileTab.PARENTS && parental.isKidLockActive) {
                                    pendingTabAfterUnlock = MobileTab.PARENTS
                                    showUnlockDialog = true
                                } else {
                                    currentTab = tab
                                }
                            },
                            icon = { Icon(icon, contentDescription = tab.label) },
                            label = { Text(tab.label, maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (parental.forceTvListOnlyMode) {
                    ForceTvBanner(
                        hasExternalDisplay = externalDisplay != null,
                        pairedTvUrl = parental.pairedTvBaseUrl,
                        playingTitle = currentVideo?.title,
                        onOpenMiracast = { showMirrorGuide = true }
                    )
                }

                if (playingOnPhone && currentVideo != null && isLandscapeOrTablet) {
                    // Wide / Landscape split-pane Watch Layout: Player on left, Queue + Content on right
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.25f)
                                .fillMaxHeight()
                                .background(KidsColors.Surface, RoundedCornerShape(12.dp))
                        ) {
                            MobileVideoPlayerBox(
                                video = currentVideo!!,
                                initialPositionMs = initialSeekPos,
                                kidLockActive = parental.isKidLockActive,
                                isFullscreen = false,
                                onToggleFullscreen = { viewModel.setFullscreen(true) },
                                onNextVideo = { viewModel.playNextInQueue() },
                                onProgress = { pos, dur -> viewModel.onVideoProgress(currentVideo!!.id, pos, dur) },
                                onEnded = { viewModel.playNextInQueue() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            if (activeChannel != null) {
                                ChannelZapperBar(
                                    activeChannel = activeChannel,
                                    resumedFromMs = initialSeekPos,
                                    onPreviousChannel = { viewModel.previousChannel() },
                                    onNextChannel = { viewModel.nextChannel() },
                                    onOpenDialpad = { showDialpadDialog = true },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = currentVideo!!.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.closePlayer() }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Fechar player")
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            val queue = activeQueue.ifEmpty { allVideos }
                            Text(
                                text = if (activeChannel != null) "Programação do canal" else "Próximos vídeos",
                                style = MaterialTheme.typography.labelMedium,
                                color = KidsColors.Primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(queue, key = { it.id }) { item ->
                                    FeedVideoCard(
                                        video = item,
                                        playlists = playlists,
                                        canManage = false,
                                        onPlay = {
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
                } else {
                    if (playingOnPhone && currentVideo != null) {
                    MobileWatchSection(
                        video = currentVideo!!,
                        initialPositionMs = initialSeekPos,
                        activeChannel = activeChannel,
                        queue = activeQueue.ifEmpty { allVideos },
                        kidLockActive = parental.isKidLockActive,
                        onClose = { viewModel.closePlayer() },
                        onSelectVideo = { next ->
                            if (activeChannel != null) {
                                viewModel.tuneChannel(activeChannel, explicitVideo = next, explicitStartMs = 0L)
                            } else {
                                viewModel.playVideo(next, activeQueue)
                            }
                        },
                        onPreviousChannel = { viewModel.previousChannel() },
                        onNextChannel = { viewModel.nextChannel() },
                        onOpenDialpad = { showDialpadDialog = true },
                        onToggleFullscreen = { viewModel.setFullscreen(true) },
                        onNextVideo = { viewModel.playNextInQueue() },
                        onProgress = { pos, dur -> viewModel.onVideoProgress(currentVideo!!.id, pos, dur) }
                    )
                } else if (parental.forceTvListOnlyMode && externalDisplay != null && activeQueue.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        if (activeChannel != null) {
                            ChannelZapperBar(
                                activeChannel = activeChannel,
                                resumedFromMs = initialSeekPos,
                                onPreviousChannel = { viewModel.previousChannel() },
                                onNextChannel = { viewModel.nextChannel() },
                                onOpenDialpad = { showDialpadDialog = true },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "Próximos na TV",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(activeQueue, key = { it.id }) { item ->
                                HorizontalVideoCard(
                                    video = item,
                                    isSelected = currentVideo?.id == item.id,
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

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        MobileTab.FEED -> {
                            FeedTabContent(
                                videos = videos,
                                playlists = playlists,
                                canManage = !parental.isKidLockActive,
                                onPlay = { video -> viewModel.playVideo(video, videos) },
                                onAddToPlaylist = { plId, vidId -> viewModel.assignVideoToPlaylist(plId, vidId) },
                                onDelete = { vidId -> viewModel.deleteVideo(vidId) },
                                onAddFirstVideo = { showAddDialog = true }
                            )
                        }

                        MobileTab.CHANNELS -> {
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

                        MobileTab.PLAYLISTS -> {
                            PlaylistsTabContent(
                                playlists = playlists,
                                selectedPlaylistId = selectedPlaylistId,
                                canManage = !parental.isKidLockActive,
                                onSelectPlaylist = { selectedPlaylistId = it },
                                onTuneChannel = { pwv -> viewModel.tuneChannel(pwv) },
                                onPlayProgram = { pwv, vid ->
                                    viewModel.tuneChannel(pwv, explicitVideo = vid, explicitStartMs = 0L)
                                },
                                onEditChannel = { pl -> editingChannel = pl },
                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                onCreatePlaylist = { showCreateChannelDialog = true }
                            )
                        }

                        MobileTab.HISTORY -> {
                            HistoryTabContent(
                                history = history,
                                canClear = !parental.isKidLockActive,
                                onPlay = { video -> viewModel.playVideo(video, allVideos) },
                                onClearHistory = { viewModel.clearHistory() }
                            )
                        }

                        MobileTab.PARENTS -> {
                            ParentsTabContent(
                                hasPin = parental.hasPinConfigured,
                                isKidLockActive = parental.isKidLockActive,
                                forceTvListOnly = parental.forceTvListOnlyMode,
                                hasExternalDisplay = externalDisplay != null,
                                pairedTvUrl = parental.pairedTvBaseUrl,
                                storage = storage,
                                isDeviceOwner = KioskLockController.isDeviceOwner(context),
                                onOpenAddVideo = { showAddDialog = true },
                                onOpenCreatePlaylist = { showCreateChannelDialog = true },
                                onConfigurePin = { showSetupPinDialog = true },
                                onActivateKidLock = { activity?.let { viewModel.activateKidLock(it) } },
                                onToggleForceTvOnly = { viewModel.setForceTvListOnlyMode(it) },
                                onOpenMiracast = { showMirrorGuide = true },
                                onOpenPairTv = { showPairTvDialog = true },
                                onUnpairTv = { viewModel.unpairTv() },
                                onAdjustQuotaMb = { viewModel.updateStorageQuotaMb(it) }
                            )
                        }
                    }
                }
                }
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
            subtitle = "Essa senha protege as configurações e impede que a criança saia da trava de tela.",
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
            title = "Desbloquear tela dos pais",
            subtitle = "Digite a senha dos pais para destravar o aparelho e liberar os ajustes.",
            onDismiss = { showUnlockDialog = false; pendingTabAfterUnlock = null },
            onVerifyPin = { pin ->
                val ok = activity?.let { viewModel.deactivateKidLockWithPin(it, pin) }
                    ?: viewModel.verifyParentPin(pin)
                if (ok) {
                    showUnlockDialog = false
                    pendingTabAfterUnlock?.let { currentTab = it }
                    pendingTabAfterUnlock = null
                }
                ok
            }
        )
    }

    if (showPairTvDialog) {
        PairTvDialog(
            discoveredTvs = discoveredTvs.map { it.name to it.baseUrl },
            onDismiss = { showPairTvDialog = false },
            onPair = { url, pin ->
                viewModel.pairWithTv(url, pin)
                showPairTvDialog = false
            }
        )
    }

    if (showMirrorGuide) {
        ScreenMirrorDialog(
            onDismiss = { showMirrorGuide = false },
            settingsUnavailable = mirrorSettingsUnavailable,
            onOpenPhoneSettings = {
                mirrorSettingsUnavailable = !viewModel.openMiracastSettings()
            }
        )
    }
}

@Composable
private fun MobileVideoPlayerBox(
    video: VideoEntity,
    initialPositionMs: Long,
    kidLockActive: Boolean,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onNextVideo: () -> Unit,
    onProgress: (Long, Long) -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (video.sourceType == VideoSourceType.YOUTUBE && !video.youtubeId.isNullOrBlank()) {
        YouTubePlayerView(
            youtubeId = video.youtubeId,
            initialPositionMs = initialPositionMs,
            kidLockActive = kidLockActive,
            isFullscreen = isFullscreen,
            onToggleFullscreen = onToggleFullscreen,
            onNextVideo = onNextVideo,
            onProgress = onProgress,
            onEnded = onEnded,
            modifier = modifier
        )
    } else if (!video.localFilePath.isNullOrBlank()) {
        LocalVideoPlayerView(
            filePath = video.localFilePath,
            initialPositionMs = initialPositionMs,
            isFullscreen = isFullscreen,
            onToggleFullscreen = onToggleFullscreen,
            onProgress = onProgress,
            onEnded = onEnded,
            modifier = modifier
        )
    }
}

@Composable
private fun MobileWatchSection(
    video: VideoEntity,
    initialPositionMs: Long,
    activeChannel: PlaylistWithVideos?,
    queue: List<VideoEntity>,
    kidLockActive: Boolean,
    onClose: () -> Unit,
    onSelectVideo: (VideoEntity) -> Unit,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onOpenDialpad: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onNextVideo: () -> Unit,
    onProgress: (Long, Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KidsColors.Surface)
    ) {
        MobileVideoPlayerBox(
            video = video,
            initialPositionMs = initialPositionMs,
            kidLockActive = kidLockActive,
            isFullscreen = false,
            onToggleFullscreen = onToggleFullscreen,
            onNextVideo = onNextVideo,
            onProgress = onProgress,
            onEnded = onNextVideo,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        if (activeChannel != null) {
            ChannelZapperBar(
                activeChannel = activeChannel,
                resumedFromMs = initialPositionMs,
                onPreviousChannel = onPreviousChannel,
                onNextChannel = onNextChannel,
                onOpenDialpad = onOpenDialpad,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.channelOrAuthor ?: "KidTube",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Fechar player")
            }
        }

        if (queue.isNotEmpty()) {
            Text(
                text = if (activeChannel != null) "Programação do canal" else "Mais vídeos para assistir",
                style = MaterialTheme.typography.labelMedium,
                color = KidsColors.Primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(queue, key = { it.id }) { item ->
                    HorizontalVideoCard(
                        video = item,
                        isSelected = item.id == video.id,
                        onClick = { onSelectVideo(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ForceTvBanner(
    hasExternalDisplay: Boolean,
    pairedTvUrl: String?,
    playingTitle: String?,
    onOpenMiracast: () -> Unit
) {
    val active = hasExternalDisplay || !pairedTvUrl.isNullOrBlank()
    Surface(
        color = if (active) KidsColors.PrimaryContainer else Color(0xFF422006),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        hasExternalDisplay -> "Assistindo na TV (Miracast)"
                        !pairedTvUrl.isNullOrBlank() -> "Modo Controle TV ativo"
                        else -> "Modo Só TV ativo (vídeo bloqueado no celular)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = when {
                        hasExternalDisplay && playingTitle != null -> "Tocando agora: $playingTitle — escolha outro na lista abaixo."
                        hasExternalDisplay -> "Toque em um vídeo ou canal abaixo para reproduzir apenas na TV."
                        !pairedTvUrl.isNullOrBlank() -> "Toque em um vídeo para enviar para a Android TV."
                        else -> "Conecte uma TV compatível com espelhamento sem fio ou pareie com a Android TV."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            if (!hasExternalDisplay) {
                TextButton(onClick = onOpenMiracast) {
                    Text("Conectar TV", color = KidsColors.Primary)
                }
            }
        }
    }
}

@Composable
private fun ScreenMirrorDialog(
    onDismiss: () -> Unit,
    settingsUnavailable: Boolean,
    onOpenPhoneSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Espelhar na TV") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("1. Na TV ou no receptor, ative o espelhamento de tela (Miracast / Screen Mirroring).")
                Text("2. Ative o Wi-Fi do celular e mantenha a TV por perto.")
                Text("3. Toque em “Abrir espelhamento do celular” e selecione sua TV.")
                Text("Se abrirem as configurações gerais, procure por Smart View, Transmitir, Tela sem fio ou Espelhamento. Você também pode procurar esses atalhos no painel rápido do celular.")
                Text(
                    "Os nomes e a compatibilidade variam conforme os aparelhos. Google Cast e Miracast são tecnologias diferentes; abrir as configurações não garante que a TV seja compatível.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (settingsUnavailable) {
                    Text(
                        "Não foi possível abrir as configurações automaticamente. Abra o painel rápido ou as configurações do celular e procure a opção de espelhamento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenPhoneSettings) {
                Text("Abrir espelhamento do celular")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
private fun FeedTabContent(
    videos: List<VideoEntity>,
    playlists: List<PlaylistWithVideos>,
    canManage: Boolean,
    onPlay: (VideoEntity) -> Unit,
    onAddToPlaylist: (Long, Long) -> Unit,
    onDelete: (Long) -> Unit,
    onAddFirstVideo: () -> Unit
) {
    if (videos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KidTubeEmblem(size = 64.dp)
                Text("Nenhum vídeo no catálogo", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Adicione links do YouTube ou escolha vídeos salvos no aparelho para montar o feed seguro.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (canManage) {
                    Button(onClick = onAddFirstVideo) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Adicionar primeiro vídeo")
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(videos, key = { it.id }) { video ->
            FeedVideoCard(
                video = video,
                playlists = playlists,
                canManage = canManage,
                onPlay = { onPlay(video) },
                onAddToPlaylist = { plId -> onAddToPlaylist(plId, video.id) },
                onDelete = { onDelete(video.id) }
            )
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    playlists: List<PlaylistWithVideos>,
    selectedPlaylistId: Long?,
    canManage: Boolean,
    onSelectPlaylist: (Long?) -> Unit,
    onTuneChannel: (PlaylistWithVideos) -> Unit,
    onPlayProgram: (PlaylistWithVideos, VideoEntity) -> Unit,
    onEditChannel: (PlaylistEntity) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onCreatePlaylist: () -> Unit
) {
    val active = playlists.firstOrNull { it.playlist.id == selectedPlaylistId }
    if (active != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChannelLogoBadge(
                        channelNumber = active.playlist.channelNumber,
                        iconKey = active.playlist.iconKey,
                        colorHex = active.playlist.colorHex,
                        size = 40.dp
                    )
                    Column {
                        Text(active.playlist.name, style = MaterialTheme.typography.titleLarge)
                        Text("Canal ${formatChannelNumber(active.playlist.channelNumber)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = { onSelectPlaylist(null) }) { Text("Todas") }
            }
            if (active.videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Playlist vazia. Adicione vídeos no Feed.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(active.videos, key = { it.id }) { vid ->
                        FeedVideoCard(
                            video = vid,
                            playlists = playlists,
                            canManage = false,
                            onPlay = { onPlayProgram(active, vid) }
                        )
                    }
                }
            }
        }
        return
    }

    if (playlists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Nenhuma playlist/canal criado", style = MaterialTheme.typography.titleLarge)
                if (canManage) {
                    Button(onClick = onCreatePlaylist) { Text("Criar canal / playlist") }
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(playlists, key = { it.playlist.id }) { pwv ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPlaylist(pwv.playlist.id) },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, KidsColors.Border),
                colors = CardDefaults.cardColors(containerColor = KidsColors.Surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            ChannelLogoBadge(
                                channelNumber = pwv.playlist.channelNumber,
                                iconKey = pwv.playlist.iconKey,
                                colorHex = pwv.playlist.colorHex,
                                size = 44.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pwv.playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "Canal ${formatChannelNumber(pwv.playlist.channelNumber)} • ${pwv.videos.size} vídeo(s)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (canManage) {
                            IconButton(onClick = { onEditChannel(pwv.playlist) }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Editar canal", tint = KidsColors.TextSecondary)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pwv.videos.isNotEmpty()) {
                            TextButton(onClick = { onTuneChannel(pwv) }) {
                                Text("Sintonizar")
                            }
                        }
                        if (canManage) {
                            TextButton(onClick = { onDeletePlaylist(pwv.playlist.id) }) {
                                Text("Excluir", color = KidsColors.Danger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTabContent(
    history: List<com.kidstv.app.data.db.WatchHistoryWithVideo>,
    canClear: Boolean,
    onPlay: (VideoEntity) -> Unit,
    onClearHistory: () -> Unit
) {
    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum vídeo assistido ainda.", style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        if (canClear) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClearHistory) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Limpar histórico")
                }
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(history, key = { it.history.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlay(item.video) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, KidsColors.Border),
                    colors = CardDefaults.cardColors(containerColor = KidsColors.Surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.video.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "Assistido em ${Formatters.formatTimestamp(item.history.watchedAt)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { onPlay(item.video) }) { Text("Assistir") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentsTabContent(
    hasPin: Boolean,
    isKidLockActive: Boolean,
    forceTvListOnly: Boolean,
    hasExternalDisplay: Boolean,
    pairedTvUrl: String?,
    storage: com.kidstv.app.data.storage.StorageSnapshot,
    isDeviceOwner: Boolean,
    onOpenAddVideo: () -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    onConfigurePin: () -> Unit,
    onActivateKidLock: () -> Unit,
    onToggleForceTvOnly: (Boolean) -> Unit,
    onOpenMiracast: () -> Unit,
    onOpenPairTv: () -> Unit,
    onUnpairTv: () -> Unit,
    onAdjustQuotaMb: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, KidsColors.Border),
                colors = CardDefaults.cardColors(containerColor = KidsColors.Surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Trava Parental (Modo Criança)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (isDeviceOwner) {
                            "Modo Kiosk completo (Device Owner ativo): bloqueia saída até com botão Home."
                        } else {
                            "Usa Fixação de Tela do Android (startLockTask) + senha dos pais para não sair nem mexer nos ajustes."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onConfigurePin,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (hasPin) "Alterar senha" else "Criar senha", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = onActivateKidLock,
                            colors = ButtonDefaults.buttonColors(containerColor = KidsColors.Warning, contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Travar tela", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, KidsColors.Border),
                colors = CardDefaults.cardColors(containerColor = KidsColors.Surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TV & Espelhamento Miracast", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Só lista no celular (Forçar TV)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Oculta o vídeo da tela do celular e reproduz apenas na TV (via Miracast ou Android TV pareada).",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(checked = forceTvListOnly, onCheckedChange = onToggleForceTvOnly)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOpenMiracast,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Cast, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (hasExternalDisplay) "Miracast ativo" else "Miracast", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (pairedTvUrl == null) {
                            OutlinedButton(
                                onClick = onOpenPairTv,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Conectar TV", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onUnpairTv,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Desconectar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, KidsColors.Border),
                colors = CardDefaults.cardColors(containerColor = KidsColors.Surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Catálogo e Canais", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOpenAddVideo,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Vídeo", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onOpenCreatePlaylist,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Canal / Lista", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        item {
            StorageStatusCard(
                snapshot = storage,
                onAdjustQuotaMb = onAdjustQuotaMb
            )
        }
    }
}

@Composable
private fun PairTvDialog(
    discoveredTvs: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onPair: (url: String, pin: String) -> Unit
) {
    var selectedTvUrl by remember(discoveredTvs) { mutableStateOf(discoveredTvs.firstOrNull()?.second) }
    var manualEntry by remember { mutableStateOf(false) }
    var tvUrl by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conectar na Android TV") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (discoveredTvs.isNotEmpty()) {
                    Text("Escolha sua TV na rede:", style = MaterialTheme.typography.bodySmall)
                    discoveredTvs.forEach { (name, url) ->
                        OutlinedButton(onClick = { selectedTvUrl = url; manualEntry = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (selectedTvUrl == url) "✓ $name" else name)
                        }
                    }
                    TextButton(onClick = { manualEntry = !manualEntry }) {
                        Text(if (manualEntry) "Escolher TV encontrada" else "TV não aparece? Digitar endereço")
                    }
                } else {
                    Text(
                        "Deixe a TV e o celular na mesma rede Wi-Fi. Abra Configurar pelo Celular na TV; ela aparecerá aqui automaticamente.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { manualEntry = !manualEntry }) {
                        Text(if (manualEntry) "Voltar" else "Digitar endereço da TV")
                    }
                }
                if (manualEntry) OutlinedTextField(
                    value = tvUrl,
                    onValueChange = { tvUrl = it },
                    label = { Text("Endereço da TV") },
                    placeholder = { Text("http://192.168.1.50:8787") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Senha dos pais da TV") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetUrl = if (manualEntry) tvUrl.trim() else selectedTvUrl.orEmpty()
                    if (targetUrl.startsWith("http") && pin.length >= 4) {
                        onPair(targetUrl, pin.trim())
                    }
                }
            ) { Text("Conectar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
