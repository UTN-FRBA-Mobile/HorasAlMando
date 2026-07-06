package com.horas_al_mando.ham_android.model

data class Waypoint(
    val order: Int,
    val lat: Double,
    val lng: Double,
)

data class WaypointTime(
    val order: Int,
    val lat: Double,
    val lng: Double,
    val elapsedSeconds: Int,
)

data class CircuitSummary(
    val id: Long,
    val name: String,
    val creatorName: String,
    val totalDistanceKm: Double,
    val waypointCount: Int,
    val startLat: Double,
    val startLng: Double,
    val distanceFromUserKm: Double?,
    val mine: Boolean,
)

data class CircuitDetail(
    val id: Long,
    val name: String,
    val creatorName: String,
    val totalDistanceKm: Double,
    val mine: Boolean,
    val waypoints: List<Waypoint>,
)

data class CircuitRun(
    val id: Long,
    val pilotName: String,
    val totalDurationSeconds: Int,
    val rank: Int,
    val waypointTimes: List<WaypointTime>,
)

data class CreateCircuitRequest(
    val name: String,
    val waypoints: List<Waypoint>,
)

data class CreateCircuitRunRequest(
    val totalDurationSeconds: Int,
    val waypointTimes: List<WaypointTime>,
    val clientFlightId: String?,
)
