package com.example.healthlog.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.healthlog.model.MeasurementType
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Insert
    suspend fun insert(measurement: Measurement)

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
}
