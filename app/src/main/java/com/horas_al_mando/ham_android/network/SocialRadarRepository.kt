package com.horas_al_mando.ham_android.network

import com.google.gson.Gson
import com.horas_al_mando.ham_android.model.MockData
import com.horas_al_mando.ham_android.model.PilotLocation
import com.horas_al_mando.ham_android.model.FlightPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Request
import okhttp3.WebSocket

object SocialRadarRepository {
    private const val WS_URL = "wss://api.horasalmando.com.ar/ws/radar"

    private val _activePilots = MutableStateFlow<List<PilotLocation>>(emptyList())
    val activePilots: StateFlow<List<PilotLocation>> = _activePilots.asStateFlow()

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    fun connect() {
        if (webSocket != null) return

        val request = Request.Builder().url(WS_URL).build()
        val listener = RadarSocketListener { pilots ->
            _activePilots.value = pilots
        }
        webSocket = ApiClient.okHttpClient.newWebSocket(request, listener)
    }

    fun disconnect() {
        webSocket?.close(1000, "User left screen")
        webSocket = null
        _activePilots.value = emptyList()
    }

    fun sendLocation(point: FlightPoint) {
        val pilotLocation = PilotLocation(
            pilotId = MockData.PILOT_ID,
            name = MockData.PILOT_NAME,
            lat = point.lat,
            lng = point.lng,
            altitude = point.altitude,
            speed = point.speed,
            heading = point.heading
        )
        val json = gson.toJson(pilotLocation)
        webSocket?.send(json)
    }
}
