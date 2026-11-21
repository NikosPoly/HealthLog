package com.example.healthlog.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationReminder
import java.util.Calendar

object MedicationAlarmScheduler {

    /**
     * Calculates the next epoch millis for a given reminder (hour:minute and days of week).
     */
    fun calculateNextTriggerMillis(
        hour: Int,
        minute: Int,
        daysOfWeekMask: Int,
        nowCalendar: Calendar = Calendar.getInstance()
    ): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = nowCalendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time today is already in the past, advance to tomorrow
        if (target.timeInMillis <= nowCalendar.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Check daysOfWeekMask. Calendar.DAY_OF_WEEK: 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat.
        // Bitmask mapping: 1=Mon(2^0), 2=Tue(2^1), 4=Wed(2^2), 8=Thu(2^3), 16=Fri(2^4), 32=Sat(2^5), 64=Sun(2^6).
        for (dayOffset in 0..7) {
            val dayOfWeek = target.get(Calendar.DAY_OF_WEEK)
            val bit = when (dayOfWeek) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 4
                Calendar.THURSDAY -> 8
                Calendar.FRIDAY -> 16
                Calendar.SATURDAY -> 32
                Calendar.SUNDAY -> 64
                else -> 1
            }

            if ((daysOfWeekMask and bit) != 0) {
                return target.timeInMillis
            }
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }

    fun scheduleReminder(
        context: Context,
        medication: Medication,
        reminder: MedicationReminder
    ) {
        if (!reminder.isEnabled || !medication.isActive) {
            cancelReminder(context, reminder.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val triggerAtMillis = calculateNextTriggerMillis(
            hour = reminder.hour,
            minute = reminder.minute,
            daysOfWeekMask = reminder.daysOfWeekMask
        )

        val scheduledTime = String.format(java.util.Locale.US, "%02d:%02d", reminder.hour, reminder.minute)

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(MedicationNotificationHelper.EXTRA_MEDICATION_ID, medication.id)
            putExtra(MedicationNotificationHelper.EXTRA_MEDICATION_NAME, medication.name)
            putExtra(MedicationNotificationHelper.EXTRA_DOSAGE, medication.dosage)
            putExtra(MedicationNotificationHelper.EXTRA_UNIT, medication.unit)
            putExtra(MedicationNotificationHelper.EXTRA_INSTRUCTIONS, medication.instructions)
            putExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(MedicationNotificationHelper.EXTRA_NOTIFICATION_ID, reminder.id.toInt())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
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
        snoozeMinutes: Int = 15
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
            putExtra(MedicationNotificationHelper.EXTRA_NOTIFICATION_ID, notificationId)
        }

        // Use a unique request code for snooze so it doesn't collide with the recurring reminder
        val requestCode = notificationId + 100000

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    private fun scheduleExactAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
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
