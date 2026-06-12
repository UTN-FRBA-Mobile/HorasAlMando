package com.horas_al_mando.ham_android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InProgressFlightDao {
    @Insert
    suspend fun insert(point: InProgressFlightPointEntity)

    @Query("SELECT * FROM in_progress_points ORDER BY id ASC")
    suspend fun getAll(): List<InProgressFlightPointEntity>

    @Query("SELECT COUNT(*) FROM in_progress_points")
    suspend fun count(): Int

    @Query("DELETE FROM in_progress_points")
    suspend fun deleteAll()
}
