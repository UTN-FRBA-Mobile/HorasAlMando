package com.horas_al_mando.ham_android.model

data class FlightTrackDetail(
    val id                  : Long,
    val startTime           : String,
    val endTime             : String,
    val totalDurationMinutes: Int,
    val locationName        : String?,
    val path                : List<FlightPoint>? = null,
    val points              : List<FlightPoint>? = null,
) {
    val actualPath: List<FlightPoint>
        get() = path ?: points ?: emptyList()
}
