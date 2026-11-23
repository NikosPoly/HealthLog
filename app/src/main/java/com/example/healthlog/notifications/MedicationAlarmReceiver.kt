package com.example.healthlog.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.healthlog.database.HealthDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(MedicationNotificationHelper.EXTRA_MEDICATION_ID, -1L)
        if (medicationId == -1L) return

        val reminderId = intent.getLongExtra(MedicationNotificationHelper.EXTRA_REMINDER_ID, -1L)
        val alarmType = intent.getStringExtra(MedicationNotificationHelper.EXTRA_ALARM_TYPE)
            ?: MedicationNotificationHelper.ALARM_TYPE_MAIN

        val medicationName = intent.getStringExtra(MedicationNotificationHelper.EXTRA_MEDICATION_NAME) ?: return
        val dosage = intent.getStringExtra(MedicationNotificationHelper.EXTRA_DOSAGE) ?: ""
        val unit = intent.getStringExtra(MedicationNotificationHelper.EXTRA_UNIT) ?: ""
        val instructions = intent.getStringExtra(MedicationNotificationHelper.EXTRA_INSTRUCTIONS)
        val scheduledTime = intent.getStringExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_TIME) ?: ""
        val notificationId = intent.getIntExtra(
            MedicationNotificationHelper.EXTRA_NOTIFICATION_ID,
            if (reminderId != -1L) reminderId.toInt() else medicationId.toInt()
        )

        val pendingResult = goAsync()

        // Acquire a partial wake lock for up to 10 seconds to ensure Room queries and alarm
        // rescheduling complete before the CPU goes back to sleep.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HealthLog:MedicationAlarmReceiver"
        )?.apply {
            setReferenceCounted(false)
            acquire(10_000L)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 0. Permission check: if notification permission was revoked, cancel and do NOT ring!
                if (!MedicationNotificationHelper.areNotificationsEnabled(context)) {
                    if (reminderId != -1L) {
                        MedicationAlarmScheduler.cancelReminder(context, reminderId)
                    }
                    return@launch
                }

                val db = HealthDatabase.getDatabase(context)
                val medication = db.medicationDao().getMedicationById(medicationId)

                // 1. Guard check: if medication was deleted or paused, cancel and do NOT ring!
                if (medication == null || !medication.isActive) {
                    if (reminderId != -1L) {
                        MedicationAlarmScheduler.cancelReminder(context, reminderId)
                    }
                    return@launch
                }

                // 2. Reminder check: if reminder was deleted or disabled, cancel and do NOT ring!
                val reminders = db.medicationDao().getRemindersForMedication(medicationId)
                val matchingReminder = if (reminderId != -1L) {
                    reminders.find { it.id == reminderId }
                } else {
                    reminders.find { it.id.toInt() == notificationId }
                }

                if (matchingReminder == null || !matchingReminder.isEnabled) {
                    if (reminderId != -1L) {
                        MedicationAlarmScheduler.cancelReminder(context, reminderId)
                    }
                    return@launch
                }

                // 3. Dose log check: if today's dose was already marked TAKEN or SKIPPED in the app,
                // do NOT ring for this scheduled dose!
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val existingLog = db.medicationDao().getDoseLog(medicationId, todayDate, scheduledTime)
                val isAlreadyLogged = existingLog != null && (existingLog.status == "TAKEN" || existingLog.status == "SKIPPED")

                if (!isAlreadyLogged) {
                    // Show the notification only if pending
                    MedicationNotificationHelper.showMedicationNotification(
                        context = context,
                        medicationId = medicationId,
                        medicationName = medicationName,
                        dosage = dosage,
                        unit = unit,
                        instructions = instructions,
                        scheduledTime = scheduledTime,
                        notificationId = notificationId,
                        scheduledDate = todayDate
                    )
                }

                // 4. Reschedule recurring alarm for next occurrence only for MAIN alarms (not snoozes)
                if (alarmType == MedicationNotificationHelper.ALARM_TYPE_MAIN) {
                    // Add a 60-second forward buffer to guarantee we advance past current minute
                    // even if the alarm was delivered a fraction of a second early by the system.
                    val forwardCalendar = Calendar.getInstance().apply {
                        add(Calendar.SECOND, 60)
                    }
                    val nextTrigger = MedicationAlarmScheduler.calculateNextTriggerMillis(
                        hour = matchingReminder.hour,
                        minute = matchingReminder.minute,
                        daysOfWeekMask = matchingReminder.daysOfWeekMask,
                        nowCalendar = forwardCalendar
                    )
                    val nextScheduledTime = String.format(
                        Locale.US,
                        "%02d:%02d",
                        matchingReminder.hour,
                        matchingReminder.minute
                    )

                    val nextIntent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                        putExtra(MedicationNotificationHelper.EXTRA_MEDICATION_ID, medication.id)
                        putExtra(MedicationNotificationHelper.EXTRA_MEDICATION_NAME, medication.name)
                        putExtra(MedicationNotificationHelper.EXTRA_DOSAGE, medication.dosage)
                        putExtra(MedicationNotificationHelper.EXTRA_UNIT, medication.unit)
                        putExtra(MedicationNotificationHelper.EXTRA_INSTRUCTIONS, medication.instructions)
                        putExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_TIME, nextScheduledTime)
                        putExtra(MedicationNotificationHelper.EXTRA_NOTIFICATION_ID, matchingReminder.id.toInt())
                        putExtra(MedicationNotificationHelper.EXTRA_REMINDER_ID, matchingReminder.id)
                        putExtra(MedicationNotificationHelper.EXTRA_ALARM_TYPE, MedicationNotificationHelper.ALARM_TYPE_MAIN)
                    }

                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        matchingReminder.id.toInt(),
                        nextIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    val showIntent = Intent(context, com.example.healthlog.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val showPendingIntent = android.app.PendingIntent.getActivity(
                        context,
                        0,
                        showIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    val clockInfo = android.app.AlarmManager.AlarmClockInfo(nextTrigger, showPendingIntent)
                    try {
                        alarmManager.setAlarmClock(clockInfo, pendingIntent)
                    } catch (_: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            nextTrigger,
                            pendingIntent
                        )
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {
                }
                pendingResult.finish()
            }
        }
    }
}
