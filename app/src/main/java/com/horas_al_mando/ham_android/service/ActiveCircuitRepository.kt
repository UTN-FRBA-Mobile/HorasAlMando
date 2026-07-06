package com.horas_al_mando.ham_android.service

import android.media.AudioManager
import android.media.ToneGenerator
import com.horas_al_mando.ham_android.model.CircuitDetail
import com.horas_al_mando.ham_android.model.CircuitRun
import com.horas_al_mando.ham_android.model.CreateCircuitRunRequest
import com.horas_al_mando.ham_android.model.WaypointTime
import com.horas_al_mando.ham_android.network.CircuitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

sealed class RunUploadState {
    object Uploading : RunUploadState()
    data class Success(val run: CircuitRun) : RunUploadState()
    data class Error(val message: String) : RunUploadState()
}

object ActiveCircuitRepository {

    private const val ARRIVAL_RADIUS_METERS = 150.0
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    private val _activeCircuit = MutableStateFlow<CircuitDetail?>(null)
    val activeCircuit: StateFlow<CircuitDetail?> = _activeCircuit.asStateFlow()

    private val _ghostRun = MutableStateFlow<CircuitRun?>(null)
    val ghostRun: StateFlow<CircuitRun?> = _ghostRun.asStateFlow()

    private val _runInProgress = MutableStateFlow(false)
    val runInProgress: StateFlow<Boolean> = _runInProgress.asStateFlow()

    private val _nextWaypointIndex = MutableStateFlow(0)
    val nextWaypointIndex: StateFlow<Int> = _nextWaypointIndex.asStateFlow()

    private val _bearingToNext = MutableStateFlow<Float?>(null)
    val bearingToNext: StateFlow<Float?> = _bearingToNext.asStateFlow()

    private val _uploadState = MutableStateFlow<RunUploadState?>(null)
    val uploadState: StateFlow<RunUploadState?> = _uploadState.asStateFlow()

    private val arrivals = mutableListOf<WaypointTime>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repository = CircuitRepository()

    private var toneGenerator: ToneGenerator? = null

    fun setGhostRun(run: CircuitRun?) {
        _ghostRun.value = run
    }

    fun arm(circuit: CircuitDetail) {
        _activeCircuit.value = circuit
        _runInProgress.value = false
        _nextWaypointIndex.value = 0
        _bearingToNext.value = null
        _uploadState.value = null
        arrivals.clear()
    }

    fun disarm() {
        _activeCircuit.value = null
        _ghostRun.value = null
        _runInProgress.value = false
        _nextWaypointIndex.value = 0
        _bearingToNext.value = null
        _uploadState.value = null
        arrivals.clear()
        releaseTone()
    }

    fun beginRun() {
        if (_activeCircuit.value == null) return
        _runInProgress.value = true
        _nextWaypointIndex.value = 0
        arrivals.clear()
        _uploadState.value = null
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        }
    }

    fun onLocation(lat: Double, lng: Double) {
        val circuit = _activeCircuit.value ?: return
        if (!_runInProgress.value) return
        val waypoints = circuit.waypoints
        val index = _nextWaypointIndex.value
        if (index >= waypoints.size) return

        val target = waypoints[index]
        _bearingToNext.value = bearingDegrees(lat, lng, target.lat, target.lng).toFloat()

        val distance = haversineMeters(lat, lng, target.lat, target.lng)
        if (distance <= ARRIVAL_RADIUS_METERS) {
            val elapsed = FlightRepository.elapsedSeconds.value.toInt()
            arrivals.add(WaypointTime(target.order, target.lat, target.lng, elapsed))

            val isLast = index == waypoints.size - 1
            if (isLast) {
                playFinishTone()
                finishRun(circuit.id, elapsed)
            } else {
                playWaypointTone()
                _nextWaypointIndex.value = index + 1
            }
        }
    }

    private fun finishRun(circuitId: Long, totalSeconds: Int) {
        _runInProgress.value = false
        _uploadState.value = RunUploadState.Uploading
        val body = CreateCircuitRunRequest(
            totalDurationSeconds = totalSeconds,
            waypointTimes = arrivals.toList(),
            clientFlightId = FlightRepository.getClientFlightId().ifBlank { null },
        )
        scope.launch {
            repository.uploadRun(circuitId, body)
                .onSuccess { _uploadState.value = RunUploadState.Success(it) }
                .onFailure { _uploadState.value = RunUploadState.Error(it.localizedMessage ?: "error") }
        }
    }

    fun acknowledgeResult() {
        _uploadState.value = null
        _activeCircuit.value = null
        _ghostRun.value = null
        _nextWaypointIndex.value = 0
        _bearingToNext.value = null
        arrivals.clear()
        releaseTone()
    }

    private fun playWaypointTone() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    }

    private fun playFinishTone() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
    }

    private fun releaseTone() {
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    private fun bearingDegrees(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLng = Math.toRadians(lng2 - lng1)
        val y = sin(dLng) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLng)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
}
