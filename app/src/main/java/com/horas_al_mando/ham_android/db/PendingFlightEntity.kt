package com.horas_al_mando.ham_android.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_flights")
data class PendingFlightEntity(
    @PrimaryKey val id: Int = 1,
    val payloadJson: String
)
