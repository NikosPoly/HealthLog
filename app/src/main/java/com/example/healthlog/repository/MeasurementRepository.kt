package com.example.healthlog.repository

import com.example.healthlog.database.Measurement
import com.example.healthlog.database.MeasurementDao
import kotlinx.coroutines.flow.Flow

class MeasurementRepository(
    private val dao: MeasurementDao
) {

    fun getAllMeasurements(): Flow<List<Measurement>> =
        dao.getAllMeasurements()

    suspend fun insert(measurement: Measurement) {
        dao.insert(measurement)
    }

    suspend fun delete(measurement: Measurement) {
        dao.delete(measurement)
    }
}
