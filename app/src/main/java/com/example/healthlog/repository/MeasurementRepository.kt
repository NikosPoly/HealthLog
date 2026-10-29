package com.example.healthlog.repository

import com.example.healthlog.database.Measurement
import com.example.healthlog.database.MeasurementDao
import com.example.healthlog.model.MeasurementType
import kotlinx.coroutines.flow.Flow

class MeasurementRepository(
    private val dao: MeasurementDao
) {

    fun getAllMeasurements(): Flow<List<Measurement>> =
        dao.getAllMeasurements()

    // Saves a new measurement (see measurementsToSave for what is stored).
    // Everything is saved in one database transaction: either all of it or nothing.
    suspend fun save(measurement: Measurement) {
        dao.insertAll(measurementsToSave(measurement))
    }

    suspend fun delete(measurement: Measurement) {
        dao.delete(measurement)
    }
}

// What is stored for a new measurement. A blood pressure measurement with a pulse also
// stores its pulse as a separate Pulse entry, with the same date and time, so it appears
// in the Pulse history too. After saving, the two entries are independent:
// deleting one keeps the other.
internal fun measurementsToSave(measurement: Measurement): List<Measurement> {
    val pulse = measurement.tertiaryValue

    return if (measurement.type == MeasurementType.BLOOD_PRESSURE && pulse != null) {
        listOf(
            measurement,
            Measurement(
                type = MeasurementType.PULSE,
                value = pulse,
                timestamp = measurement.timestamp
            )
        )
    } else {
        listOf(measurement)
    }
}
