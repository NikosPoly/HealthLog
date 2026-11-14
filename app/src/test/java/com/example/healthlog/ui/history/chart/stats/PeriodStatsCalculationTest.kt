package com.example.healthlog.ui.history.chart.stats

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.BodyPosition
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementArm
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.history.chart.stats.periodStats
import com.example.healthlog.ui.history.chart.stats.roundForStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodStatsCalculationTest {

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

    @Test
    fun emptyMeasurements_emptyConditionBreakdown() {
        val breakdown = periodConditionStats(emptyList(), MeasurementType.BLOOD_PRESSURE)

        assertTrue(breakdown.isEmpty)
        assertTrue(breakdown.armDistribution.isEmpty())
        assertTrue(breakdown.positionDistribution.isEmpty())
        assertTrue(breakdown.mealDistribution.isEmpty())
        assertNull(breakdown.arrhythmiaStats)
    }

    @Test
    fun bloodPressure_conditionBreakdown_armPositionAndArrhythmia() {
        val measurements = listOf(
            Measurement(
                id = 1,
                type = MeasurementType.BLOOD_PRESSURE,
                value = 120.0,
                secondaryValue = 80.0,
                arm = MeasurementArm.LEFT.name,
                bodyPosition = BodyPosition.SITTING.name,
                hasArrhythmia = false
            ),
            Measurement(
                id = 2,
                type = MeasurementType.BLOOD_PRESSURE,
                value = 125.0,
                secondaryValue = 82.0,
                arm = MeasurementArm.LEFT.name,
                bodyPosition = BodyPosition.SITTING.name,
                hasArrhythmia = true
            ),
            Measurement(
                id = 3,
                type = MeasurementType.BLOOD_PRESSURE,
                value = 130.0,
                secondaryValue = 85.0,
                arm = MeasurementArm.RIGHT.name,
                bodyPosition = BodyPosition.STANDING.name,
                hasArrhythmia = false
            ),
            Measurement(
                id = 4,
                type = MeasurementType.BLOOD_PRESSURE,
                value = 118.0,
                secondaryValue = 78.0,
                arm = null,
                bodyPosition = null,
                hasArrhythmia = null
            )
        )

        val breakdown = periodConditionStats(measurements, MeasurementType.BLOOD_PRESSURE)

        // Arms: 2 Left, 1 Right (4th is null, total = 3)
        assertEquals(2, breakdown.armDistribution.size)
        val leftArm = breakdown.armDistribution[0]
        assertEquals(MeasurementArm.LEFT, leftArm.value)
        assertEquals(2, leftArm.count)
        assertEquals(3, leftArm.total)
        assertEquals(67, leftArm.percentage) // 2/3 = 66.67% -> 67%

        val rightArm = breakdown.armDistribution[1]
        assertEquals(MeasurementArm.RIGHT, rightArm.value)
        assertEquals(1, rightArm.count)
        assertEquals(3, rightArm.total)
        assertEquals(33, rightArm.percentage) // 1/3 = 33.33% -> 33%

        // Body Position: 2 Sitting, 1 Standing (4th is null, total = 3)
        assertEquals(2, breakdown.positionDistribution.size)
        val sitting = breakdown.positionDistribution[0]
        assertEquals(BodyPosition.SITTING, sitting.value)
        assertEquals(2, sitting.count)
        assertEquals(3, sitting.total)
        assertEquals(67, sitting.percentage)

        val standing = breakdown.positionDistribution[1]
        assertEquals(BodyPosition.STANDING, standing.value)
        assertEquals(1, standing.count)
        assertEquals(3, standing.total)
        assertEquals(33, standing.percentage)

        // Arrhythmia: 1 detected out of 3 recorded
        assertNotNull(breakdown.arrhythmiaStats)
        assertEquals(1, breakdown.arrhythmiaStats!!.detectedCount)
        assertEquals(3, breakdown.arrhythmiaStats!!.totalRecorded)
        assertEquals(33, breakdown.arrhythmiaStats!!.percentage)

        // Meal distribution should be empty for blood pressure
        assertTrue(breakdown.mealDistribution.isEmpty())
    }

    @Test
    fun bloodSugar_conditionBreakdown_mealContext() {
        val measurements = listOf(
            Measurement(
                id = 1,
                type = MeasurementType.BLOOD_SUGAR,
                value = 95.0,
                mealContext = MealContext.BEFORE_MEAL.name
            ),
            Measurement(
                id = 2,
                type = MeasurementType.BLOOD_SUGAR,
                value = 100.0,
                mealContext = MealContext.BEFORE_MEAL.name
            ),
            Measurement(
                id = 3,
                type = MeasurementType.BLOOD_SUGAR,
                value = 140.0,
                mealContext = MealContext.AFTER_MEAL_2H.name
            ),
            Measurement(
                id = 4,
                type = MeasurementType.BLOOD_SUGAR,
                value = 110.0,
                mealContext = null
            )
        )

        val breakdown = periodConditionStats(measurements, MeasurementType.BLOOD_SUGAR)

        assertEquals(2, breakdown.mealDistribution.size)
        val before = breakdown.mealDistribution[0]
        assertEquals(MealContext.BEFORE_MEAL, before.value)
        assertEquals(2, before.count)
        assertEquals(3, before.total)
        assertEquals(67, before.percentage)

        val after = breakdown.mealDistribution[1]
        assertEquals(MealContext.AFTER_MEAL_2H, after.value)
        assertEquals(1, after.count)
        assertEquals(3, after.total)
        assertEquals(33, after.percentage)

        assertTrue(breakdown.armDistribution.isEmpty())
        assertTrue(breakdown.positionDistribution.isEmpty())
        assertNull(breakdown.arrhythmiaStats)
    }

    @Test
    fun pulse_conditionBreakdown_arrhythmiaOnly() {
        val measurements = listOf(
            Measurement(id = 1, type = MeasurementType.PULSE, value = 72.0, hasArrhythmia = true),
            Measurement(id = 2, type = MeasurementType.PULSE, value = 68.0, hasArrhythmia = true)
        )

        val breakdown = periodConditionStats(measurements, MeasurementType.PULSE)

        assertNotNull(breakdown.arrhythmiaStats)
        assertEquals(2, breakdown.arrhythmiaStats!!.detectedCount)
        assertEquals(2, breakdown.arrhythmiaStats!!.totalRecorded)
        assertEquals(100, breakdown.arrhythmiaStats!!.percentage)
        assertTrue(breakdown.armDistribution.isEmpty())
        assertTrue(breakdown.positionDistribution.isEmpty())
        assertTrue(breakdown.mealDistribution.isEmpty())
    }

    @Test
    fun weightAndHeight_alwaysEmptyBreakdown() {
        val weightEntry = Measurement(
            id = 1,
            type = MeasurementType.WEIGHT,
            value = 75.0,
            arm = MeasurementArm.LEFT.name,
            hasArrhythmia = true
        )
        val breakdown = periodConditionStats(listOf(weightEntry), MeasurementType.WEIGHT)

        assertTrue(breakdown.isEmpty)
    }

    @Test
    fun periodStats_includesConditionBreakdown() {
        val bp = Measurement(
            id = 1,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
            arm = MeasurementArm.LEFT.name
        )
        val stats = periodStats(listOf(bp), MeasurementType.BLOOD_PRESSURE)

        assertEquals(1, stats.conditions.armDistribution.size)
        assertEquals(MeasurementArm.LEFT, stats.conditions.armDistribution[0].value)
        assertEquals(100, stats.conditions.armDistribution[0].percentage)
    }
}
