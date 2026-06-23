package com.horas_al_mando.ham_android.network

import com.horas_al_mando.ham_android.model.FlightTrackDetail
import com.horas_al_mando.ham_android.model.FlightTrackSummary

class FlightTrackRepository(private val api: FlightApi) {

    suspend fun getTracks(): Result<List<FlightTrackSummary>> = runCatching {
        api.getFlightTracks().body() ?: emptyList()
    }

    suspend fun getTrackDetail(id: Long): Result<FlightTrackDetail> = runCatching {
        api.getFlightTrackDetail(id).body()
            ?: error("FlightTrack $id not found")
    }

    suspend fun getGhostTrack(circuitRunId: Long): Result<FlightTrackDetail> = runCatching {
        api.getGhostTrack(circuitRunId).body()
            ?: error("Ghost track for run $circuitRunId not found")
    }
}
