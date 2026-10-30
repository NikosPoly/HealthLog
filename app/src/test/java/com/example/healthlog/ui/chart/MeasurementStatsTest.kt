package com.example.healthlog.ui.chart

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeasurementStatsTest {

    private fun weight(id: Long, value: Double, timestamp: Long = id) =
        Measurement(id = id, type = MeasurementType.WEIGHT, value = value, timestamp = timestamp)

    private fun bloodPressure(id: Long, systolic: Double, diastolic: Double) = Measurement(
        id = id,
        type = MeasurementType.BLOOD_PRESSURE,
        value = systolic,
        secondaryValue = diastolic,
        tertiaryValue = 70.0,
        timestamp = id
    )

    @Test
    fun noMeasurements_countZero_noValues() {
        val stats = periodStats(emptyList(), MeasurementType.WEIGHT)

        assertEquals(0, stats.count)
        assertNull(stats.primary)
        assertNull(stats.secondary)
    }

    @Test
    fun oneMeasurement_isTheHighestLowestAndAverage() {
        val only = weight(1, 72.5)
        val stats = periodStats(listOf(only), MeasurementType.WEIGHT)

        assertEquals(1, stats.count)
        assertEquals(only, stats.primary?.highest)
        assertEquals(only, stats.primary?.lowest)
        assertEquals(72.5, stats.primary!!.average, 0.0)
    }

    @Test
    fun weight_highestLowestAverage() {
        val low = weight(1, 71.0)
        val high = weight(2, 74.0)
        val stats = periodStats(listOf(low, high, weight(3, 72.5)), MeasurementType.WEIGHT)

        assertEquals(3, stats.count)
        assertEquals(high, stats.primary?.highest)
        assertEquals(low, stats.primary?.lowest)
        assertEquals(72.5, stats.primary!!.average, 0.0)
        assertNull(stats.secondary)
    }

    @Test
    fun tie_choosesTheMostRecent() {
        val older = weight(1, 74.0, timestamp = 100)
        val newer = weight(2, 74.0, timestamp = 200)
        val stats = periodStats(listOf(older, newer), MeasurementType.WEIGHT)

        assertEquals(newer, stats.primary?.highest)
        assertEquals(newer, stats.primary?.lowest)
    }

    @Test
    fun bloodPressure_keepsThePairs_andAveragesBoth() {
        val a = bloodPressure(1, systolic = 130.0, diastolic = 85.0)
        val b = bloodPressure(2, systolic = 140.0, diastolic = 80.0)
        val c = bloodPressure(3, systolic = 120.0, diastolic = 90.0)
        val stats = periodStats(listOf(a, b, c), MeasurementType.BLOOD_PRESSURE)

        assertEquals(b, stats.primary?.highest)
        assertEquals(c, stats.primary?.lowest)
        assertEquals(130.0, stats.primary!!.average, 0.0)

        assertEquals(c, stats.secondary?.highest)
        assertEquals(b, stats.secondary?.lowest)
        assertEquals(85.0, stats.secondary!!.average, 0.0)
    }

    @Test
    fun wholeNumberAverage_dropsTheDecimals() {
        val pulses = listOf(70.0, 71.0).mapIndexed { i, value ->
            Measurement(id = i.toLong(), type = MeasurementType.PULSE, value = value)
        }

        assertEquals(70.0, periodStats(pulses, MeasurementType.PULSE).primary!!.average, 0.0)
    }

    @Test
    fun roundForStats_rules() {
        assertEquals(72.46, roundForStats(72.456, 2), 0.0)
        assertEquals(72.46, roundForStats(72.455, 2), 0.0)
        assertEquals(132.0, roundForStats(132.9, 0), 0.0)
    }
}
