package com.horas_al_mando.ham_android.network

import android.util.Log
import com.google.gson.Gson
import com.horas_al_mando.ham_android.BuildConfig
import com.horas_al_mando.ham_android.model.FlightPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

object FlightSyncSocketRepository {
    private val WS_URL = BuildConfig.WS_BASE_URL + "/ws/flight-sync"
    private const val MAX_BACKOFF_MS = 15000L

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var webSocket: WebSocket? = null
    private var clientFlightId: String = ""
    private var startTime: String = ""

    @Volatile private var ackedCount: Int = 0
    @Volatile private var connected: Boolean = false
    @Volatile private var shouldReconnect: Boolean = false
    private var reconnectAttempts: Int = 0

    fun connect(clientFlightId: String, startTime: String) {
        this.clientFlightId = clientFlightId
        this.startTime = startTime
        ackedCount = 0
        reconnectAttempts = 0
        shouldReconnect = true
        openSocket()
    }

    fun pushPoints(fullPath: List<FlightPoint>) {
        val ws = webSocket
        if (!connected || ws == null || fullPath.size <= ackedCount) return

        val newPoints = fullPath.subList(ackedCount, fullPath.size)
        val message = mapOf(
            "clientFlightId" to clientFlightId,
            "startTime" to startTime,
            "fromIndex" to ackedCount,
            "points" to newPoints,
        )
        ws.send(gson.toJson(message))
    }

    fun disconnect() {
        shouldReconnect = false
        connected = false
        webSocket?.close(1000, "Flight ended")
        webSocket = null
        ackedCount = 0
    }

    private fun openSocket() {
        // Authorization is injected by AuthInterceptor (on ApiClient.okHttpClient) for non-/auth/
        // paths. Adding it here too would send two Authorization headers, which Cloudflare rejects
        // with 400 on the WS handshake (works locally direct-to-Spring, fails in prod behind CDN).
        val builder = Request.Builder().url(WS_URL)
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

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            connected = true
            reconnectAttempts = 0
            Log.d("FlightSyncSocket", "connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val ack = gson.fromJson(text, Ack::class.java)
                if (ack.ackedCount >= ackedCount) ackedCount = ack.ackedCount
            } catch (e: Exception) {
                Log.e("FlightSyncSocket", "ack parse error: ${e.message}")
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            connected = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            connected = false
            Log.e("FlightSyncSocket", "failure: ${t.message}")
            scheduleReconnect()
        }
    }

    private data class Ack(val clientFlightId: String?, val ackedCount: Int)
}
