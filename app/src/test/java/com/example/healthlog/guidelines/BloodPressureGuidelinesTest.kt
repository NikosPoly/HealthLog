package com.example.healthlog.guidelines

import org.junit.Assert.assertEquals
import org.junit.Test

class BloodPressureGuidelinesTest {

    @Test
    fun lowBloodPressure_whenSystolicBelow90() {
        assertEquals(BloodPressureStage.LOW, evaluateBloodPressure(89.0, 65.0))
    }

    @Test
    fun lowBloodPressure_whenDiastolicBelow60() {
        assertEquals(BloodPressureStage.LOW, evaluateBloodPressure(110.0, 59.0))
    }

    @Test
    fun lowBloodPressure_whenBothBelowThreshold() {
        assertEquals(BloodPressureStage.LOW, evaluateBloodPressure(85.0, 55.0))
    }

    @Test
    fun normalBloodPressure_atLowerBoundary() {
        assertEquals(BloodPressureStage.NORMAL, evaluateBloodPressure(90.0, 60.0))
    }

    @Test
    fun normalBloodPressure_typicalReading() {
        assertEquals(BloodPressureStage.NORMAL, evaluateBloodPressure(115.0, 75.0))
    }

    @Test
    fun normalBloodPressure_atUpperBoundary() {
        assertEquals(BloodPressureStage.NORMAL, evaluateBloodPressure(129.0, 84.0))
    }

    @Test
    fun highNormal_whenSystolicAtOrAbove130() {
        assertEquals(BloodPressureStage.HIGH_NORMAL, evaluateBloodPressure(130.0, 80.0))
        assertEquals(BloodPressureStage.HIGH_NORMAL, evaluateBloodPressure(139.0, 84.0))
    }

    @Test
    fun highNormal_whenDiastolicAtOrAbove85() {
        assertEquals(BloodPressureStage.HIGH_NORMAL, evaluateBloodPressure(120.0, 85.0))
        assertEquals(BloodPressureStage.HIGH_NORMAL, evaluateBloodPressure(125.0, 89.0))
    }

    @Test
    fun grade1Hypertension_whenSystolicAtOrAbove140() {
        assertEquals(BloodPressureStage.GRADE_1_HYPERTENSION, evaluateBloodPressure(140.0, 80.0))
        assertEquals(BloodPressureStage.GRADE_1_HYPERTENSION, evaluateBloodPressure(159.0, 84.0))
    }

    @Test
    fun grade1Hypertension_whenDiastolicAtOrAbove90() {
        assertEquals(BloodPressureStage.GRADE_1_HYPERTENSION, evaluateBloodPressure(125.0, 90.0))
        assertEquals(BloodPressureStage.GRADE_1_HYPERTENSION, evaluateBloodPressure(135.0, 99.0))
    }

    @Test
    fun grade2Hypertension_whenSystolicAtOrAbove160() {
        assertEquals(BloodPressureStage.GRADE_2_HYPERTENSION, evaluateBloodPressure(160.0, 85.0))
        assertEquals(BloodPressureStage.GRADE_2_HYPERTENSION, evaluateBloodPressure(175.0, 95.0))
    }

    @Test
    fun grade2Hypertension_whenDiastolicAtOrAbove100() {
        assertEquals(BloodPressureStage.GRADE_2_HYPERTENSION, evaluateBloodPressure(130.0, 100.0))
        assertEquals(BloodPressureStage.GRADE_2_HYPERTENSION, evaluateBloodPressure(150.0, 115.0))
    }

    @Test
    fun grade2Hypertension_atCrisisThresholdBoundary() {
        // Exactly 180/120 is Grade 2; Crisis is strictly > 180 or > 120
        assertEquals(BloodPressureStage.GRADE_2_HYPERTENSION, evaluateBloodPressure(180.0, 120.0))
    }

    @Test
    fun hypertensiveCrisis_whenSystolicAbove180() {
        assertEquals(BloodPressureStage.HYPERTENSIVE_CRISIS, evaluateBloodPressure(181.0, 90.0))
    }

    @Test
    fun hypertensiveCrisis_whenDiastolicAbove120() {
        assertEquals(BloodPressureStage.HYPERTENSIVE_CRISIS, evaluateBloodPressure(140.0, 121.0))
    }

    @Test
    fun hypertensiveCrisis_whenBothExceedCrisisThresholds() {
        assertEquals(BloodPressureStage.HYPERTENSIVE_CRISIS, evaluateBloodPressure(195.0, 125.0))
    }

    @Test
    fun higherRiskPrecedence_grade1WithLowDiastolic_classifiedAsGrade1() {
        // Isolated systolic hypertension: high systolic takes precedence over low diastolic
        assertEquals(BloodPressureStage.GRADE_1_HYPERTENSION, evaluateBloodPressure(145.0, 55.0))
    }

    @Test
    fun higherRiskPrecedence_crisisWithLowDiastolic_classifiedAsCrisis() {
        assertEquals(BloodPressureStage.HYPERTENSIVE_CRISIS, evaluateBloodPressure(190.0, 50.0))
    }
}
