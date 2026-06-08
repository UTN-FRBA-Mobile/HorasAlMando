package com.horas_al_mando.ham_android.model

data class PilotLocation(
    val pilotId : Long,
    val name    : String,
    val lat     : Double,
    val lng     : Double,
    val altitude: Double,
    val speed   : Double,
    val heading : Double,
)
