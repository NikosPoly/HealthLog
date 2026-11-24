package com.example.healthlog.model

import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import com.example.healthlog.database.MedicationWithReminders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MedicationScheduleResolverTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 23) // Monday

    private fun epochFor(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun sampleMed(
        id: Long = 1L,
        name: String = "Amlodipine",
        isActive: Boolean = true,
        createdDate: LocalDate = LocalDate.of(2026, 11, 1),
        reminders: List<MedicationReminder> = listOf(
            MedicationReminder(id = 10L, medicationId = id, hour = 8, minute = 0)
        )
    ) = MedicationWithReminders(
        medication = Medication(
            id = id,
            name = name,
            dosage = "5",
            unit = "mg",
            frequency = "DAILY",
            isActive = isActive,
            createdAt = epochFor(createdDate)
        ),
        reminders = reminders
    )

    @Test
    fun resolveForDate_projectsRecurringOnTodayAndFuture_notPast() {
        val meds = listOf(sampleMed())

        // Past date with no logs -> empty (does not invent retroactive pending doses)
        val yesterday = MedicationScheduleResolver.resolveForDate(
            date = today.minusDays(1),
            medicationsWithReminders = meds,
            doseLogs = emptyList(),
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertTrue(yesterday.isEmpty())

        // Today -> projected as PENDING
        val todayItems = MedicationScheduleResolver.resolveForDate(
            date = today,
            medicationsWithReminders = meds,
            doseLogs = emptyList(),
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertEquals(1, todayItems.size)
        val item = todayItems.first()
        assertEquals("Amlodipine", item.medicationName)
        assertEquals("08:00", item.scheduledTime)
        assertEquals(ScheduledDoseStatus.PENDING, item.status)
        assertTrue(item.isRecurring)
        assertNull(item.doseLogId)

        // Future date -> projected as PENDING
        val tomorrowItems = MedicationScheduleResolver.resolveForDate(
            date = today.plusDays(1),
            medicationsWithReminders = meds,
            doseLogs = emptyList(),
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertEquals(1, tomorrowItems.size)
        assertEquals(ScheduledDoseStatus.PENDING, tomorrowItems.first().status)
    }

    @Test
    fun resolveForDate_matchesDoseLogsAcrossPastAndToday() {
        val meds = listOf(sampleMed())
        val pastDate = today.minusDays(2)
        val logs = listOf(
            MedicationDoseLog(
                id = 100L,
                medicationId = 1L,
                scheduledDate = pastDate.toString(),
                scheduledTime = "08:00",
                status = "TAKEN"
            ),
            MedicationDoseLog(
                id = 101L,
                medicationId = 1L,
                scheduledDate = today.toString(),
                scheduledTime = "08:00",
                status = "SKIPPED"
            )
        )

        val pastItems = MedicationScheduleResolver.resolveForDate(
            date = pastDate,
            medicationsWithReminders = meds,
            doseLogs = logs,
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertEquals(1, pastItems.size)
        assertEquals(ScheduledDoseStatus.TAKEN, pastItems.first().status)
        assertEquals(100L, pastItems.first().doseLogId)

        val todayItems = MedicationScheduleResolver.resolveForDate(
            date = today,
            medicationsWithReminders = meds,
            doseLogs = logs,
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertEquals(1, todayItems.size)
        assertEquals(ScheduledDoseStatus.SKIPPED, todayItems.first().status)
        assertEquals(101L, todayItems.first().doseLogId)
    }

    @Test
    fun resolveForDate_appliesRescheduledAndCancelledOverrides() {
        val meds = listOf(sampleMed())
        val overrides = listOf(
            MedicationScheduleOverride(
                id = 200L,
                reminderId = 10L,
                medicationId = 1L,
                originalDate = today.toString(),
                overrideType = "RESCHEDULED",
                newHour = 10,
                newMinute = 30
            )
        )

        val todayItems = MedicationScheduleResolver.resolveForDate(
            date = today,
            medicationsWithReminders = meds,
            doseLogs = emptyList(),
            overrides = overrides,
            today = today,
            zoneId = zone
        )
        assertEquals(1, todayItems.size)
        val item = todayItems.first()
        assertEquals("10:30", item.scheduledTime)
        assertEquals("08:00", item.originalTime)
        assertEquals(200L, item.overrideId)

        // Cancelled override suppresses the dose completely
        val cancelledOverrides = listOf(
            MedicationScheduleOverride(
                id = 201L,
                reminderId = 10L,
                medicationId = 1L,
                originalDate = today.toString(),
                overrideType = "CANCELLED"
            )
        )
        val cancelledItems = MedicationScheduleResolver.resolveForDate(
            date = today,
            medicationsWithReminders = meds,
            doseLogs = emptyList(),
            overrides = cancelledOverrides,
            today = today,
            zoneId = zone
        )
        assertTrue(cancelledItems.isEmpty())
    }

    @Test
    fun resolveForDate_handlesOneTimeAndAsNeededLogsCorrectly() {
        val oneTimeMed = MedicationWithReminders(
            medication = Medication(
                id = 2L,
                name = "Ibuprofen",
                dosage = "400",
                unit = "mg",
                frequency = "ONE_TIME",
                isActive = false,
                createdAt = epochFor(today)
            ),
            reminders = emptyList()
        )
        val logs = listOf(
            MedicationDoseLog(
                id = 300L,
                medicationId = 2L,
                scheduledDate = today.toString(),
                scheduledTime = "14:15",
                status = "TAKEN"
            )
        )

        val items = MedicationScheduleResolver.resolveForDate(
            date = today,
            medicationsWithReminders = listOf(oneTimeMed),
            doseLogs = logs,
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertEquals(1, items.size)
        val item = items.first()
        assertEquals("Ibuprofen", item.medicationName)
        assertEquals("14:15", item.scheduledTime)
        assertFalse(item.isRecurring)
        assertEquals(300L, item.doseLogId)
    }

    @Test
    fun resolveForMonth_aggregatesDoseCountsAccurately() {
        val meds = listOf(sampleMed())
        val logs = listOf(
            MedicationDoseLog(
                id = 1L,
                medicationId = 1L,
                scheduledDate = today.toString(),
                scheduledTime = "08:00",
                status = "TAKEN"
            )
        )

        val monthSummaries = MedicationScheduleResolver.resolveForMonth(
            yearMonth = YearMonth.of(2026, 11),
            medicationsWithReminders = meds,
            doseLogs = logs,
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )

        val todayDoses = monthSummaries[today].orEmpty()
        assertEquals(1, todayDoses.size)
        assertEquals(ScheduledDoseStatus.TAKEN, todayDoses.single().status)

        // Tomorrow is pending
        val tomorrowDoses = monthSummaries[today.plusDays(1)].orEmpty()
        assertEquals(1, tomorrowDoses.size)
        assertEquals(ScheduledDoseStatus.PENDING, tomorrowDoses.single().status)
    }

    @Test
    fun resolveForDate_respectsReminderStartAndEndDates() {
        val med = sampleMed(
            reminders = listOf(
                MedicationReminder(
                    id = 10L,
                    medicationId = 1L,
                    hour = 8,
                    minute = 0,
                    startDate = today.plusDays(2).toString(),
                    endDate = today.plusDays(5).toString()
                )
            )
        )

        // Before start date -> not projected
        val todayItems = MedicationScheduleResolver.resolveForDate(
            date = today,
            medicationsWithReminders = listOf(med),
            doseLogs = emptyList(),
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertTrue(todayItems.isEmpty())

        // In range -> projected
        val inRangeItems = MedicationScheduleResolver.resolveForDate(
            date = today.plusDays(3),
            medicationsWithReminders = listOf(med),
            doseLogs = emptyList(),
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertEquals(1, inRangeItems.size)

        // After end date -> not projected
        val afterEndItems = MedicationScheduleResolver.resolveForDate(
            date = today.plusDays(6),
            medicationsWithReminders = listOf(med),
            doseLogs = emptyList(),
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertTrue(afterEndItems.isEmpty())
    }

    @Test
    fun resolveForDate_softDeletedMedication_keepsHistoricalLogsAndNeverProjectsFutureReminders() {
        val deletedMed = MedicationWithReminders(
            medication = Medication(
                id = 9L,
                name = "Amoxicillin",
                dosage = "500",
                unit = "mg",
                frequency = "DAILY",
                isActive = false,
                isDeleted = true
            ),
            // Even if a stale reminder somehow existed, isDeleted must never project it
            reminders = listOf(
                MedicationReminder(
                    id = 90L,
                    medicationId = 9L,
                    hour = 8,
                    minute = 0,
                    daysOfWeekMask = 127,
                    isEnabled = true,
                    startDate = "2026-11-01"
                )
            )
        )
        val logs = listOf(
            MedicationDoseLog(
                id = 500L,
                medicationId = 9L,
                scheduledDate = "2026-11-23",
                scheduledTime = "08:00",
                status = "TAKEN"
            )
        )

        // Today: preserves TAKEN log with real name/dosage
        val todayItems = MedicationScheduleResolver.resolveForDate(
            date = today,
            medicationsWithReminders = listOf(deletedMed),
            doseLogs = logs,
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertEquals(1, todayItems.size)
        assertEquals("Amoxicillin", todayItems.single().medicationName)
        assertEquals("500", todayItems.single().dosage)
        assertEquals(ScheduledDoseStatus.TAKEN, todayItems.single().status)

        // Tomorrow: zero projected pending reminders
        val tomorrowItems = MedicationScheduleResolver.resolveForDate(
            date = today.plusDays(1),
            medicationsWithReminders = listOf(deletedMed),
            doseLogs = emptyList(),
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertTrue(tomorrowItems.isEmpty())
    }

    @Test
    fun resolveForMonth_softDeletedMedication_countsOnlyLoggedDosesAndNeverProjectsFuturePending() {
        val deletedMed = MedicationWithReminders(
            medication = Medication(
                id = 9L,
                name = "Amoxicillin",
                dosage = "500",
                unit = "mg",
                frequency = "DAILY",
                isActive = false,
                isDeleted = true
            ),
            reminders = listOf(
                MedicationReminder(
                    id = 90L,
                    medicationId = 9L,
                    hour = 8,
                    minute = 0,
                    daysOfWeekMask = 127,
                    isEnabled = true,
                    startDate = "2026-11-01"
                )
            )
        )
        val logs = listOf(
            MedicationDoseLog(
                id = 500L,
                medicationId = 9L,
                scheduledDate = "2026-11-10",
                scheduledTime = "08:00",
                status = "TAKEN"
            )
        )

        val monthSummaries = MedicationScheduleResolver.resolveForMonth(
            yearMonth = YearMonth.of(2026, 11),
            medicationsWithReminders = listOf(deletedMed),
            doseLogs = logs,
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )

        // Nov 10 has the 1 taken dose
        val nov10Doses = monthSummaries[LocalDate.of(2026, 11, 10)].orEmpty()
        assertEquals(1, nov10Doses.size)
        assertEquals(ScheduledDoseStatus.TAKEN, nov10Doses.single().status)

        // Future dates (e.g. today or tomorrow) have 0 count
        val nov23Doses = monthSummaries[today].orEmpty()
        assertTrue(nov23Doses.isEmpty())
        val nov24Doses = monthSummaries[today.plusDays(1)].orEmpty()
        assertTrue(nov24Doses.isEmpty())
    }
}
