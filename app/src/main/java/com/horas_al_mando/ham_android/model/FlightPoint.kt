package com.horas_al_mando.ham_android.model

data class FlightPoint(
    val lat          : Double,
    val lng          : Double,
    val timestamp    : String,
    val altitude     : Double,
    val speed        : Double,
    val heading      : Double,
    val verticalSpeed: Double,
    val pressure     : Double,
)
