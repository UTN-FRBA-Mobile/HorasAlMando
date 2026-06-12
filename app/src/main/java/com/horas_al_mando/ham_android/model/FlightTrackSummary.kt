package com.horas_al_mando.ham_android.model

data class FlightTrackSummary(
    val id                  : Long,
    val startTime           : String,
    val endTime             : String,
    val totalDurationMinutes: Int,
    val locationName        : String?,
)
