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
        assertEquals("08:00", todayItems[0].scheduledTime)
        assertEquals(ScheduledDoseStatus.PENDING, todayItems[0].status)
        assertTrue(todayItems[0].isRecurring)
        assertEquals(10L, todayItems[0].reminderId)

        // Future date -> projected as PENDING
        val nextWeek = MedicationScheduleResolver.resolveForDate(
            date = today.plusDays(7),
            medicationsWithReminders = meds,
            doseLogs = emptyList(),
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertEquals(1, nextWeek.size)
        assertEquals(ScheduledDoseStatus.PENDING, nextWeek[0].status)
    }

    @Test
    fun resolveForDate_respectsStartDateEndDateAndDaysOfWeekMask() {
        // Only Mon (1) and Wed (4), active Nov 25..Nov 27
        val reminder = MedicationReminder(
            id = 10L,
            medicationId = 1L,
            hour = 9,
            minute = 30,
            daysOfWeekMask = 1 or 4,
            startDate = "2026-11-25",
            endDate = "2026-11-27"
        )
        val meds = listOf(sampleMed(reminders = listOf(reminder)))

        // Nov 23 (Mon, before startDate) -> empty
        assertTrue(
            MedicationScheduleResolver.resolveForDate(
                date = LocalDate.of(2026, 11, 23),
                medicationsWithReminders = meds,
                doseLogs = emptyList(),
                overrides = emptyList(),
                today = today,
                zoneId = zone
            ).isEmpty()
        )

        // Nov 25 (Wed, inside range, matches mask) -> 1 item
        val nov25 = MedicationScheduleResolver.resolveForDate(
            date = LocalDate.of(2026, 11, 25),
            medicationsWithReminders = meds,
            doseLogs = emptyList(),
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )
        assertEquals(1, nov25.size)
        assertEquals("09:30", nov25[0].scheduledTime)

        // Nov 26 (Thu, inside range, doesn't match mask) -> empty
        assertTrue(
            MedicationScheduleResolver.resolveForDate(
                date = LocalDate.of(2026, 11, 26),
                medicationsWithReminders = meds,
                doseLogs = emptyList(),
                overrides = emptyList(),
                today = today,
                zoneId = zone
            ).isEmpty()
        )

        // Nov 30 (Mon, after endDate) -> empty
        assertTrue(
            MedicationScheduleResolver.resolveForDate(
                date = LocalDate.of(2026, 11, 30),
                medicationsWithReminders = meds,
                doseLogs = emptyList(),
                overrides = emptyList(),
                today = today,
                zoneId = zone
            ).isEmpty()
        )
    }

    @Test
    fun resolveForDate_appliesRescheduledAndCancelledOverrides() {
        val meds = listOf(sampleMed())
        val overrides = listOf(
            MedicationScheduleOverride(
                id = 50L,
                reminderId = 10L,
                medicationId = 1L,
                originalDate = "2026-11-23",
                overrideType = MedicationScheduleOverride.TYPE_RESCHEDULED,
                newHour = 10,
                newMinute = 45
            ),
            MedicationScheduleOverride(
                id = 51L,
                reminderId = 10L,
                medicationId = 1L,
                originalDate = "2026-11-24",
                overrideType = MedicationScheduleOverride.TYPE_CANCELLED
            )
        )
        val logs = listOf(
            MedicationDoseLog(
                id = 100L,
                medicationId = 1L,
                scheduledDate = "2026-11-23",
                scheduledTime = "10:45",
                status = "TAKEN"
            )
        )

        // Nov 23 -> moved to 10:45 and matched as TAKEN
        val nov23 = MedicationScheduleResolver.resolveForDate(
            date = LocalDate.of(2026, 11, 23),
            medicationsWithReminders = meds,
            doseLogs = logs,
            overrides = overrides,
            today = today,
            zoneId = zone
        )
        assertEquals(1, nov23.size)
        assertEquals("10:45", nov23[0].scheduledTime)
        assertEquals("08:00", nov23[0].originalTime)
        assertEquals(50L, nov23[0].overrideId)
        assertEquals(ScheduledDoseStatus.TAKEN, nov23[0].status)
        assertEquals(100L, nov23[0].doseLogId)

        // Nov 24 -> CANCELLED, empty
        val nov24 = MedicationScheduleResolver.resolveForDate(
            date = LocalDate.of(2026, 11, 24),
            medicationsWithReminders = meds,
            doseLogs = emptyList(),
            overrides = overrides,
            today = today,
            zoneId = zone
        )
        assertTrue(nov24.isEmpty())

        // Nov 25 -> unaffected, back to 08:00 PENDING
        val nov25 = MedicationScheduleResolver.resolveForDate(
            date = LocalDate.of(2026, 11, 25),
            medicationsWithReminders = meds,
            doseLogs = emptyList(),
            overrides = overrides,
            today = today,
            zoneId = zone
        )
        assertEquals(1, nov25.size)
        assertEquals("08:00", nov25[0].scheduledTime)
        assertNull(nov25[0].overrideId)
        assertEquals(ScheduledDoseStatus.PENDING, nov25[0].status)
    }

    @Test
    fun resolveForDate_mergesOneTimeAndPausedLogsAndSortsChronologically() {
        val activeMed = sampleMed(id = 1L, name = "Amlodipine", isActive = true)
        val pausedMed = sampleMed(id = 2L, name = "Ibuprofen", isActive = false, reminders = emptyList())

        val logs = listOf(
            MedicationDoseLog(
                id = 200L,
                medicationId = 2L,
                scheduledDate = "2026-11-23",
                scheduledTime = "14:15",
                status = "TAKEN"
            ),
            MedicationDoseLog(
                id = 201L,
                medicationId = 2L,
                scheduledDate = "2026-11-23",
                scheduledTime = "06:30",
                status = "TAKEN"
            )
        )

        val items = MedicationScheduleResolver.resolveForDate(
            date = today,
            medicationsWithReminders = listOf(activeMed, pausedMed),
            doseLogs = logs,
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )

        assertEquals(3, items.size)
        assertEquals(listOf("06:30", "08:00", "14:15"), items.map { it.scheduledTime })
        assertFalse(items[0].isRecurring)
        assertTrue(items[1].isRecurring)
        assertFalse(items[2].isRecurring)
        assertEquals("Ibuprofen", items[0].medicationName)
    }

    @Test
    fun resolveForMonth_populatesEveryDayInMonth() {
        val meds = listOf(sampleMed())
        val monthMap = MedicationScheduleResolver.resolveForMonth(
            yearMonth = YearMonth.of(2026, 11),
            medicationsWithReminders = meds,
            doseLogs = emptyList(),
            overrides = emptyList(),
            today = today,
            zoneId = zone
        )

        assertEquals(30, monthMap.size)
        assertTrue(monthMap[LocalDate.of(2026, 11, 22)].orEmpty().isEmpty())
        assertEquals(1, monthMap[LocalDate.of(2026, 11, 23)].orEmpty().size)
        assertEquals(1, monthMap[LocalDate.of(2026, 11, 30)].orEmpty().size)
    }
}
