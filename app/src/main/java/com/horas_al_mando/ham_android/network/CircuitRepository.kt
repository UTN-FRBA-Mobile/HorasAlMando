package com.horas_al_mando.ham_android.network

import com.horas_al_mando.ham_android.model.CircuitDetail
import com.horas_al_mando.ham_android.model.CircuitRun
import com.horas_al_mando.ham_android.model.CircuitSummary
import com.horas_al_mando.ham_android.model.CreateCircuitRequest
import com.horas_al_mando.ham_android.model.CreateCircuitRunRequest
import com.horas_al_mando.ham_android.model.Waypoint

class CircuitRepository(private val api: CircuitApi = CircuitApiService.api) {

    suspend fun getCircuits(
        lat: Double?,
        lng: Double?,
        rangeKm: Double?,
        mine: Boolean?,
    ): Result<List<CircuitSummary>> = runCatching {
        val response = api.getCircuits(lat, lng, rangeKm, mine)
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        response.body() ?: emptyList()
    }

    suspend fun getCircuit(id: Long): Result<CircuitDetail> = runCatching {
        val response = api.getCircuit(id)
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        response.body() ?: error("Circuit $id not found")
    }

    suspend fun createCircuit(name: String, waypoints: List<Waypoint>): Result<CircuitDetail> = runCatching {
        val response = api.createCircuit(CreateCircuitRequest(name, waypoints))
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        response.body() ?: error("Empty response")
    }

    suspend fun deleteCircuit(id: Long): Result<Unit> = runCatching {
        val response = api.deleteCircuit(id)
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        Unit
    }

    suspend fun getRuns(id: Long): Result<List<CircuitRun>> = runCatching {
        val response = api.getRuns(id)
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        response.body() ?: emptyList()
    }

    suspend fun uploadRun(circuitId: Long, body: CreateCircuitRunRequest): Result<CircuitRun> = runCatching {
        val response = api.createRun(circuitId, body)
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        response.body() ?: error("Empty response")
    }
}
