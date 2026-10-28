package com.example.healthlog.model

import com.example.healthlog.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The rules of MeasurementType that are otherwise only written in comments.
// If one of these fails, a change to the enum broke an assumption of the app or the database.
class MeasurementTypeTest {

    @Test
    fun constantNames_matchWhatTheDatabaseStores() {
        // The database stores these names. Renaming or removing one needs a migration
        // (see HealthDatabase.kt); adding a new one doesn't, but then add it here too.
        assertEquals(
            listOf("BLOOD_PRESSURE", "PULSE", "BLOOD_SUGAR", "WEIGHT", "HEIGHT"),
            MeasurementType.entries.map { it.name }
        )
    }

    @Test
    fun everyType_hasOneToThreeFields() {
        // The values are stored in three columns: value, secondaryValue, tertiaryValue.
        MeasurementType.entries.forEach { type ->
            assertTrue("$type has ${type.fields.size} fields", type.fields.size in 1..3)
        }
    }

    @Test
    fun bloodPressure_isSystolicDiastolicPulse_inThisOrder() {
        // The order decides the column each value is stored in.
        val fields = MeasurementType.BLOOD_PRESSURE.fields
        assertEquals(
            listOf(R.string.field_systolic, R.string.field_diastolic, R.string.type_pulse),
            fields.map { it.label }
        )
        assertEquals(listOf("mmHg", "mmHg", "bpm"), fields.map { it.unit })
    }

    @Test
    fun singleValueTypes_useTheTypeLabelForTheirField() {
        MeasurementType.entries
            .filter { it.fields.size == 1 }
            .forEach { type -> assertEquals("$type", type.label, type.fields[0].label) }
    }

    @Test
    fun everyRange_isPositive_andMinIsBelowMax() {
        MeasurementType.entries.forEach { type ->
            type.fields.forEach { field ->
                assertTrue("$type: min must be positive", field.minValue > 0)
                assertTrue("$type: min must be below max", field.minValue < field.maxValue)
            }
        }
    }

    @Test
    fun maxDecimals_isNeverNegative() {
        MeasurementType.entries.forEach { type ->
            type.fields.forEach { field ->
                assertTrue("$type: maxDecimals must be 0 or more", field.maxDecimals >= 0)
            }
        }
    }

    @Test
    fun systolicMinimum_isAboveDiastolicMinimum() {
        // Diastolic must be lower than systolic, so the lowest systolic value must still
        // leave room for a valid diastolic value below it.
        val (systolic, diastolic) = MeasurementType.BLOOD_PRESSURE.fields
        assertTrue(systolic.minValue > diastolic.minValue)
    }

    @Test
    fun onlyWeightAllowsDecimals() {
        assertEquals(
            listOf(MeasurementType.WEIGHT),
            MeasurementType.entries.filter { type -> type.fields.any { it.allowsDecimals } }
        )
    }
}
