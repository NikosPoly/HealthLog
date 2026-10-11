package com.example.healthdiary.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Insert
    suspend fun insert(measurement: Measurement)

    @Delete
    suspend fun delete(measurement: Measurement)

    @Query("SELECT * FROM measurements ORDER BY timestamp DESC, id DESC")
    fun getAllMeasurements(): Flow<List<Measurement>>

    @Query(
        "SELECT * FROM measurements WHERE type = :type ORDER BY timestamp DESC, id DESC"
    )
    fun getMeasurementsByType(type: String): Flow<List<Measurement>>
}
