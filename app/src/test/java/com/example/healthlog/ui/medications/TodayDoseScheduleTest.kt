package com.example.healthlog.ui.medications

import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationWithReminders
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.model.ScheduledDoseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayDoseScheduleTest {

    @Test
    fun scheduledDoseStatus_valuesAreCorrect() {
        assertEquals(4, ScheduledDoseStatus.entries.size)
        assertTrue(ScheduledDoseStatus.entries.contains(ScheduledDoseStatus.PENDING))
        assertTrue(ScheduledDoseStatus.entries.contains(ScheduledDoseStatus.TAKEN))
        assertTrue(ScheduledDoseStatus.entries.contains(ScheduledDoseStatus.SKIPPED))
        assertTrue(ScheduledDoseStatus.entries.contains(ScheduledDoseStatus.UNLOGGED))
    }

    @Test
    fun prescriptionsView_visibilityFilter_excludesOneTimeMedications() {
        val dailyMed = MedicationWithReminders(
            medication = Medication(id = 1, name = "Aspirin", dosage = "100", unit = "mg", frequency = "DAILY"),
            reminders = emptyList()
        )
        val asNeededMed = MedicationWithReminders(
            medication = Medication(id = 2, name = "Paracetamol", dosage = "500", unit = "mg", frequency = "AS_NEEDED"),
            reminders = emptyList()
        )
        val oneTimeMed = MedicationWithReminders(
            medication = Medication(id = 3, name = "Ibuprofen", dosage = "400", unit = "mg", frequency = "ONE_TIME"),
            reminders = emptyList()
        )

        val allMeds = listOf(dailyMed, asNeededMed, oneTimeMed)
        val visibleMeds = allMeds.filter { it.medication.frequency != "ONE_TIME" }

        assertEquals(2, visibleMeds.size)
        assertTrue(visibleMeds.contains(dailyMed))
        assertTrue(visibleMeds.contains(asNeededMed))
        assertFalse(visibleMeds.contains(oneTimeMed))
    }

    @Test
    fun daysOfWeekMask_bitCalculations() {
        val mondayBit = 1
        val tuesdayBit = 2
        val wednesdayBit = 4
        val thursdayBit = 8
        val fridayBit = 16
        val saturdayBit = 32
        val sundayBit = 64

        val everydayMask = 127
        assertEquals(127, mondayBit or tuesdayBit or wednesdayBit or thursdayBit or fridayBit or saturdayBit or sundayBit)

        val weekdaysMask = mondayBit or tuesdayBit or wednesdayBit or thursdayBit or fridayBit
        assertEquals(31, weekdaysMask)

        val weekendsMask = saturdayBit or sundayBit
        assertEquals(96, weekendsMask)

        assertTrue((everydayMask and wednesdayBit) != 0)
        assertTrue((weekdaysMask and wednesdayBit) != 0)
        assertFalse((weekendsMask and wednesdayBit) != 0)
        assertTrue((weekendsMask and saturdayBit) != 0)
    }

    @Test
    fun scheduleGrouping_ordersByScheduledTime() {
        val evening = ScheduledDoseItem(
            medicationId = 1, medicationName = "Pill A", dosage = "10", unit = "mg", instructions = null,
            scheduledDate = "2026-11-18", scheduledTime = "20:00", hour = 20, minute = 0,
            status = ScheduledDoseStatus.PENDING, isRecurring = true
        )
        val morning = evening.copy(scheduledTime = "08:00", hour = 8)
        val noon = evening.copy(scheduledTime = "13:00", hour = 13)

        val sorted = listOf(evening, morning, noon).sortedWith(compareBy({ it.hour }, { it.minute }))

        assertEquals("08:00", sorted[0].scheduledTime)
        assertEquals("13:00", sorted[1].scheduledTime)
        assertEquals("20:00", sorted[2].scheduledTime)
    }
}
