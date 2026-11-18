package com.kidstv.app.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.kidstv.app.data.db.VideoEntity
import com.kidstv.app.data.repository.KidsTvRepository
import com.kidstv.app.util.DeviceUtils
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed interface TvRemoteCommand {
    data class PlayVideo(val videoId: Long) : TvRemoteCommand
    data class PlayYouTubeUrl(val url: String, val title: String?) : TvRemoteCommand
}

class TvLocalServer(
    private val context: Context,
    private val repository: KidsTvRepository,
    port: Int = DEFAULT_PORT
) : NanoHTTPD(port) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeSessions = ConcurrentHashMap.newKeySet<String>()
    private data class LoginAttempts(var failures: Int = 0, var lockedUntilMs: Long = 0L)
    private val loginAttempts = ConcurrentHashMap<String, LoginAttempts>()
    val pairToken: String = UUID.randomUUID().toString().take(8)

    private val _remoteCommands = MutableSharedFlow<TvRemoteCommand>(extraBufferCapacity = 16)
    val remoteCommands: SharedFlow<TvRemoteCommand> = _remoteCommands.asSharedFlow()

    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun startServerAndRegister(): String? {
        if (!isAlive) {
            start(SOCKET_READ_TIMEOUT, false)
        }
        registerNsdService(listeningPort)
        val ip = DeviceUtils.getLocalIpv4Address() ?: return null
        return "http://$ip:$listeningPort/?pair=$pairToken"
    }

    fun stopServer() {
        unregisterNsdService()
        if (isAlive) stop()
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        val method = session.method

        return try {
            when {
                method == Method.GET && uri == "/" -> {
                    newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", TvWebPortalHtml.buildHtml())
                }

                method == Method.GET && uri == "/api/status" -> {
                    val authed = isAuthenticated(session)
                    val json = JSONObject().apply {
                        put("hasPinConfigured", repository.parentalPrefs.hasPinConfigured())
                        put("authenticated", authed)
                        put("appName", "KidTube")
                    }
                    jsonResponse(Response.Status.OK, json)
                }

                method == Method.POST && uri == "/api/auth/setup" -> handleAuthSetup(session)
                method == Method.POST && uri == "/api/auth/login" -> handleAuthLogin(session)

                // Authenticated Parent Endpoints
                method == Method.GET && uri == "/api/catalog" -> {
                    if (!isAuthenticated(session)) return unauthorized()
                    handleGetCatalog()
                }

                method == Method.POST && uri == "/api/videos/youtube" -> {
                    if (!isAuthenticated(session)) return unauthorized()
                    handleAddYouTube(session)
                }

                method == Method.POST && uri == "/api/videos/upload" -> {
                    if (!isAuthenticated(session)) return unauthorized()
                    handleUploadVideo(session)
                }

                method == Method.DELETE && uri == "/api/videos" -> {
                    if (!isAuthenticated(session)) return unauthorized()
                    val id = session.parameters["id"]?.firstOrNull()?.toLongOrNull()
                        ?: return errorResponse(Response.Status.BAD_REQUEST, "ID inválido.")
                    runBlocking { repository.deleteVideo(id) }
                    jsonResponse(Response.Status.OK, JSONObject().put("deleted", true))
                }

                method == Method.POST && uri == "/api/playlists" -> {
                    if (!isAuthenticated(session)) return unauthorized()
                    val body = readJsonBody(session)
                    val name = body.optString("name").trim()
                    if (name.isEmpty()) return errorResponse(Response.Status.BAD_REQUEST, "Nome obrigatório.")
                    val chNum = if (body.has("channelNumber")) body.optInt("channelNumber", 0).takeIf { it > 0 } else null
                    val iconKey = body.optString("iconKey", "tv").ifBlank { "tv" }
                    val id = runBlocking {
                        repository.createPlaylist(
                            name = name,
                            channelNumber = chNum,
                            iconKey = iconKey
                        )
                    }
                    jsonResponse(Response.Status.OK, JSONObject().put("id", id))
                }

                method == Method.DELETE && uri == "/api/playlists" -> {
                    if (!isAuthenticated(session)) return unauthorized()
                    val id = session.parameters["id"]?.firstOrNull()?.toLongOrNull()
                        ?: return errorResponse(Response.Status.BAD_REQUEST, "ID inválido.")
                    runBlocking { repository.deletePlaylist(id) }
                    jsonResponse(Response.Status.OK, JSONObject().put("deleted", true))
                }

                method == Method.POST && uri == "/api/storage/quota" -> {
                    if (!isAuthenticated(session)) return unauthorized()
                    val body = readJsonBody(session)
                    val quotaMb = body.optInt("quotaMb", 4096)
                    repository.parentalPrefs.setStorageQuotaMb(quotaMb)
                    jsonResponse(Response.Status.OK, JSONObject().put("quotaMb", quotaMb))
                }

                method == Method.POST && uri == "/api/tv/play" -> {
                    if (!isAuthenticated(session)) return unauthorized()
                    val body = readJsonBody(session)
                    val videoId = body.optLong("videoId", -1L)
                    val ytUrl = body.optString("youtubeUrl").takeIf { it.isNotBlank() }
                    val title = body.optString("title").takeIf { it.isNotBlank() }
                    when {
                        videoId > 0L -> _remoteCommands.tryEmit(TvRemoteCommand.PlayVideo(videoId))
                        ytUrl != null -> _remoteCommands.tryEmit(TvRemoteCommand.PlayYouTubeUrl(ytUrl, title))
                        else -> return errorResponse(Response.Status.BAD_REQUEST, "Informe videoId ou youtubeUrl.")
                    }
                    jsonResponse(Response.Status.OK, JSONObject().put("dispatched", true))
                }

                else -> errorResponse(Response.Status.NOT_FOUND, "Rota não encontrada.")
            }
        } catch (e: Exception) {
            errorResponse(Response.Status.INTERNAL_ERROR, e.message ?: "Erro interno")
        }
    }

    private fun handleAuthSetup(session: IHTTPSession): Response {
        if (session.headers["x-pair-token"] != pairToken) {
            return errorResponse(Response.Status.FORBIDDEN, "Abra o QR Code exibido na TV para configurar a senha.")
        }
        if (repository.parentalPrefs.hasPinConfigured()) {
            return errorResponse(Response.Status.FORBIDDEN, "A senha já foi configurada anteriormente.")
        }
        val body = readJsonBody(session)
        val pin = body.optString("pin").trim()
        if (!repository.parentalPrefs.setPin(pin)) {
            return errorResponse(Response.Status.BAD_REQUEST, "A senha precisa ter pelo menos 4 caracteres.")
        }
        val token = UUID.randomUUID().toString()
        activeSessions.add(token)
        return jsonResponse(Response.Status.OK, JSONObject().put("sessionToken", token))
    }

    private fun handleAuthLogin(session: IHTTPSession): Response {
        if (!repository.parentalPrefs.hasPinConfigured()) {
            return errorResponse(Response.Status.BAD_REQUEST, "Configure a senha primeiro.")
        }
        val remoteAddress = session.remoteIpAddress ?: "unknown"
        val attempts = loginAttempts.computeIfAbsent(remoteAddress) { LoginAttempts() }
        val now = System.currentTimeMillis()
        synchronized(attempts) {
            if (now < attempts.lockedUntilMs) {
                return errorResponse(Response.Status.FORBIDDEN, "Muitas tentativas. Aguarde 30 segundos e tente novamente.")
            }
        }
        val body = readJsonBody(session)
        val pin = body.optString("pin").trim()
        if (!repository.parentalPrefs.verifyPin(pin)) {
            synchronized(attempts) {
                attempts.failures++
                if (attempts.failures >= MAX_PIN_ATTEMPTS) {
                    attempts.failures = 0
                    attempts.lockedUntilMs = System.currentTimeMillis() + PIN_LOCKOUT_MS
                }
            }
            return errorResponse(Response.Status.UNAUTHORIZED, "Senha incorreta.")
        }
        loginAttempts.remove(remoteAddress, attempts)
        val token = UUID.randomUUID().toString()
        activeSessions.add(token)
        return jsonResponse(Response.Status.OK, JSONObject().put("sessionToken", token))
    }

    private fun handleGetCatalog(): Response {
        val videos = runBlocking { repository.getAllVideosOnce() }
        val playlists = runBlocking { repository.getAllPlaylistsOnce() }
        val storage = repository.getStorageSnapshot()

        val videosArr = JSONArray().apply {
            videos.forEach { put(videoToJson(it)) }
        }
        val playlistsArr = JSONArray().apply {
            playlists.forEach { pwv ->
                put(
                    JSONObject().apply {
                        put("id", pwv.playlist.id)
                        put("name", pwv.playlist.name)
                        put("channelNumber", pwv.playlist.channelNumber)
                        put("iconKey", pwv.playlist.iconKey)
                        put("videoCount", pwv.videos.size)
                    }
                )
            }
        }
        val storageObj = JSONObject().apply {
            put("totalDeviceBytes", storage.totalDeviceBytes)
            put("freeDeviceBytes", storage.freeDeviceBytes)
            put("appVideosBytes", storage.appVideosBytes)
            put("quotaBytes", storage.quotaBytes)
            put("remainingAllowedBytes", storage.remainingAllowedBytes)
            put("usedRatioOfQuota", storage.usedRatioOfQuota.toDouble())
        }
        val root = JSONObject().apply {
            put("videos", videosArr)
            put("playlists", playlistsArr)
            put("storage", storageObj)
        }
        return jsonResponse(Response.Status.OK, root)
    }

    private fun handleAddYouTube(session: IHTTPSession): Response {
        val body = readJsonBody(session)
        val url = body.optString("url")
        val title = body.optString("title").takeIf { it.isNotBlank() }
        val playlistId = if (body.isNull("playlistId")) null else body.optLong("playlistId", -1L).takeIf { it > 0L }
        val result = runBlocking {
            repository.addYouTubeVideo(
                rawUrl = url,
                customTitle = title,
                playlistId = playlistId,
                addedFrom = "TV_QR_WEB"
            )
        }
        return result.fold(
            onSuccess = { jsonResponse(Response.Status.OK, videoToJson(it)) },
            onFailure = { errorResponse(Response.Status.BAD_REQUEST, it.message ?: "URL inválida") }
        )
    }

    private fun handleUploadVideo(session: IHTTPSession): Response {
        val contentLength = session.headers["content-length"]?.toLongOrNull() ?: 0L
        if (contentLength > 0L) {
            val (fits, reason) = repository.storageManager.canFitNewFile(
                contentLength,
                repository.parentalPrefs.state.value.storageQuotaMb
            )
            if (!fits) {
                return errorResponse(Response.Status.PAYLOAD_TOO_LARGE, reason ?: "Sem espaço na TV.")
            }
        }

        val files = HashMap<String, String>()
        session.parseBody(files)

        val tempPath = files["file"]
            ?: return errorResponse(Response.Status.BAD_REQUEST, "Arquivo não enviado.")
        val tempFile = File(tempPath)
        val originalName = session.parameters["file"]?.firstOrNull() ?: "video.mp4"
        val customTitle = session.parameters["title"]?.firstOrNull()?.takeIf { it.isNotBlank() }
        val playlistId = session.parameters["playlistId"]?.firstOrNull()?.toLongOrNull()

        val result = runBlocking {
            repository.addUploadedVideoFile(
                tempFile = tempFile,
                originalFileName = originalName,
                customTitle = customTitle,
                playlistId = playlistId
            )
        }
        return result.fold(
            onSuccess = { jsonResponse(Response.Status.OK, videoToJson(it)) },
            onFailure = { errorResponse(Response.Status.BAD_REQUEST, it.message ?: "Falha ao salvar vídeo.") }
        )
    }

    private fun isAuthenticated(session: IHTTPSession): Boolean {
        val headerToken = session.headers["x-session-token"]
        return headerToken != null && activeSessions.contains(headerToken)
    }

    private fun readJsonBody(session: IHTTPSession): JSONObject {
        val map = HashMap<String, String>()
        session.parseBody(map)
        val raw = map["postData"] ?: "{}"
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private fun videoToJson(v: VideoEntity): JSONObject = JSONObject().apply {
        put("id", v.id)
        put("title", v.title)
        put("sourceType", v.sourceType.name)
        put("youtubeId", v.youtubeId)
        put("fileSizeBytes", v.fileSizeBytes)
        put("durationMs", v.durationMs)
    }

    private fun unauthorized(): Response =
        errorResponse(Response.Status.UNAUTHORIZED, "401 Autenticação necessária")

    private fun jsonResponse(status: Response.Status, obj: JSONObject): Response =
        newFixedLengthResponse(status, "application/json; charset=utf-8", obj.toString())

    private fun errorResponse(status: Response.Status, message: String): Response =
        jsonResponse(status, JSONObject().put("error", message))

    private fun registerNsdService(port: Int) {
        try {
            unregisterNsdService()
            val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
            nsdManager = manager
            val info = NsdServiceInfo().apply {
                serviceName = "KidTube"
                serviceType = "_kidstv._tcp."
                setPort(port)
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {}
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            }
            registrationListener = listener
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Exception) {
        }
    }

    private fun unregisterNsdService() {
        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
        } catch (_: Exception) {
        } finally {
            registrationListener = null
        }
    }

    companion object {
        const val DEFAULT_PORT = 8787
        private const val MAX_PIN_ATTEMPTS = 5
        private const val PIN_LOCKOUT_MS = 30_000L
    }
}
