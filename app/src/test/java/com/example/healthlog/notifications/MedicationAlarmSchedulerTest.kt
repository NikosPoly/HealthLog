package com.example.healthlog.notifications

import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MedicationAlarmSchedulerTest {

    @Test
    fun calculateNextTriggerMillis_laterToday_triggersSameDay() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 8, 0, 0) // 08:00 AM Wed (mask = 127 = everyday)
            set(Calendar.MILLISECOND, 0)
        }

        // Reminder at 08:30 AM (30 mins in future)
        val trigger = MedicationAlarmScheduler.calculateNextTriggerMillis(
            hour = 8,
            minute = 30,
            daysOfWeekMask = 127,
            nowCalendar = now
        )

        val expected = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 30)
        }.timeInMillis

        assertEquals(expected, trigger)
    }

    @Test
    fun calculateNextTriggerMillis_alreadyPassedToday_triggersNextDay() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 9, 0, 0) // 09:00 AM Wed
            set(Calendar.MILLISECOND, 0)
        }

        // Reminder at 08:00 AM (already passed today)
        val trigger = MedicationAlarmScheduler.calculateNextTriggerMillis(
            hour = 8,
            minute = 0,
            daysOfWeekMask = 127,
            nowCalendar = now
        )

        val expected = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1) // Tomorrow (Nov 19)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        assertEquals(expected, trigger)
    }

    @Test
    fun calculateNextTriggerMillis_skipsUnselectedDays() {
        // Wednesday Nov 18, 2026
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Only Fridays (bit 16 in bitmask: 1=Mon, 2=Tue, 4=Wed, 8=Thu, 16=Fri)
        val fridayMask = 16

        val trigger = MedicationAlarmScheduler.calculateNextTriggerMillis(
            hour = 8,
            minute = 0,
            daysOfWeekMask = fridayMask,
            nowCalendar = now
        )

        assertNotNull(trigger)
        val resultCalendar = Calendar.getInstance().apply { timeInMillis = trigger!! }
        assertEquals(Calendar.FRIDAY, resultCalendar.get(Calendar.DAY_OF_WEEK))
        assertEquals(8, resultCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCalendar.get(Calendar.MINUTE))
        assertTrue(trigger!! > now.timeInMillis)
    }

    @Test
    fun calculateNextTriggerMillis_emptyMask_returnsNull() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertNull(
            MedicationAlarmScheduler.calculateNextTriggerMillis(
                hour = 8,
                minute = 0,
                daysOfWeekMask = 0,
                nowCalendar = now
            )
        )
    }

    @Test
    fun calculateNextTriggerMillisForReminder_skipsCancelledAndRescheduledDates() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 23, 7, 0, 0) // Mon 07:00
            set(Calendar.MILLISECOND, 0)
        }
        val reminder = MedicationReminder(id = 10L, medicationId = 1L, hour = 8, minute = 0)
        val overrides = listOf(
            MedicationScheduleOverride(
                id = 1L,
                reminderId = 10L,
                medicationId = 1L,
                originalDate = "2026-11-23",
                overrideType = MedicationScheduleOverride.TYPE_RESCHEDULED,
                newHour = 10,
                newMinute = 0
            ),
            MedicationScheduleOverride(
                id = 2L,
                reminderId = 10L,
                medicationId = 1L,
                originalDate = "2026-11-24",
                overrideType = MedicationScheduleOverride.TYPE_CANCELLED
            )
        )

        val trigger = MedicationAlarmScheduler.calculateNextTriggerMillisForReminder(
            reminder = reminder,
            overrides = overrides,
            nowCalendar = now
        )

        assertNotNull(trigger)
        val cal = Calendar.getInstance().apply { timeInMillis = trigger!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun calculateNextTriggerMillisForReminder_honorsFutureStartDateAndEndDate() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 23, 7, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Future start date jumps directly to Nov 27
        val futureReminder = MedicationReminder(
            id = 11L,
            medicationId = 1L,
            hour = 9,
            minute = 15,
            startDate = "2026-11-27"
        )
        val futureTrigger = MedicationAlarmScheduler.calculateNextTriggerMillisForReminder(
            reminder = futureReminder,
            nowCalendar = now
        )
        assertNotNull(futureTrigger)
        val cal = Calendar.getInstance().apply { timeInMillis = futureTrigger!! }
        assertEquals(27, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, cal.get(Calendar.MINUTE))

        // Expired endDate returns null
        val expiredReminder = MedicationReminder(
            id = 12L,
            medicationId = 1L,
            hour = 9,
            minute = 0,
            endDate = "2026-11-22"
        )
        assertNull(
            MedicationAlarmScheduler.calculateNextTriggerMillisForReminder(
                reminder = expiredReminder,
                nowCalendar = now
            )
        )
    }

    @Test
    fun calculateOverrideTriggerMillis_returnsExactMillisForFutureReschedule_nullOtherwise() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 23, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Future rescheduled slot today at 11:30 -> valid trigger
        val futureOverride = MedicationScheduleOverride(
            id = 1L,
            reminderId = 10L,
            medicationId = 1L,
            originalDate = "2026-11-23",
            overrideType = MedicationScheduleOverride.TYPE_RESCHEDULED,
            newHour = 11,
            newMinute = 30
        )
        val trigger = MedicationAlarmScheduler.calculateOverrideTriggerMillis(futureOverride, now)
        assertNotNull(trigger)
        val cal = Calendar.getInstance().apply { timeInMillis = trigger!! }
        assertEquals(23, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(11, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))

        // Already passed slot today at 08:00 -> null
        val pastOverride = futureOverride.copy(newHour = 8, newMinute = 0)
        assertNull(MedicationAlarmScheduler.calculateOverrideTriggerMillis(pastOverride, now))

        // Cancelled override -> null
        val cancelledOverride = futureOverride.copy(
            overrideType = MedicationScheduleOverride.TYPE_CANCELLED,
            newHour = null,
            newMinute = null
        )
        assertNull(MedicationAlarmScheduler.calculateOverrideTriggerMillis(cancelledOverride, now))
    }

    @Test
    fun formatTriggerDate_matchesScheduledDayAcrossMidnightBoundaries() {
        val tz = java.util.TimeZone.getTimeZone("UTC")

        // Bedtime dose at 23:55 on Nov 24
        val bedtimeNow = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 24, 23, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val bedtimeReminder = MedicationReminder(id = 20L, medicationId = 1L, hour = 23, minute = 55)
        val bedtimeTrigger = MedicationAlarmScheduler.calculateNextTriggerMillisForReminder(
            reminder = bedtimeReminder,
            nowCalendar = bedtimeNow
        )
        assertNotNull(bedtimeTrigger)
        assertEquals(
            "2026-11-24",
            MedicationAlarmScheduler.formatTriggerDate(bedtimeTrigger!!, tz)
        )

        // Midnight dose at 00:00 scheduled from 23:50 on Nov 24 -> belongs to Nov 25
        val midnightNow = Calendar.getInstance(tz).apply {
            set(2026, Calendar.NOVEMBER, 24, 23, 50, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightReminder = MedicationReminder(id = 21L, medicationId = 1L, hour = 0, minute = 0)
        val midnightTrigger = MedicationAlarmScheduler.calculateNextTriggerMillisForReminder(
            reminder = midnightReminder,
            nowCalendar = midnightNow
        )
        assertNotNull(midnightTrigger)
        assertEquals(
            "2026-11-25",
            MedicationAlarmScheduler.formatTriggerDate(midnightTrigger!!, tz)
        )
    }
}
