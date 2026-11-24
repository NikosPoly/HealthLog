package com.example.healthlog.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.healthlog.MainActivity
import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import com.example.healthlog.model.MedicationScheduleResolver
import java.time.LocalDate
import java.util.Calendar

object MedicationAlarmScheduler {

    const val SNOOZE_OFFSET = 100_000
    const val OVERRIDE_OFFSET = 200_000

    /**
     * Calculates the next epoch millis for a given reminder (hour:minute and days of week).
     */
    fun calculateNextTriggerMillis(
        hour: Int,
        minute: Int,
        daysOfWeekMask: Int,
        nowCalendar: Calendar = Calendar.getInstance()
    ): Long {
        val target = Calendar.getInstance(nowCalendar.timeZone).apply {
            timeInMillis = nowCalendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time today is already at or before now, advance to tomorrow
        if (target.timeInMillis <= nowCalendar.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Check daysOfWeekMask. Bitmask mapping: 1=Mon, 2=Tue, 4=Wed, 8=Thu, 16=Fri, 32=Sat, 64=Sun.
        for (dayOffset in 0..7) {
            val date = calendarToLocalDate(target)
            if (MedicationScheduleResolver.matchesDayOfWeek(date, daysOfWeekMask)) {
                return target.timeInMillis
            }
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }

    /**
     * Calculates the next recurring trigger millis for [reminder], honoring optional `startDate`,
     * `endDate`, `daysOfWeekMask`, and skipping any dates present in [overrides] (`CANCELLED` or
     * `RESCHEDULED`, since rescheduled dates use their own one-off override alarm).
     *
     * Returns null if no future occurrence exists (e.g., reminder is disabled, mask is 0, or `endDate` passed).
     */
    fun calculateNextTriggerMillisForReminder(
        reminder: MedicationReminder,
        overrides: List<MedicationScheduleOverride> = emptyList(),
        nowCalendar: Calendar = Calendar.getInstance()
    ): Long? {
        if (!reminder.isEnabled || reminder.daysOfWeekMask == 0) return null

        val overridesByDate = overrides
            .filter { it.reminderId == reminder.id }
            .associateBy { it.originalDate }

        val target = Calendar.getInstance(nowCalendar.timeZone).apply {
            timeInMillis = nowCalendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.timeInMillis <= nowCalendar.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val startDate = MedicationScheduleResolver.parseIsoDateOrNull(reminder.startDate)
        if (startDate != null && calendarToLocalDate(target).isBefore(startDate)) {
            target.set(Calendar.YEAR, startDate.year)
            target.set(Calendar.MONTH, startDate.monthValue - 1)
            target.set(Calendar.DAY_OF_MONTH, startDate.dayOfMonth)
            target.set(Calendar.HOUR_OF_DAY, reminder.hour)
            target.set(Calendar.MINUTE, reminder.minute)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)
            if (target.timeInMillis <= nowCalendar.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val endDate = MedicationScheduleResolver.parseIsoDateOrNull(reminder.endDate)

        // Search up to 366 days ahead to skip cancelled/rescheduled dates and weekday gaps.
        for (dayOffset in 0..366) {
            val candidateDate = calendarToLocalDate(target)
            if (endDate != null && candidateDate.isAfter(endDate)) {
                return null
            }

            val isOverridden = overridesByDate.containsKey(candidateDate.toString())
            if (!isOverridden && MedicationScheduleResolver.matchesDayOfWeek(candidateDate, reminder.daysOfWeekMask)) {
                return target.timeInMillis
            }

            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return null
    }

    /**
     * Calculates the exact one-off trigger millis for a `RESCHEDULED` single-day override.
     * Returns null if the override is not `RESCHEDULED`, has invalid date/time, or is already in the past.
     */
    fun calculateOverrideTriggerMillis(
        override: MedicationScheduleOverride,
        nowCalendar: Calendar = Calendar.getInstance()
    ): Long? {
        if (override.overrideType != MedicationScheduleOverride.TYPE_RESCHEDULED) return null
        val hour = override.newHour ?: return null
        val minute = override.newMinute ?: return null
        val date = MedicationScheduleResolver.parseIsoDateOrNull(override.originalDate) ?: return null

        val target = Calendar.getInstance(nowCalendar.timeZone).apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return if (target.timeInMillis > nowCalendar.timeInMillis) {
            target.timeInMillis
        } else {
            null
        }
    }

    fun scheduleReminder(
        context: Context,
        medication: Medication,
        reminder: MedicationReminder,
        overrides: List<MedicationScheduleOverride> = emptyList(),
        nowCalendar: Calendar = Calendar.getInstance()
    ) {
        if (!reminder.isEnabled || !medication.isActive) {
            cancelReminder(context, reminder.id, overrides)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val triggerAtMillis = calculateNextTriggerMillisForReminder(
            reminder = reminder,
            overrides = overrides,
            nowCalendar = nowCalendar
        )

        if (triggerAtMillis == null) {
            // No future recurring occurrence remains (e.g. final endDate dose already fired),
            // so clear any pending main alarm without dismissing an active tray notification or snooze.
            cancelMainAlarmOnly(context, alarmManager, reminder.id.toInt())
        } else {
            val scheduledTime = MedicationScheduleResolver.formatTime(reminder.hour, reminder.minute)

            val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                putExtra(MedicationNotificationHelper.EXTRA_MEDICATION_ID, medication.id)
                putExtra(MedicationNotificationHelper.EXTRA_MEDICATION_NAME, medication.name)
                putExtra(MedicationNotificationHelper.EXTRA_DOSAGE, medication.dosage)
                putExtra(MedicationNotificationHelper.EXTRA_UNIT, medication.unit)
                putExtra(MedicationNotificationHelper.EXTRA_INSTRUCTIONS, medication.instructions)
                putExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
                putExtra(MedicationNotificationHelper.EXTRA_NOTIFICATION_ID, reminder.id.toInt())
                putExtra(MedicationNotificationHelper.EXTRA_REMINDER_ID, reminder.id)
                putExtra(MedicationNotificationHelper.EXTRA_ALARM_TYPE, MedicationNotificationHelper.ALARM_TYPE_MAIN)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleReliableAlarm(context, alarmManager, triggerAtMillis, pendingIntent)
        }

        // Sync any per-day overrides belonging to this reminder
        overrides
            .filter { it.reminderId == reminder.id }
            .forEach { override ->
                if (override.overrideType == MedicationScheduleOverride.TYPE_RESCHEDULED) {
                    scheduleOverrideAlarm(context, medication, override, nowCalendar)
                } else {
                    cancelOverrideAlarm(context, override.id)
                }
            }
    }

    fun scheduleOverrideAlarm(
        context: Context,
        medication: Medication,
        override: MedicationScheduleOverride,
        nowCalendar: Calendar = Calendar.getInstance()
    ) {
        if (!medication.isActive || override.overrideType != MedicationScheduleOverride.TYPE_RESCHEDULED) {
            cancelOverrideAlarm(context, override.id)
            return
        }

        val triggerAtMillis = calculateOverrideTriggerMillis(override, nowCalendar)
        if (triggerAtMillis == null) {
            // Override time has already passed; keep any active notification or snooze intact.
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            cancelMainAlarmOnly(context, alarmManager, overrideRequestCode(override.id))
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = overrideRequestCode(override.id)
        val scheduledTime = MedicationScheduleResolver.formatTime(override.newHour!!, override.newMinute!!)

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(MedicationNotificationHelper.EXTRA_MEDICATION_ID, medication.id)
            putExtra(MedicationNotificationHelper.EXTRA_MEDICATION_NAME, medication.name)
            putExtra(MedicationNotificationHelper.EXTRA_DOSAGE, medication.dosage)
            putExtra(MedicationNotificationHelper.EXTRA_UNIT, medication.unit)
            putExtra(MedicationNotificationHelper.EXTRA_INSTRUCTIONS, medication.instructions)
            putExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_DATE, override.originalDate)
            putExtra(MedicationNotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(MedicationNotificationHelper.EXTRA_REMINDER_ID, override.reminderId)
            putExtra(MedicationNotificationHelper.EXTRA_OVERRIDE_ID, override.id)
            putExtra(MedicationNotificationHelper.EXTRA_ALARM_TYPE, MedicationNotificationHelper.ALARM_TYPE_OVERRIDE)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleReliableAlarm(context, alarmManager, triggerAtMillis, pendingIntent)
    }

    fun cancelOverrideAlarm(context: Context, overrideId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelMainAndSnoozeAlarms(context, alarmManager, overrideRequestCode(overrideId))
    }

    fun cancelReminder(
        context: Context,
        reminderId: Long,
        overrides: List<MedicationScheduleOverride> = emptyList()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelMainAndSnoozeAlarms(context, alarmManager, reminderId.toInt())

        overrides
            .filter { it.reminderId == reminderId }
            .forEach { override ->
                cancelOverrideAlarm(context, override.id)
            }
    }

    private fun cancelMainAlarmOnly(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int
    ) {
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (mainPendingIntent != null) {
            alarmManager.cancel(mainPendingIntent)
            mainPendingIntent.cancel()
        }
    }

    private fun cancelMainAndSnoozeAlarms(
        context: Context,
        alarmManager: AlarmManager,
        notificationId: Int
    ) {
        cancelMainAlarmOnly(context, alarmManager, notificationId)

        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        val snoozeRequestCode = notificationId + SNOOZE_OFFSET
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePendingIntent != null) {
            alarmManager.cancel(snoozePendingIntent)
            snoozePendingIntent.cancel()
        }

        MedicationNotificationHelper.cancelNotification(context, notificationId)
    }

    fun scheduleSnooze(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        unit: String,
        instructions: String?,
        scheduledTime: String,
        notificationId: Int,
        reminderId: Long = notificationId.toLong(),
        overrideId: Long = -1L,
        scheduledDate: String? = null,
        snoozeMinutes: Int = 30
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(MedicationNotificationHelper.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationNotificationHelper.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(MedicationNotificationHelper.EXTRA_DOSAGE, dosage)
            putExtra(MedicationNotificationHelper.EXTRA_UNIT, unit)
            putExtra(MedicationNotificationHelper.EXTRA_INSTRUCTIONS, instructions)
            putExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
            if (scheduledDate != null) {
                putExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_DATE, scheduledDate)
            }
            putExtra(MedicationNotificationHelper.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(MedicationNotificationHelper.EXTRA_REMINDER_ID, reminderId)
            if (overrideId != -1L) {
                putExtra(MedicationNotificationHelper.EXTRA_OVERRIDE_ID, overrideId)
            }
            putExtra(MedicationNotificationHelper.EXTRA_ALARM_TYPE, MedicationNotificationHelper.ALARM_TYPE_SNOOZE)
        }

        val requestCode = notificationId + SNOOZE_OFFSET

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleReliableAlarm(context, alarmManager, triggerAtMillis, pendingIntent)
    }

    private fun overrideRequestCode(overrideId: Long): Int = overrideId.toInt() + OVERRIDE_OFFSET

    private fun calendarToLocalDate(calendar: Calendar): LocalDate {
        return LocalDate.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun scheduleReliableAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)

        try {
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }
}
