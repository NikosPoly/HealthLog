package com.example.healthlog.ui.history.chart.vico

import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetRangeBandTest {

    @Test
    fun targetRangesFor_pulseReturnsRestingNormalRange() {
        val ranges = targetRangesFor(MeasurementType.PULSE)
        assertEquals(1, ranges.size)
        assertEquals(60.0, ranges[0].start, 0.001)
        assertEquals(100.0, ranges[0].endInclusive, 0.001)
    }

    @Test
    fun targetRangesFor_weightWithHeightReturnsHealthyRange() {
        val ranges = targetRangesFor(MeasurementType.WEIGHT, latestHeightCm = 180.0)
        assertEquals(1, ranges.size)
        val h2 = 1.8 * 1.8
        assertEquals(18.5 * h2, ranges[0].start, 0.001)
        assertEquals(24.9 * h2, ranges[0].endInclusive, 0.001)
    }

    @Test
    fun targetRangesFor_weightWithoutHeightReturnsEmpty() {
        assertTrue(targetRangesFor(MeasurementType.WEIGHT, latestHeightCm = null).isEmpty())
        assertTrue(targetRangesFor(MeasurementType.WEIGHT, latestHeightCm = 0.0).isEmpty())
    }

    @Test
    fun targetRangesFor_otherTypesReturnEmpty() {
        assertTrue(targetRangesFor(MeasurementType.BLOOD_PRESSURE).isEmpty())
        assertTrue(targetRangesFor(MeasurementType.BLOOD_SUGAR).isEmpty())
        assertTrue(targetRangesFor(MeasurementType.HEIGHT).isEmpty())
    }

    @Test
    fun stageBandsFor_pulseReturnsThreeClinicalBands() {
        val bands = stageBandsFor(MeasurementType.PULSE, dark = false)
        assertEquals(3, bands.size)

        // Bradycardia (< 60 bpm)
        assertEquals(0.0, bands[0].range.start, 0.001)
        assertEquals(60.0, bands[0].range.endInclusive, 0.001)

        // Normal resting (60–100 bpm)
        assertEquals(60.0, bands[1].range.start, 0.001)
        assertEquals(100.0, bands[1].range.endInclusive, 0.001)

        // Tachycardia (> 100 bpm) - extends to infinity
        assertEquals(100.0, bands[2].range.start, 0.001)
        assertEquals(Double.POSITIVE_INFINITY, bands[2].range.endInclusive, 0.0)
    }

    @Test
    fun stageBandsFor_weightWithHeightReturnsFourBmiBands() {
        val heightCm = 175.0
        val bands = stageBandsFor(MeasurementType.WEIGHT, dark = false, latestHeightCm = heightCm)
        assertEquals(4, bands.size)

        val h2 = 1.75 * 1.75

        // Underweight (BMI < 18.5)
        assertEquals(0.0, bands[0].range.start, 0.001)
        assertEquals(18.5 * h2, bands[0].range.endInclusive, 0.001)

        // Normal (BMI 18.5–24.9)
        assertEquals(18.5 * h2, bands[1].range.start, 0.001)
        assertEquals(25.0 * h2, bands[1].range.endInclusive, 0.001)

        // Overweight (BMI 25.0–29.9)
        assertEquals(25.0 * h2, bands[2].range.start, 0.001)
        assertEquals(30.0 * h2, bands[2].range.endInclusive, 0.001)

        // Obese (BMI ≥ 30.0) - extends to infinity
        assertEquals(30.0 * h2, bands[3].range.start, 0.001)
        assertEquals(Double.POSITIVE_INFINITY, bands[3].range.endInclusive, 0.0)
    }

    @Test
    fun stageBandsFor_weightWithoutHeightReturnsEmpty() {
        assertTrue(stageBandsFor(MeasurementType.WEIGHT, dark = false, latestHeightCm = null).isEmpty())
        assertTrue(stageBandsFor(MeasurementType.WEIGHT, dark = false, latestHeightCm = -10.0).isEmpty())
    }

    @Test
    fun stageBandsFor_otherTypesReturnEmpty() {
        assertTrue(stageBandsFor(MeasurementType.BLOOD_PRESSURE, dark = false).isEmpty())
        assertTrue(stageBandsFor(MeasurementType.BLOOD_SUGAR, dark = false).isEmpty())
        assertTrue(stageBandsFor(MeasurementType.HEIGHT, dark = false).isEmpty())
    }

    @Test
    fun paddedRange_anchorsAroundTargetRange() {
        // Data is only 70..75 bpm, but target is 60..100 bpm
        val targetRanges = listOf(60.0..100.0)
        val provider = PaddedRange(minMargin = 5.0, targetRanges = targetRanges)

        val minY = provider.getMinY(minY = 70.0, maxY = 75.0)
        val maxY = provider.getMaxY(minY = 70.0, maxY = 75.0)

        // Min must be <= 60 - margin
        assertTrue("minY should be <= 60.0", minY <= 60.0)
        // Max must be >= 100 + margin
        assertTrue("maxY should be >= 100.0", maxY >= 100.0)
    }

    @Test
    fun stageLabelResFor_resolvesPulseStages() {
        val brady = Measurement(type = MeasurementType.PULSE, value = 55.0)
        assertEquals(R.string.pulse_zone_bradycardia, stageLabelResFor(brady))

        val normal = Measurement(type = MeasurementType.PULSE, value = 72.0)
        assertEquals(R.string.pulse_zone_normal, stageLabelResFor(normal))

        val tachy = Measurement(type = MeasurementType.PULSE, value = 110.0)
        assertEquals(R.string.pulse_zone_tachycardia, stageLabelResFor(tachy))
    }

    @Test
    fun stageLabelResFor_resolvesWeightStagesWithHeight() {
        val heightCm = 180.0 // Normal weight is approx 60..81 kg
        val normalWeight = Measurement(type = MeasurementType.WEIGHT, value = 72.0)
        assertEquals(R.string.bmi_category_normal, stageLabelResFor(normalWeight, heightCm))

        val obeseWeight = Measurement(type = MeasurementType.WEIGHT, value = 110.0)
        assertEquals(R.string.bmi_category_obese, stageLabelResFor(obeseWeight, heightCm))

        // Without height, weight stage cannot be evaluated
        assertNull(stageLabelResFor(normalWeight, null))
    }

    @Test
    fun stageLabelResFor_resolvesBloodPressureAndBloodSugarStages() {
        val bp = Measurement(
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
        )
        assertNotNull(stageLabelResFor(bp))

        val sugarFasting = Measurement(
            type = MeasurementType.BLOOD_SUGAR,
            value = 95.0,
            mealContext = "FASTING"
        )
        assertNotNull(stageLabelResFor(sugarFasting))
    }
}
