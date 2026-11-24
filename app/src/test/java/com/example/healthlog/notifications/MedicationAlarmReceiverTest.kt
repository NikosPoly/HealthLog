package com.example.healthlog.notifications

import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationAlarmReceiverTest {

    private val baseReminder = MedicationReminder(
        id = 10L,
        medicationId = 1L,
        hour = 8,
        minute = 0
    )

    @Test
    fun mainSnooze_ringsWhenScheduleUnchanged() {
        assertTrue(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_SNOOZE,
                matchingReminder = baseReminder,
                overrides = emptyList(),
                overrideId = -1L,
                effectiveDate = "2026-11-24",
                scheduledTime = "08:00"
            )
        )
    }

    @Test
    fun mainSnooze_doesNotRingWhenRescheduledForThisDayOnly() {
        val override = MedicationScheduleOverride(
            id = 50L,
            reminderId = 10L,
            medicationId = 1L,
            originalDate = "2026-11-24",
            overrideType = MedicationScheduleOverride.TYPE_RESCHEDULED,
            newHour = 10,
            newMinute = 0
        )

        assertFalse(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_SNOOZE,
                matchingReminder = baseReminder,
                overrides = listOf(override),
                overrideId = -1L,
                effectiveDate = "2026-11-24",
                scheduledTime = "08:00"
            )
        )
    }

    @Test
    fun mainSnooze_doesNotRingWhenCancelledForThisDayOnly() {
        val override = MedicationScheduleOverride(
            id = 51L,
            reminderId = 10L,
            medicationId = 1L,
            originalDate = "2026-11-24",
            overrideType = MedicationScheduleOverride.TYPE_CANCELLED
        )

        assertFalse(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_SNOOZE,
                matchingReminder = baseReminder,
                overrides = listOf(override),
                overrideId = -1L,
                effectiveDate = "2026-11-24",
                scheduledTime = "08:00"
            )
        )
    }

    @Test
    fun mainSnooze_doesNotRingWhenReminderRescheduledInPlace() {
        val updatedReminder = baseReminder.copy(hour = 10, minute = 0)

        assertFalse(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_SNOOZE,
                matchingReminder = updatedReminder,
                overrides = emptyList(),
                overrideId = -1L,
                effectiveDate = "2026-11-24",
                scheduledTime = "08:00"
            )
        )
    }

    @Test
    fun mainSnooze_ringsWhenOverrideIsForDifferentDate() {
        val tomorrowOverride = MedicationScheduleOverride(
            id = 52L,
            reminderId = 10L,
            medicationId = 1L,
            originalDate = "2026-11-25",
            overrideType = MedicationScheduleOverride.TYPE_RESCHEDULED,
            newHour = 10,
            newMinute = 0
        )

        assertTrue(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_SNOOZE,
                matchingReminder = baseReminder,
                overrides = listOf(tomorrowOverride),
                overrideId = -1L,
                effectiveDate = "2026-11-24",
                scheduledTime = "08:00"
            )
        )
    }

    @Test
    fun overrideSnooze_ringsOnlyWhileMatchingRescheduledOverrideExists() {
        val override = MedicationScheduleOverride(
            id = 60L,
            reminderId = 10L,
            medicationId = 1L,
            originalDate = "2026-11-24",
            overrideType = MedicationScheduleOverride.TYPE_RESCHEDULED,
            newHour = 10,
            newMinute = 0
        )

        // Still valid
        assertTrue(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_SNOOZE,
                matchingReminder = baseReminder,
                overrides = listOf(override),
                overrideId = 60L,
                effectiveDate = "2026-11-24",
                scheduledTime = "10:00"
            )
        )

        // Override rescheduled to another time
        assertFalse(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_SNOOZE,
                matchingReminder = baseReminder,
                overrides = listOf(override.copy(newHour = 12, newMinute = 0)),
                overrideId = 60L,
                effectiveDate = "2026-11-24",
                scheduledTime = "10:00"
            )
        )

        // Override cancelled
        assertFalse(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_SNOOZE,
                matchingReminder = baseReminder,
                overrides = listOf(
                    override.copy(
                        overrideType = MedicationScheduleOverride.TYPE_CANCELLED,
                        newHour = null,
                        newMinute = null
                    )
                ),
                overrideId = 60L,
                effectiveDate = "2026-11-24",
                scheduledTime = "10:00"
            )
        )

        // Override deleted / cleared back to base time
        assertFalse(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_SNOOZE,
                matchingReminder = baseReminder,
                overrides = emptyList(),
                overrideId = 60L,
                effectiveDate = "2026-11-24",
                scheduledTime = "10:00"
            )
        )
    }

    @Test
    fun mainAndOverrideAlarms_validateAgainstOverrides() {
        val override = MedicationScheduleOverride(
            id = 70L,
            reminderId = 10L,
            medicationId = 1L,
            originalDate = "2026-11-24",
            overrideType = MedicationScheduleOverride.TYPE_RESCHEDULED,
            newHour = 10,
            newMinute = 30
        )

        // MAIN alarm skipped on overridden date
        assertFalse(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_MAIN,
                matchingReminder = baseReminder,
                overrides = listOf(override),
                overrideId = -1L,
                effectiveDate = "2026-11-24",
                scheduledTime = "08:00"
            )
        )

        // OVERRIDE alarm rings when matching
        assertTrue(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_OVERRIDE,
                matchingReminder = baseReminder,
                overrides = listOf(override),
                overrideId = 70L,
                effectiveDate = "2026-11-24",
                scheduledTime = "10:30"
            )
        )

        // OVERRIDE alarm skipped if time changed
        assertFalse(
            MedicationAlarmReceiver.shouldRingForSchedule(
                alarmType = MedicationNotificationHelper.ALARM_TYPE_OVERRIDE,
                matchingReminder = baseReminder,
                overrides = listOf(override.copy(newHour = 11, newMinute = 0)),
                overrideId = 70L,
                effectiveDate = "2026-11-24",
                scheduledTime = "10:30"
            )
        )
    }
}
