package com.horas_al_mando.ham_android.model

data class FlightSyncPayload(
    val pilotId  : Long,
    val startTime: String,
    val endTime  : String,
    val path     : List<FlightPoint>,
)
