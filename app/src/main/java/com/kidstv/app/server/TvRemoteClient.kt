package com.kidstv.app.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DiscoveredTv(
    val name: String,
    val baseUrl: String
)

class TvRemoteClient(private val context: Context) {

    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var sessionToken: String? = null

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    fun startDiscovery() {
        stopDiscovery()
        try {
            val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
            nsdManager = manager
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {}
                override fun onServiceFound(service: NsdServiceInfo) {
                    if (service.serviceType.contains("_kidstv._tcp")) {
                        @Suppress("DEPRECATION")
                        manager.resolveService(service, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                @Suppress("DEPRECATION")
                                val host = serviceInfo.host?.hostAddress ?: return
                                val url = "http://$host:${serviceInfo.port}"
                                val item = DiscoveredTv(serviceInfo.serviceName, url)
                                val current = _discoveredTvs.value.toMutableList()
                                if (current.none { it.baseUrl == item.baseUrl }) {
                                    current.add(item)
                                    _discoveredTvs.value = current
                                }
                            }
                        })
                    }
                }
                override fun onServiceLost(service: NsdServiceInfo) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            }
            discoveryListener = listener
            manager.discoverServices("_kidstv._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Exception) {
        }
    }

    fun stopDiscovery() {
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (_: Exception) {
        } finally {
            discoveryListener = null
        }
    }

    suspend fun authenticateOnTv(baseUrl: String, pin: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("${baseUrl.trimEnd('/')}/api/auth/login").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(JSONObject().put("pin", pin).toString().toByteArray()) }
            val body = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
            if (conn.responseCode in 200..299) {
                val token = json.optString("sessionToken")
                sessionToken = token
                Result.success(token)
            } else {
                Result.failure(IllegalStateException(json.optString("error", "Falha ao autenticar na TV")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPlayYouTubeToTv(baseUrl: String, youtubeUrl: String, title: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val token = sessionToken
                    ?: return@withContext Result.failure(IllegalStateException("Autentique-se na TV antes de enviar comandos."))
                val conn = (URL("${baseUrl.trimEnd('/')}/api/tv/play").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Session-Token", token)
                }
                val payload = JSONObject().apply {
                    put("youtubeUrl", youtubeUrl)
                    if (!title.isNullOrBlank()) put("title", title)
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                if (conn.responseCode in 200..299) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Erro ao enviar para TV (HTTP ${conn.responseCode})"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
