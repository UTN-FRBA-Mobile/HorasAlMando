package com.horas_al_mando.ham_android.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.horas_al_mando.ham_android.model.PilotLocation
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class RadarSocketListener(
    private val onPilots: (List<PilotLocation>) -> Unit,
    private val onOpen: () -> Unit,
    private val onClosed: () -> Unit,
) : WebSocketListener() {

    private val gson = Gson()

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("RadarSocket", "WebSocket Connected")
        onOpen()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val type = object : TypeToken<List<PilotLocation>>() {}.type
            val pilots: List<PilotLocation> = gson.fromJson(text, type)
            onPilots(pilots)
        } catch (e: Exception) {
            Log.e("RadarSocket", "Error parsing message: ${e.message}")
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(1000, null)
        Log.d("RadarSocket", "WebSocket Closing: $reason")
        onClosed()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("RadarSocket", "WebSocket Failure: ${t.message}")
        onClosed()
    }
}
