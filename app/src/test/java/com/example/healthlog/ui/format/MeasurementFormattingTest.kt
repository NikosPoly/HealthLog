package com.example.healthlog.ui.format

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Test

// The value text shown in the history and the delete confirmation (formatMeasurementValue).
class MeasurementFormattingTest {

    // ---- weight: always two decimals, with the language's separator ----

    @Test
    fun weight_inEnglish_hasTwoDecimalsWithDot() {
        val weight = Measurement(type = MeasurementType.WEIGHT, value = 72.5)
        assertEquals("72.50 kg", formatMeasurementValue(weight, '.'))
    }

    @Test
    fun weight_inGreek_hasTwoDecimalsWithComma() {
        val weight = Measurement(type = MeasurementType.WEIGHT, value = 72.5)
        assertEquals("72,50 kg", formatMeasurementValue(weight, ','))
    }

    @Test
    fun weight_wholeNumber_stillHasTwoDecimals() {
        val weight = Measurement(type = MeasurementType.WEIGHT, value = 80.0)
        assertEquals("80.00 kg", formatMeasurementValue(weight, '.'))
    }

    @Test
    fun weight_twoDecimals_areKept() {
        val weight = Measurement(type = MeasurementType.WEIGHT, value = 80.55)
        assertEquals("80,55 kg", formatMeasurementValue(weight, ','))
    }

    // ---- whole-number types: no ".0" ----

    @Test
    fun height_wholeNumber_hasNoDecimals() {
        val height = Measurement(type = MeasurementType.HEIGHT, value = 180.0)
        assertEquals("180 cm", formatMeasurementValue(height, '.'))
    }

    @Test
    fun pulseAndBloodSugar_showTheirUnits() {
        assertEquals(
            "72 bpm",
            formatMeasurementValue(Measurement(type = MeasurementType.PULSE, value = 72.0), '.')
        )
        assertEquals(
            "95 mg/dL",
            formatMeasurementValue(Measurement(type = MeasurementType.BLOOD_SUGAR, value = 95.0), '.')
        )
    }

    @Test
    fun olderEntryWithDecimals_keepsThemWithTheLanguagesSeparator() {
        // Saved before whole-number fields stopped accepting decimals.
        val height = Measurement(type = MeasurementType.HEIGHT, value = 175.5)
        assertEquals("175.5 cm", formatMeasurementValue(height, '.'))
        assertEquals("175,5 cm", formatMeasurementValue(height, ','))
    }

    // ---- blood pressure ----

    @Test
    fun bloodPressure_withPulse() {
        val bloodPressure = Measurement(
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
            tertiaryValue = 72.0
        )
        assertEquals("120/80 mmHg · 72 bpm", formatMeasurementValue(bloodPressure, '.'))
    }

    @Test
    fun bloodPressure_withoutPulse_asInOlderEntries() {
        val bloodPressure = Measurement(
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0
        )
        assertEquals("120/80 mmHg", formatMeasurementValue(bloodPressure, '.'))
    }

    @Test
    fun bloodPressure_withoutDiastolic_showsQuestionMark() {
        val bloodPressure = Measurement(type = MeasurementType.BLOOD_PRESSURE, value = 120.0)
        assertEquals("120/? mmHg", formatMeasurementValue(bloodPressure, '.'))
    }

    @Test
    fun bloodPressure_isTheSameInEveryLanguage() {
        val bloodPressure = Measurement(
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
            tertiaryValue = 72.0
        )
        assertEquals(
            formatMeasurementValue(bloodPressure, '.'),
            formatMeasurementValue(bloodPressure, ',')
        )
    }
}
