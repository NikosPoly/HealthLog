package com.example.healthlog.model

import com.example.healthlog.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MeasurementContextTest {

    @Test
    fun measurementArm_fromNameOrNull_returnsCorrectEnum() {
        assertEquals(MeasurementArm.LEFT, MeasurementArm.fromNameOrNull("LEFT"))
        assertEquals(MeasurementArm.RIGHT, MeasurementArm.fromNameOrNull("RIGHT"))
        assertNull(MeasurementArm.fromNameOrNull(null))
        assertNull(MeasurementArm.fromNameOrNull("UNKNOWN"))
    }

    @Test
    fun bodyPosition_fromNameOrNull_returnsCorrectEnum() {
        assertEquals(BodyPosition.SITTING, BodyPosition.fromNameOrNull("SITTING"))
        assertEquals(BodyPosition.STANDING, BodyPosition.fromNameOrNull("STANDING"))
        assertEquals(BodyPosition.LYING_DOWN, BodyPosition.fromNameOrNull("LYING_DOWN"))
        assertNull(BodyPosition.fromNameOrNull(null))
        assertNull(BodyPosition.fromNameOrNull("UNKNOWN"))
    }

    @Test
    fun mealContext_fromNameOrNull_returnsCorrectEnum() {
        assertEquals(MealContext.BEFORE_MEAL, MealContext.fromNameOrNull("BEFORE_MEAL"))
        assertEquals(MealContext.AFTER_MEAL_2H, MealContext.fromNameOrNull("AFTER_MEAL_2H"))
        assertNull(MealContext.fromNameOrNull(null))
        assertNull(MealContext.fromNameOrNull("UNKNOWN"))
    }

    @Test
    fun arrhythmiaHelpers_returnCorrectResources() {
        assertEquals(R.string.arrhythmia_yes, arrhythmiaLabel(true))
        assertEquals(R.string.arrhythmia_no, arrhythmiaLabel(false))
        assertEquals(R.drawable.ic_arrhythmia_yes, arrhythmiaIcon(true))
        assertEquals(R.drawable.ic_arrhythmia_no, arrhythmiaIcon(false))
    }

    @Test
    fun allEnums_haveValidResourceIds() {
        MeasurementArm.entries.forEach {
            assertNotNull(it.label)
            assertNotNull(it.iconRes)
        }
        BodyPosition.entries.forEach {
            assertNotNull(it.label)
            assertNotNull(it.iconRes)
        }
        MealContext.entries.forEach {
            assertNotNull(it.label)
            assertNotNull(it.iconRes)
        }
    }
}
