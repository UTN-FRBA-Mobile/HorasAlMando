package com.horas_al_mando.ham_android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingFlightDao {
    @Query("SELECT * FROM pending_flights WHERE id = 1")
    suspend fun get(): PendingFlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: PendingFlightEntity)

    @Query("DELETE FROM pending_flights WHERE id = 1")
    suspend fun delete()
}
