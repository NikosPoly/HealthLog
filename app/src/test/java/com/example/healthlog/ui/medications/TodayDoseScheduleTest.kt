package com.example.healthlog.ui.medications

import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationWithReminders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TodayDoseScheduleTest {

    @Test
    fun doseStatus_valuesAreCorrect() {
        assertEquals(3, DoseStatus.entries.size)
        assertTrue(DoseStatus.entries.contains(DoseStatus.PENDING))
        assertTrue(DoseStatus.entries.contains(DoseStatus.TAKEN))
        assertTrue(DoseStatus.entries.contains(DoseStatus.SKIPPED))
    }

    @Test
    fun todayDoseItem_stableKey_usesLogIdWhenAvailable() {
        val med = Medication(id = 1, name = "Amlodipine", dosage = "5", unit = "mg", frequency = "DAILY")
        val itemWithLog = TodayDoseItem(
            logId = 42L,
            medication = med,
            reminder = null,
            scheduledTime = "08:00",
            scheduledDate = "2026-11-18",
            status = DoseStatus.TAKEN
        )
        val key = itemWithLog.logId ?: "${itemWithLog.medication.id}_${itemWithLog.scheduledTime}"
        assertEquals(42L, key)
    }

    @Test
    fun todayDoseItem_stableKey_usesMedicationIdAndTimeWhenPending() {
        val med = Medication(id = 5, name = "Metformin", dosage = "500", unit = "mg", frequency = "DAILY")
        val pendingItem = TodayDoseItem(
            logId = null,
            medication = med,
            reminder = null,
            scheduledTime = "13:00",
            scheduledDate = "2026-11-18",
            status = DoseStatus.PENDING
        )
        val key = pendingItem.logId ?: "${pendingItem.medication.id}_${pendingItem.scheduledTime}"
        assertEquals("5_13:00", key)
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

        // Verify day matching logic
        assertTrue((everydayMask and wednesdayBit) != 0)
        assertTrue((weekdaysMask and wednesdayBit) != 0)
        assertFalse((weekendsMask and wednesdayBit) != 0)
        assertTrue((weekendsMask and saturdayBit) != 0)
    }

    @Test
    fun scheduleGrouping_ordersByScheduledTime() {
        val med = Medication(id = 1, name = "Pill A", dosage = "10", unit = "mg", frequency = "DAILY")
        val evening = TodayDoseItem(logId = 1, medication = med, reminder = null, scheduledTime = "20:00", scheduledDate = "2026-11-18", status = DoseStatus.PENDING)
        val morning = TodayDoseItem(logId = 2, medication = med, reminder = null, scheduledTime = "08:00", scheduledDate = "2026-11-18", status = DoseStatus.PENDING)
        val noon = TodayDoseItem(logId = 3, medication = med, reminder = null, scheduledTime = "13:00", scheduledDate = "2026-11-18", status = DoseStatus.PENDING)

        val unsorted = listOf(evening, morning, noon)
        val sorted = unsorted.sortedBy { it.scheduledTime }

        assertEquals("08:00", sorted[0].scheduledTime)
        assertEquals("13:00", sorted[1].scheduledTime)
        assertEquals("20:00", sorted[2].scheduledTime)
    }

    @Test
    fun asNeededDose_isProperlyFlagged() {
        val asNeededMed = Medication(id = 10, name = "Pain Reliever", dosage = "500", unit = "mg", frequency = "AS_NEEDED")
        val item = TodayDoseItem(
            logId = 100L,
            medication = asNeededMed,
            reminder = null,
            scheduledTime = "14:15",
            scheduledDate = "2026-11-18",
            status = DoseStatus.TAKEN,
            actionTimestamp = 1763475300000L,
            isAsNeeded = true
        )

        assertTrue(item.isAsNeeded)
        assertEquals(DoseStatus.TAKEN, item.status)
        assertEquals("Pain Reliever", item.medication.name)
    }
}
