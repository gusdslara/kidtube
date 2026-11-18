package com.kidstv.app.ui

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.view.Display
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kidstv.app.KidsTvApplication
import com.kidstv.app.data.db.PlaylistWithVideos
import com.kidstv.app.data.db.VideoEntity
import com.kidstv.app.data.db.VideoSourceType
import com.kidstv.app.data.db.WatchHistoryWithVideo
import com.kidstv.app.data.security.KioskLockController
import com.kidstv.app.data.security.ParentalState
import com.kidstv.app.data.storage.StorageSnapshot
import com.kidstv.app.player.ExternalDisplayMonitor
import com.kidstv.app.server.DiscoveredTv
import com.kidstv.app.server.TvLocalServer
import com.kidstv.app.server.TvRemoteClient
import com.kidstv.app.server.TvRemoteCommand
import com.kidstv.app.util.DeviceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TvQrSessionState(
    val isRunning: Boolean = false,
    val url: String? = null,
    val qrBitmap: Bitmap? = null,
    val error: String? = null
)

class KidsTvViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as KidsTvApplication).repository
    private val displayMonitor = ExternalDisplayMonitor(application)
    private val tvRemoteClient = TvRemoteClient(application)
    private var localServer: TvLocalServer? = null

    val isAndroidTv: Boolean = DeviceUtils.isAndroidTv(application)

    val parentalState: StateFlow<ParentalState> = repo.parentalPrefs.state
    val externalDisplay: StateFlow<Display?> = displayMonitor.externalDisplay
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = tvRemoteClient.discoveredTvs

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allVideos: StateFlow<List<VideoEntity>> = repo.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredVideos: StateFlow<List<VideoEntity>> = combine(allVideos, _searchQuery) { list, q ->
        val clean = q.trim()
        if (clean.isEmpty()) list
        else list.filter {
            it.title.contains(clean, ignoreCase = true) ||
                (it.channelOrAuthor?.contains(clean, ignoreCase = true) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistWithVideos>> = repo.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<WatchHistoryWithVideo>> = repo.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _storageSnapshot = MutableStateFlow(repo.getStorageSnapshot())
    val storageSnapshot: StateFlow<StorageSnapshot> = _storageSnapshot.asStateFlow()

    private val _currentVideo = MutableStateFlow<VideoEntity?>(null)
    val currentVideo: StateFlow<VideoEntity?> = _currentVideo.asStateFlow()

    private val _activeChannelId = MutableStateFlow<Long?>(null)
    val activeChannelId: StateFlow<Long?> = _activeChannelId.asStateFlow()

    private val _initialSeekPositionMs = MutableStateFlow(0L)
    val initialSeekPositionMs: StateFlow<Long> = _initialSeekPositionMs.asStateFlow()

    private val _activeQueue = MutableStateFlow<List<VideoEntity>>(emptyList())
    val activeQueue: StateFlow<List<VideoEntity>> = _activeQueue.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _tvQrState = MutableStateFlow(TvQrSessionState())
    val tvQrState: StateFlow<TvQrSessionState> = _tvQrState.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private var livePlaybackPosMs: Long = 0L
    private var lastSavedChannelPersistTimeMs: Long = 0L

    init {
        viewModelScope.launch {
            repo.localVideosSizeBytes.collect { refreshStorageSnapshot() }
        }
        viewModelScope.launch {
            parentalState.collect { refreshStorageSnapshot() }
        }
        if (!isAndroidTv) {
            tvRemoteClient.startDiscovery()
        }
    }

    fun refreshStorageSnapshot() {
        _storageSnapshot.value = repo.getStorageSnapshot()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun playVideo(video: VideoEntity, queue: List<VideoEntity> = allVideos.value) {
        flushActiveChannelState()
        _activeChannelId.value = null
        _initialSeekPositionMs.value = 0L
        livePlaybackPosMs = 0L
        playVideoInternal(video, queue, startMs = 0L)
    }

    /**
     * Cable TV Channel Mode:
     * Tunes a playlist as a TV channel. Resumes strictly from the video and timestamp
     * where the user stopped watching that channel (unless an explicit program is chosen).
     */
    fun tuneChannel(
        channel: PlaylistWithVideos,
        explicitVideo: VideoEntity? = null,
        explicitStartMs: Long? = null
    ) {
        if (channel.videos.isEmpty()) {
            _feedbackMessage.value = "Canal ${channel.playlist.channelNumber} (${channel.playlist.name}) está vazio."
            return
        }

        // Persist outgoing channel's exact stopped position before switching
        flushActiveChannelState()

        val targetVideo: VideoEntity
        val resumeMs: Long
        if (explicitVideo != null) {
            targetVideo = explicitVideo
            resumeMs = explicitStartMs ?: 0L
        } else {
            val savedId = channel.playlist.lastVideoId
            val savedMatch = savedId?.let { id -> channel.videos.firstOrNull { it.id == id } }
            if (savedMatch != null) {
                targetVideo = savedMatch
                resumeMs = channel.playlist.lastPositionMs.coerceAtLeast(0L)
            } else {
                targetVideo = channel.videos.first()
                resumeMs = 0L
            }
        }

        _activeChannelId.value = channel.playlist.id
        _initialSeekPositionMs.value = resumeMs
        livePlaybackPosMs = resumeMs

        viewModelScope.launch {
            repo.saveChannelPlaybackState(channel.playlist.id, targetVideo.id, resumeMs)
        }
        playVideoInternal(targetVideo, channel.videos, startMs = resumeMs)
    }

    fun tuneChannelByNumber(channelNumber: Int): Boolean {
        val match = playlists.value.firstOrNull { it.playlist.channelNumber == channelNumber }
        if (match == null) {
            _feedbackMessage.value = "Canal $channelNumber não encontrado."
            return false
        }
        tuneChannel(match)
        return true
    }

    fun nextChannel() {
        val channels = playlists.value.filter { it.videos.isNotEmpty() }
            .sortedBy { it.playlist.channelNumber }
        if (channels.isEmpty()) {
            _feedbackMessage.value = "Nenhum canal com vídeos cadastrado."
            return
        }
        val currentChId = _activeChannelId.value
        val idx = channels.indexOfFirst { it.playlist.id == currentChId }
        val next = if (idx < 0) channels.first() else channels[(idx + 1) % channels.size]
        tuneChannel(next)
    }

    fun previousChannel() {
        val channels = playlists.value.filter { it.videos.isNotEmpty() }
            .sortedBy { it.playlist.channelNumber }
        if (channels.isEmpty()) {
            _feedbackMessage.value = "Nenhum canal com vídeos cadastrado."
            return
        }
        val currentChId = _activeChannelId.value
        val idx = channels.indexOfFirst { it.playlist.id == currentChId }
        val prev = if (idx < 0) channels.last() else channels[(idx - 1 + channels.size) % channels.size]
        tuneChannel(prev)
    }

    private fun playVideoInternal(video: VideoEntity, queue: List<VideoEntity>, startMs: Long) {
        val pState = parentalState.value
        val hasExternalDisplay = externalDisplay.value != null

        if (!isAndroidTv && pState.forceTvListOnlyMode) {
            when {
                hasExternalDisplay -> {
                    _currentVideo.value = video
                    _activeQueue.value = if (queue.isNotEmpty()) queue else listOf(video)
                    viewModelScope.launch { repo.recordWatchProgress(video.id, startMs, video.durationMs) }
                }
                !pState.pairedTvBaseUrl.isNullOrBlank() -> {
                    if (video.sourceType != VideoSourceType.YOUTUBE) {
                        _currentVideo.value = null
                        _feedbackMessage.value =
                            "Vídeos locais do celular tocam na TV via Miracast ou após enviar para a TV via QR Code."
                        return
                    }
                    viewModelScope.launch {
                        val res = tvRemoteClient.sendPlayYouTubeToTv(
                            baseUrl = pState.pairedTvBaseUrl,
                            youtubeUrl = video.youtubeUrl ?: video.youtubeId.orEmpty(),
                            title = video.title
                        )
                        res.fold(
                            onSuccess = {
                                _feedbackMessage.value = "Enviado para a Android TV: ${video.title}"
                                repo.recordWatchProgress(video.id, startMs, video.durationMs)
                            },
                            onFailure = {
                                _feedbackMessage.value = it.message ?: "Não foi possível enviar para a TV."
                            }
                        )
                    }
                }
                else -> {
                    _currentVideo.value = null
                    _feedbackMessage.value =
                        "Modo Só TV ativo: conecte o Miracast ou pareie sua Android TV para reproduzir na TV."
                }
            }
            return
        }

        _currentVideo.value = video
        _activeQueue.value = if (queue.isNotEmpty()) queue else listOf(video)
        viewModelScope.launch {
            repo.recordWatchProgress(video.id, startMs, video.durationMs)
        }
    }

    fun playNextInQueue() {
        val current = _currentVideo.value ?: return
        val queue = _activeQueue.value
        if (queue.isEmpty()) return
        val idx = queue.indexOfFirst { it.id == current.id }
        val next = if (idx >= 0 && idx + 1 < queue.size) queue[idx + 1] else queue.firstOrNull()
        val chId = _activeChannelId.value

        if (chId != null && next != null) {
            val channel = playlists.value.firstOrNull { it.playlist.id == chId }
            if (channel != null) {
                _initialSeekPositionMs.value = 0L
                livePlaybackPosMs = 0L
                viewModelScope.launch {
                    repo.saveChannelPlaybackState(chId, next.id, 0L)
                }
                playVideoInternal(next, queue, startMs = 0L)
                return
            }
        }

        if (next != null && next.id != current.id) {
            playVideo(next, queue)
        }
    }

    fun closePlayer() {
        flushActiveChannelState()
        _currentVideo.value = null
        _activeChannelId.value = null
        _initialSeekPositionMs.value = 0L
        _isFullscreen.value = false
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
    }

    fun onVideoProgress(videoId: Long, positionMs: Long, durationMs: Long) {
        livePlaybackPosMs = positionMs.coerceAtLeast(0L)
        val chId = _activeChannelId.value
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repo.recordWatchProgress(videoId, positionMs, durationMs)
            if (chId != null && now - lastSavedChannelPersistTimeMs >= 2500L) {
                lastSavedChannelPersistTimeMs = now
                repo.saveChannelPlaybackState(chId, videoId, livePlaybackPosMs)
            }
        }
    }

    fun flushChannelProgressNow() {
        flushActiveChannelState()
    }

    private fun flushActiveChannelState() {
        val chId = _activeChannelId.value ?: return
        val vidId = _currentVideo.value?.id ?: return
        val pos = livePlaybackPosMs.coerceAtLeast(0L)
        viewModelScope.launch {
            repo.saveChannelPlaybackState(chId, vidId, pos)
        }
    }

    fun addYouTubeVideo(rawUrl: String, customTitle: String?, playlistId: Long?) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.addYouTubeVideo(
                rawUrl = rawUrl,
                customTitle = customTitle,
                playlistId = playlistId,
                addedFrom = if (isAndroidTv) "TV" else "MOBILE"
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { _feedbackMessage.value = "Adicionado: ${it.title}" },
                onFailure = { _feedbackMessage.value = it.message ?: "Erro ao adicionar vídeo do YouTube." }
            )
        }
    }

    fun importLocalVideos(uris: List<Uri>, customTitle: String?, playlistId: Long?) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            var addedCount = 0
            var lastError: String? = null
            for (uri in uris) {
                val res = repo.addLocalVideoFromUri(
                    uri = uri,
                    customTitle = if (uris.size == 1) customTitle else null,
                    playlistId = playlistId,
                    addedFrom = if (isAndroidTv) "TV" else "MOBILE"
                )
                res.onSuccess { addedCount++ }
                res.onFailure { lastError = it.message }
            }
            refreshStorageSnapshot()
            _isBusy.value = false
            _feedbackMessage.value = when {
                addedCount > 0 && lastError == null -> "$addedCount vídeo(s) importado(s) com sucesso!"
                addedCount > 0 -> "$addedCount importado(s). Aviso: $lastError"
                else -> lastError ?: "Não foi possível importar o vídeo."
            }
        }
    }

    fun deleteVideo(videoId: Long) {
        viewModelScope.launch {
            if (_currentVideo.value?.id == videoId) {
                _currentVideo.value = null
            }
            repo.deleteVideo(videoId)
            refreshStorageSnapshot()
            _feedbackMessage.value = "Vídeo removido e espaço liberado."
        }
    }

    fun createPlaylist(
        name: String,
        channelNumber: Int? = null,
        iconKey: String = "tv"
    ) {
        viewModelScope.launch {
            val id = repo.createPlaylist(name = name, channelNumber = channelNumber, iconKey = iconKey)
            if (id != null) {
                _feedbackMessage.value = "Canal '$name' criado."
            }
        }
    }

    fun updateChannelInfo(
        playlistId: Long,
        name: String,
        channelNumber: Int,
        iconKey: String
    ) {
        viewModelScope.launch {
            repo.updateChannelInfo(
                playlistId = playlistId,
                name = name,
                channelNumber = channelNumber,
                iconKey = iconKey
            )
            _feedbackMessage.value = "Canal $channelNumber atualizado."
        }
    }

    fun moveVideoInPlaylist(playlistId: Long, videoId: Long, moveUp: Boolean) {
        viewModelScope.launch {
            repo.moveVideoInPlaylist(playlistId, videoId, moveUp)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            if (_activeChannelId.value == playlistId) {
                closePlayer()
            }
            repo.deletePlaylist(playlistId)
        }
    }

    fun assignVideoToPlaylist(playlistId: Long, videoId: Long) {
        viewModelScope.launch {
            repo.addVideoToPlaylist(playlistId, videoId)
            _feedbackMessage.value = "Adicionado à programação do canal."
        }
    }

    fun removeVideoFromPlaylist(playlistId: Long, videoId: Long) {
        viewModelScope.launch {
            repo.removeVideoFromPlaylist(playlistId, videoId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repo.clearHistory()
            _feedbackMessage.value = "Histórico limpo."
        }
    }

    fun updateStorageQuotaMb(quotaMb: Int) {
        repo.parentalPrefs.setStorageQuotaMb(quotaMb)
        refreshStorageSnapshot()
    }

    fun setupParentPin(rawPin: String): Boolean {
        val ok = repo.parentalPrefs.setPin(rawPin)
        if (ok) _feedbackMessage.value = "Senha dos pais configurada."
        return ok
    }

    fun verifyParentPin(rawPin: String): Boolean =
        repo.parentalPrefs.verifyPin(rawPin)

    fun activateKidLock(activity: Activity) {
        if (!repo.parentalPrefs.hasPinConfigured()) {
            _feedbackMessage.value = "Configure uma senha dos pais antes de ativar a trava."
            return
        }
        repo.parentalPrefs.setKidLockActive(true)
        KioskLockController.enterLockTask(activity)
        _feedbackMessage.value = "Modo Criança travado. Só sai com a senha dos pais."
    }

    fun deactivateKidLockWithPin(activity: Activity, pin: String): Boolean {
        if (!repo.parentalPrefs.verifyPin(pin)) return false
        repo.parentalPrefs.setKidLockActive(false)
        KioskLockController.exitLockTask(activity)
        _feedbackMessage.value = "Trava desativada."
        return true
    }

    fun setForceTvListOnlyMode(enabled: Boolean) {
        repo.parentalPrefs.setForceTvListOnlyMode(enabled)
        if (enabled && !isAndroidTv && externalDisplay.value == null) {
            _currentVideo.value = null
        }
    }

    fun openMiracastSettings(): Boolean {
        val opened = ExternalDisplayMonitor.openMiracastSettings(getApplication())
        if (!opened) {
            _feedbackMessage.value = "Abra o painel rápido ou as configurações e procure Smart View, Transmitir ou Tela sem fio."
        }
        return opened
    }

    fun pairWithTv(baseUrl: String, pin: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = tvRemoteClient.authenticateOnTv(baseUrl, pin)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    repo.parentalPrefs.setPairedTvBaseUrl(baseUrl)
                    _feedbackMessage.value = "Conectado à Android TV ($baseUrl)!"
                },
                onFailure = {
                    _feedbackMessage.value = it.message ?: "Erro ao conectar na Android TV."
                }
            )
        }
    }

    fun unpairTv() {
        repo.parentalPrefs.setPairedTvBaseUrl(null)
        _feedbackMessage.value = "TV desconectada."
    }

    fun startTvQrServer() {
        viewModelScope.launch {
            try {
                if (localServer == null) {
                    val srv = TvLocalServer(getApplication(), repo)
                    localServer = srv
                    launch {
                        srv.remoteCommands.collect { cmd ->
                            when (cmd) {
                                is TvRemoteCommand.PlayVideo -> {
                                    val video = repo.getVideoById(cmd.videoId)
                                    if (video != null) playVideo(video)
                                }
                                is TvRemoteCommand.PlayYouTubeUrl -> {
                                    val added = repo.addYouTubeVideo(
                                        rawUrl = cmd.url,
                                        customTitle = cmd.title,
                                        addedFrom = "MOBILE_REMOTE"
                                    )
                                    added.onSuccess { playVideo(it) }
                                }
                            }
                        }
                    }
                }
                val url = localServer?.startServerAndRegister()
                if (url == null) {
                    _tvQrState.value = TvQrSessionState(
                        isRunning = false,
                        error = "Conecte a TV ao Wi-Fi ou cabo de rede para gerar o QR Code."
                    )
                } else {
                    val bmp = DeviceUtils.generateQrCodeBitmap(url, 640)
                    _tvQrState.value = TvQrSessionState(
                        isRunning = true,
                        url = url,
                        qrBitmap = bmp,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _tvQrState.value = TvQrSessionState(
                    isRunning = false,
                    error = e.message ?: "Não foi possível iniciar o servidor local."
                )
            }
        }
    }

    fun stopTvQrServer() {
        localServer?.stopServer()
        _tvQrState.value = TvQrSessionState(isRunning = false)
    }

    override fun onCleared() {
        flushActiveChannelState()
        super.onCleared()
        displayMonitor.release()
        tvRemoteClient.stopDiscovery()
        localServer?.stopServer()
    }
}
