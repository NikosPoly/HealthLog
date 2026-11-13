package com.example.healthlog.guidelines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BmiGuidelinesTest {

    private val height = 180.0 // 1.80 m
    private val heightM = 1.80
    private val expectedMinWeight = 18.5 * (heightM * heightM) // 59.94 kg
    private val expectedMaxWeight = 24.9 * (heightM * heightM) // 80.676 kg

    @Test
    fun normalWeight_calculatesCorrectBmiAndZeroDelta() {
        val result = calculateBmi(weightKg = 70.0, heightCm = height)
        assertNotNull(result)
        result!!

        val expectedBmi = 70.0 / (heightM * heightM)
        assertEquals(expectedBmi, result.bmi, 0.001)
        assertEquals(BmiCategory.NORMAL, result.category)
        assertTrue(result.isHealthyWeight)
        assertEquals(expectedMinWeight, result.healthyWeightMinKg, 0.001)
        assertEquals(expectedMaxWeight, result.healthyWeightMaxKg, 0.001)
        assertEquals(0.0, result.weightDeltaKg, 0.001)
    }

    @Test
    fun overweight_hasPositiveDeltaAboveMaxWeight() {
        val weight = 90.0
        val result = calculateBmi(weightKg = weight, heightCm = height)
        assertNotNull(result)
        result!!

        assertEquals(BmiCategory.OVERWEIGHT, result.category)
        assertFalse(result.isHealthyWeight)
        val expectedDelta = weight - expectedMaxWeight
        assertEquals(expectedDelta, result.weightDeltaKg, 0.001)
        assertTrue(result.weightDeltaKg > 0.0)
    }

    @Test
    fun underweight_hasNegativeDeltaBelowMinWeight() {
        val weight = 50.0
        val result = calculateBmi(weightKg = weight, heightCm = height)
        assertNotNull(result)
        result!!

        assertEquals(BmiCategory.UNDERWEIGHT, result.category)
        assertFalse(result.isHealthyWeight)
        val expectedDelta = weight - expectedMinWeight
        assertEquals(expectedDelta, result.weightDeltaKg, 0.001)
        assertTrue(result.weightDeltaKg < 0.0)
    }

    @Test
    fun obese_classificationAbove30() {
        val result = calculateBmi(weightKg = 105.0, heightCm = height)
        assertNotNull(result)
        assertEquals(BmiCategory.OBESE, result!!.category)
        assertFalse(result.isHealthyWeight)
    }

    @Test
    fun oxfordNewBmi_calculatesExpectedFormula() {
        val weight = 70.0
        val result = calculateBmi(weightKg = weight, heightCm = height)
        assertNotNull(result)
        val expectedNewBmi = 1.3 * weight / Math.pow(heightM, 2.5)
        assertEquals(expectedNewBmi, result!!.newBmi, 0.001)
    }

    @Test
    fun invalidInputs_returnNull() {
        assertNull(calculateBmi(weightKg = 0.0, heightCm = 175.0))
        assertNull(calculateBmi(weightKg = -70.0, heightCm = 175.0))
        assertNull(calculateBmi(weightKg = 70.0, heightCm = 0.0))
        assertNull(calculateBmi(weightKg = 70.0, heightCm = -175.0))
    }

    @Test
    fun healthyWeightRange_returnsExpectedBounds() {
        val range = calculateHealthyWeightRange(height)
        assertNotNull(range)
        assertEquals(expectedMinWeight, range!!.start, 0.001)
        assertEquals(expectedMaxWeight, range.endInclusive, 0.001)
    }

    @Test
    fun healthyWeightRange_invalidHeight_returnsNull() {
        assertNull(calculateHealthyWeightRange(0.0))
        assertNull(calculateHealthyWeightRange(-170.0))
    }
}
