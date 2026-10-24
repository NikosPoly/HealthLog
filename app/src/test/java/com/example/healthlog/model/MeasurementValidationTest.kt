package com.example.healthlog.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeasurementValidationTest {

    private val pulse = MeasurementType.PULSE.fields[0]
    private val weight = MeasurementType.WEIGHT.fields[0]

    // ---- single field ----

    @Test
    fun emptyField_isRequired() {
        assertEquals(ValidationError.Required, validateField(pulse, ""))
    }

    @Test
    fun valueEndingWithDot_isIncompleteDecimal() {
        assertEquals(ValidationError.IncompleteDecimal, validateField(weight, "72."))
    }

    @Test
    fun notANumber_isInvalidNumber() {
        assertEquals(ValidationError.InvalidNumber, validateField(pulse, "abc"))
    }

    @Test
    fun valueInsideRange_isValid() {
        assertNull(validateField(pulse, "72"))
    }

    @Test
    fun rangeLimits_areIncluded() {
        assertNull(validateField(pulse, "30"))
        assertNull(validateField(pulse, "300"))
    }

    @Test
    fun valueBelowRange_isOutOfRange() {
        assertEquals(ValidationError.OutOfRange(30.0, 300.0), validateField(pulse, "29"))
    }

    @Test
    fun valueAboveRange_isOutOfRange() {
        assertEquals(ValidationError.OutOfRange(30.0, 300.0), validateField(pulse, "30000"))
    }

    @Test
    fun weightWithTwoDecimals_isValid() {
        assertNull(validateField(weight, "80.55"))
    }

    // ---- blood pressure ----

    @Test
    fun normalBloodPressure_isValid() {
        val errors = validateMeasurement(MeasurementType.BLOOD_PRESSURE, listOf("120", "80", "70"))
        assertEquals(listOf(null, null, null), errors)
    }

    @Test
    fun diastolicHigherThanSystolic_errorOnDiastolic() {
        val errors = validateMeasurement(MeasurementType.BLOOD_PRESSURE, listOf("80", "120", "70"))
        assertEquals(listOf(null, ValidationError.DiastolicNotBelowSystolic, null), errors)
    }

    @Test
    fun diastolicEqualToSystolic_errorOnDiastolic() {
        val errors = validateMeasurement(MeasurementType.BLOOD_PRESSURE, listOf("120", "120", "70"))
        assertEquals(listOf(null, ValidationError.DiastolicNotBelowSystolic, null), errors)
    }

    @Test
    fun diastolicOutOfRange_showsRangeErrorNotComparison() {
        val errors = validateMeasurement(MeasurementType.BLOOD_PRESSURE, listOf("120", "200", "70"))
        assertEquals(listOf(null, ValidationError.OutOfRange(30.0, 150.0), null), errors)
    }

    @Test
    fun emptyFields_areAllRequired() {
        val errors = validateMeasurement(MeasurementType.BLOOD_PRESSURE, listOf("", "", ""))
        assertEquals(
            listOf(ValidationError.Required, ValidationError.Required, ValidationError.Required),
            errors
        )
    }
}
