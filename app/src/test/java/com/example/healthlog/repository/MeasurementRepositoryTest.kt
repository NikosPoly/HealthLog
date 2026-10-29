package com.example.healthlog.repository

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Test

// What is stored when a measurement is saved (measurementsToSave).
class MeasurementRepositoryTest {

    @Test
    fun bloodPressureWithPulse_alsoSavesThePulseAsItsOwnEntry() {
        val bloodPressure = Measurement(
            type = MeasurementType.BLOOD_PRESSURE,
            value = 130.0,
            secondaryValue = 70.0,
            tertiaryValue = 80.0,
            timestamp = 1000L
        )

        val saved = measurementsToSave(bloodPressure)

        assertEquals(2, saved.size)
        assertEquals(bloodPressure, saved[0])

        val pulse = saved[1]
        assertEquals(MeasurementType.PULSE, pulse.type)
        assertEquals(80.0, pulse.value, 0.0)
        assertEquals(null, pulse.secondaryValue)
        assertEquals(null, pulse.tertiaryValue)
        assertEquals(1000L, pulse.timestamp)
    }

    @Test
    fun bloodPressureWithoutPulse_isSavedAlone() {
        // Can't happen through the entry dialog (pulse is required), but older code paths
        // or future imports might create one.
        val bloodPressure = Measurement(
            type = MeasurementType.BLOOD_PRESSURE,
            value = 130.0,
            secondaryValue = 70.0
        )

        assertEquals(listOf(bloodPressure), measurementsToSave(bloodPressure))
    }

    @Test
    fun otherTypes_areSavedAlone() {
        MeasurementType.entries
            .filter { it != MeasurementType.BLOOD_PRESSURE }
            .forEach { type ->
                val measurement = Measurement(type = type, value = 50.0)
                assertEquals("$type", listOf(measurement), measurementsToSave(measurement))
            }
    }
}
