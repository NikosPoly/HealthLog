package com.example.healthlog.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.healthlog.model.MeasurementType
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    // Inserts several measurements in one transaction: either all of them or none.
    @Insert
    suspend fun insertAll(measurements: List<Measurement>)

    @Delete
    suspend fun delete(measurement: Measurement)

    // The measurements of one type, newest first (same time: the later saved first).
    @Query(
        "SELECT * FROM measurements WHERE type = :type ORDER BY timestamp DESC, id DESC"
    )
    fun getMeasurementsByType(type: MeasurementType): Flow<List<Measurement>>

    // A single measurement by its ID, or null if it was deleted or not found.
    @Query("SELECT * FROM measurements WHERE id = :id")
    fun getMeasurementById(id: Long): Flow<Measurement?>

    // The most recently recorded height value in cm, or null if no height has been logged.
    @Query(
        "SELECT value FROM measurements WHERE type = 'HEIGHT' ORDER BY timestamp DESC, id DESC LIMIT 1"
    )
    fun getLatestHeight(): Flow<Double?>
}
