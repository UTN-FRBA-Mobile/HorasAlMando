package com.horas_al_mando.ham_android.network

import com.google.gson.Gson
import com.horas_al_mando.ham_android.BuildConfig
import com.horas_al_mando.ham_android.model.PilotLocation
import com.horas_al_mando.ham_android.model.FlightPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.WebSocket

object SocialRadarRepository {
    private val WS_URL = BuildConfig.WS_BASE_URL + "/ws/radar"
    private const val MAX_BACKOFF_MS = 15000L

    private val _activePilots = MutableStateFlow<List<PilotLocation>>(emptyList())
    val activePilots: StateFlow<List<PilotLocation>> = _activePilots.asStateFlow()

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var webSocket: WebSocket? = null
    @Volatile private var connected: Boolean = false
    @Volatile private var shouldReconnect: Boolean = false
    private var reconnectAttempts: Int = 0

    fun connect() {
        if (shouldReconnect) return
        shouldReconnect = true
        reconnectAttempts = 0
        openSocket()
    }

    fun disconnect() {
        shouldReconnect = false
        connected = false
        webSocket?.close(1000, "User left screen")
        webSocket = null
        _activePilots.value = emptyList()
    }

    fun sendLocation(point: FlightPoint) {
        if (!connected) return
        val message = mapOf("lat" to point.lat, "lng" to point.lng)
        webSocket?.send(gson.toJson(message))
    }

    private fun openSocket() {
        val token = ApiClient.getSessionManager().getAuthToken()
        val builder = Request.Builder().url(WS_URL)
        if (token != null) builder.addHeader("Authorization", "Bearer $token")
        val listener = RadarSocketListener(
            onPilots = { _activePilots.value = it },
            onOpen = { connected = true; reconnectAttempts = 0 },
            onClosed = { connected = false; _activePilots.value = emptyList(); scheduleReconnect() },
        )
        webSocket = ApiClient.okHttpClient.newWebSocket(builder.build(), listener)
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        reconnectAttempts++
        val backoff = minOf(2000L * reconnectAttempts, MAX_BACKOFF_MS)
        scope.launch {
            delay(backoff)
            if (shouldReconnect) openSocket()
        }
    }
}
