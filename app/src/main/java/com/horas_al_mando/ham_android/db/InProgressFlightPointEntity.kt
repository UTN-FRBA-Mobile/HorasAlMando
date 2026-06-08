package com.horas_al_mando.ham_android.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "in_progress_points")
data class InProgressFlightPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lat: Double,
    val lng: Double,
    val timestamp: String,
    val altitude: Double,
    val speed: Double,
    val heading: Double,
    val verticalSpeed: Double,
    val pressure: Double
)
