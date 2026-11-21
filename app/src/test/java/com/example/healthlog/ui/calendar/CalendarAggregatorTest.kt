package com.example.healthlog.ui.calendar

import com.example.healthlog.database.Measurement
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.SymptomSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarAggregatorTest {

    private val fixedZone = ZoneId.of("UTC")
    private val testMonth = YearMonth.of(2026, 11)

    @Test
    fun monthIsoDateRange_computesFirstAndLastDay() {
        val (start, end) = CalendarAggregator.monthIsoDateRange(testMonth)
        assertEquals("2026-11-01", start)
        assertEquals("2026-11-30", end)
    }

    @Test
    fun monthTimeRange_computesEpochMillisecondBounds() {
        val (startMillis, endMillis) = CalendarAggregator.monthTimeRange(testMonth, fixedZone)
        val expectedStart = LocalDate.of(2026, 11, 1).atStartOfDay(fixedZone).toInstant().toEpochMilli()
        val expectedEnd = LocalDate.of(2026, 12, 1).atStartOfDay(fixedZone).toInstant().toEpochMilli()

        assertEquals(expectedStart, startMillis)
        assertEquals(expectedEnd, endMillis)
    }

    @Test
    fun aggregateMonth_groupsDayEventsAndResolvesWorstClinicalStage() {
        val nov5 = LocalDate.of(2026, 11, 5)
        val nov5Millis = nov5.atTime(9, 30).atZone(fixedZone).toInstant().toEpochMilli()

        val nov10 = LocalDate.of(2026, 11, 10)
        val nov10Millis = nov10.atTime(14, 0).atZone(fixedZone).toInstant().toEpochMilli()

        // Nov 5: normal blood pressure (118/78) + high blood pressure (165/102 -> Grade 2)
        val measurements = listOf(
            Measurement(
                id = 1L,
                type = MeasurementType.BLOOD_PRESSURE,
                value = 118.0,
                secondaryValue = 78.0,
                timestamp = nov5Millis - 3600_000
            ),
            Measurement(
                id = 2L,
                type = MeasurementType.BLOOD_PRESSURE,
                value = 165.0,
                secondaryValue = 102.0,
                timestamp = nov5Millis
            )
        )

        // Nov 5: doses taken
        val doses = listOf(
            MedicationDoseLog(
                id = 10L,
                medicationId = 1L,
                scheduledDate = "2026-11-05",
                scheduledTime = "08:00",
                status = "TAKEN"
            )
        )

        // Nov 5 & Nov 10: symptoms
        val symptoms = listOf(
            SymptomEntry(
                id = 100L,
                symptomType = "HEADACHE",
                severity = 2,
                timestamp = nov5Millis
            ),
            SymptomEntry(
                id = 101L,
                symptomType = "DIZZINESS",
                severity = 4,
                timestamp = nov5Millis + 1800_000
            ),
            SymptomEntry(
                id = 102L,
                symptomType = "FATIGUE",
                severity = 1,
                timestamp = nov10Millis
            )
        )

        val summaries = CalendarAggregator.aggregateMonth(
            yearMonth = testMonth,
            measurements = measurements,
            doseLogs = doses,
            symptoms = symptoms,
            zoneId = fixedZone
        )

        assertEquals(30, summaries.size)

        // Verify Nov 5 summary
        val day5 = summaries[nov5]
        assertNotNull(day5)
        assertEquals(2, day5!!.measurements.size)
        assertEquals(1, day5.doseLogs.size)
        assertEquals(2, day5.symptoms.size)

        // Grade 2 should be the worst vital rank (ALERT_RED), while grid dots use fixed category colors
        assertEquals(VitalClinicalRank.ALERT_RED, day5.highestVitalRank)
        assertEquals(SymptomSeverity.SEVERE, day5.maxSymptomSeverity)
        assertTrue(day5.vitalDotColor == CalendarVitalDotColor)
        assertTrue(day5.symptomDotColor == CalendarSymptomDotColor)
        assertTrue(day5.medicationDotColor == CalendarMedicationDotColor)

        // Verify Nov 10 summary
        val day10 = summaries[nov10]
        assertNotNull(day10)
        assertTrue(day10!!.measurements.isEmpty())
        assertTrue(day10.doseLogs.isEmpty())
        assertEquals(1, day10.symptoms.size)
        assertNull(day10.highestVitalRank)
        assertEquals(SymptomSeverity.VERY_MILD, day10.maxSymptomSeverity)
        assertNull(day10.vitalDotColor)
        assertTrue(day10.symptomDotColor == CalendarSymptomDotColor)
        assertNull(day10.medicationDotColor)

        // Verify empty day (Nov 1)
        val day1 = summaries[LocalDate.of(2026, 11, 1)]
        assertNotNull(day1)
        assertFalse(day1!!.hasAnyData)
        assertNull(day1.vitalDotColor)
        assertNull(day1.symptomDotColor)
        assertNull(day1.medicationDotColor)
    }

    @Test
    fun aggregateMonth_filtersCompanionPulseFromBloodPressureButKeepsStandalonePulse() {
        val nov18 = LocalDate.of(2026, 11, 18)
        val bpTimestamp = nov18.atTime(10, 0).atZone(fixedZone).toInstant().toEpochMilli()
        val standalonePulseTimestamp = nov18.atTime(15, 30).atZone(fixedZone).toInstant().toEpochMilli()

        val measurements = listOf(
            // Blood pressure with pulse 74 bpm
            Measurement(
                id = 1L,
                type = MeasurementType.BLOOD_PRESSURE,
                value = 132.0,
                secondaryValue = 84.0,
                tertiaryValue = 74.0,
                timestamp = bpTimestamp
            ),
            // Companion pulse automatically saved at the exact same timestamp and value
            Measurement(
                id = 2L,
                type = MeasurementType.PULSE,
                value = 74.0,
                timestamp = bpTimestamp
            ),
            // Independent standalone pulse logged later that day
            Measurement(
                id = 3L,
                type = MeasurementType.PULSE,
                value = 68.0,
                timestamp = standalonePulseTimestamp
            )
        )

        val summaries = CalendarAggregator.aggregateMonth(
            yearMonth = testMonth,
            measurements = measurements,
            doseLogs = emptyList(),
            symptoms = emptyList(),
            zoneId = fixedZone
        )

        val day18 = summaries[nov18]
        assertNotNull(day18)
        // Companion pulse (id=2) must be excluded; BP (id=1) and standalone pulse (id=3) remain
        assertEquals(listOf(1L, 3L), day18!!.measurements.map { it.id })
        // Vital dot color always uses the calendar legend green color
        assertTrue(day18.vitalDotColor == CalendarVitalDotColor)
    }

    @Test
    fun parseIsoDateOrNull_handlesValidAndInvalidInputs() {
        assertEquals(LocalDate.of(2026, 11, 21), CalendarAggregator.parseIsoDateOrNull("2026-11-21"))
        assertNull(CalendarAggregator.parseIsoDateOrNull("21/11/2026"))
        assertNull(CalendarAggregator.parseIsoDateOrNull(""))
        assertNull(CalendarAggregator.parseIsoDateOrNull("invalid"))
    }

    @Test
    fun filterCompanionPulses_keepsPulseWhenBloodPressureHasNoPulseOrDifferentValue() {
        val ts = LocalDate.of(2026, 11, 18).atTime(9, 0).atZone(fixedZone).toInstant().toEpochMilli()
        val measurements = listOf(
            // BP without pulse recorded
            Measurement(
                id = 1L,
                type = MeasurementType.BLOOD_PRESSURE,
                value = 120.0,
                secondaryValue = 80.0,
                tertiaryValue = null,
                timestamp = ts
            ),
            // Separate pulse logged at same time
            Measurement(
                id = 2L,
                type = MeasurementType.PULSE,
                value = 72.0,
                timestamp = ts
            )
        )

        val filtered = CalendarAggregator.filterCompanionPulses(measurements)
        assertEquals(listOf(1L, 2L), filtered.map { it.id })
    }

    @Test
    fun calculateHighestVitalRank_evaluatesAcrossAllMeasurementTypes() {
        assertNull(calculateHighestVitalRank(emptyList()))

        // Normal pulse + Overweight BMI -> ALERT_ORANGE
        val mixed = listOf(
            Measurement(id = 1L, type = MeasurementType.PULSE, value = 72.0),
            Measurement(id = 2L, type = MeasurementType.WEIGHT, value = 85.0) // BMI ~27.8 at 175cm
        )
        assertEquals(VitalClinicalRank.ALERT_ORANGE, calculateHighestVitalRank(mixed, latestHeightCm = 175.0))

        // Hypertensive crisis beats everything
        val withCrisis = mixed + Measurement(
            id = 3L,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 190.0,
            secondaryValue = 125.0
        )
        assertEquals(VitalClinicalRank.CRISIS, calculateHighestVitalRank(withCrisis, latestHeightCm = 175.0))
    }
}
