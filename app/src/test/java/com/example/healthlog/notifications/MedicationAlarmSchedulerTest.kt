package com.example.healthlog.notifications

import org.junit.Assert.assertEquals
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

        val resultCalendar = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(Calendar.FRIDAY, resultCalendar.get(Calendar.DAY_OF_WEEK))
        assertEquals(8, resultCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCalendar.get(Calendar.MINUTE))
        assertTrue(trigger > now.timeInMillis)
    }
}
