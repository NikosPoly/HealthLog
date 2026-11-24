package com.example.healthlog.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.healthlog.database.HealthDatabase
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import com.example.healthlog.model.MedicationScheduleResolver
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
        val overrideId = intent.getLongExtra(MedicationNotificationHelper.EXTRA_OVERRIDE_ID, -1L)
        val alarmType = intent.getStringExtra(MedicationNotificationHelper.EXTRA_ALARM_TYPE)
            ?: MedicationNotificationHelper.ALARM_TYPE_MAIN

        val medicationName = intent.getStringExtra(MedicationNotificationHelper.EXTRA_MEDICATION_NAME) ?: return
        val dosage = intent.getStringExtra(MedicationNotificationHelper.EXTRA_DOSAGE) ?: ""
        val unit = intent.getStringExtra(MedicationNotificationHelper.EXTRA_UNIT) ?: ""
        val instructions = intent.getStringExtra(MedicationNotificationHelper.EXTRA_INSTRUCTIONS)
        val scheduledTime = intent.getStringExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_TIME) ?: ""
        val scheduledDateExtra = intent.getStringExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_DATE)
        val notificationId = intent.getIntExtra(
            MedicationNotificationHelper.EXTRA_NOTIFICATION_ID,
            if (reminderId != -1L) reminderId.toInt() else medicationId.toInt()
        )

        val pendingResult = goAsync()

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
                    if (alarmType == MedicationNotificationHelper.ALARM_TYPE_OVERRIDE && overrideId != -1L) {
                        MedicationAlarmScheduler.cancelOverrideAlarm(context, overrideId)
                    } else if (reminderId != -1L) {
                        MedicationAlarmScheduler.cancelReminder(context, reminderId)
                    }
                    return@launch
                }

                val db = HealthDatabase.getDatabase(context)
                val dao = db.medicationDao()
                val medication = dao.getMedicationById(medicationId)

                // 1. Guard check: if medication was deleted or paused, cancel and do NOT ring!
                if (medication == null || !medication.isActive || medication.isDeleted) {
                    if (alarmType == MedicationNotificationHelper.ALARM_TYPE_OVERRIDE && overrideId != -1L) {
                        MedicationAlarmScheduler.cancelOverrideAlarm(context, overrideId)
                    } else if (reminderId != -1L) {
                        val overrides = dao.getOverridesForReminder(reminderId)
                        MedicationAlarmScheduler.cancelReminder(context, reminderId, overrides)
                    }
                    return@launch
                }

                // 2. Reminder check: if reminder was deleted or disabled, cancel and do NOT ring!
                val reminders = dao.getRemindersForMedication(medicationId)
                val matchingReminder = if (reminderId != -1L) {
                    reminders.find { it.id == reminderId }
                } else {
                    reminders.find { it.id.toInt() == notificationId }
                }

                if (matchingReminder == null || !matchingReminder.isEnabled) {
                    if (alarmType == MedicationNotificationHelper.ALARM_TYPE_OVERRIDE && overrideId != -1L) {
                        MedicationAlarmScheduler.cancelOverrideAlarm(context, overrideId)
                    } else if (reminderId != -1L) {
                        MedicationAlarmScheduler.cancelReminder(context, reminderId)
                    }
                    return@launch
                }

                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val effectiveDate = scheduledDateExtra ?: todayDate
                val overrides = dao.getOverridesForReminder(matchingReminder.id)

                // 3. Validate override/schedule state for MAIN, OVERRIDE, and SNOOZE alarms
                val shouldRingForSchedule = shouldRingForSchedule(
                    alarmType = alarmType,
                    matchingReminder = matchingReminder,
                    overrides = overrides,
                    overrideId = overrideId,
                    effectiveDate = effectiveDate,
                    scheduledTime = scheduledTime
                )

                // 4. Dose log check: if this slot was already marked TAKEN or SKIPPED, do NOT ring!
                val existingLog = dao.getDoseLog(medicationId, effectiveDate, scheduledTime)
                val isAlreadyLogged = existingLog != null &&
                    (existingLog.status == "TAKEN" || existingLog.status == "SKIPPED")

                if (shouldRingForSchedule && !isAlreadyLogged) {
                    MedicationNotificationHelper.showMedicationNotification(
                        context = context,
                        medicationId = medicationId,
                        medicationName = medicationName,
                        dosage = dosage,
                        unit = unit,
                        instructions = instructions,
                        scheduledTime = scheduledTime,
                        notificationId = notificationId,
                        scheduledDate = effectiveDate,
                        reminderId = reminderId,
                        overrideId = overrideId
                    )
                }

                // 5. Reschedule next recurring occurrence only for MAIN alarms (never for snoozes or one-off overrides)
                if (alarmType == MedicationNotificationHelper.ALARM_TYPE_MAIN) {
                    val forwardCalendar = Calendar.getInstance().apply {
                        add(Calendar.SECOND, 60)
                    }
                    MedicationAlarmScheduler.scheduleReminder(
                        context = context,
                        medication = medication,
                        reminder = matchingReminder,
                        overrides = overrides,
                        nowCalendar = forwardCalendar
                    )
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

    companion object {
        internal fun shouldRingForSchedule(
            alarmType: String,
            matchingReminder: MedicationReminder,
            overrides: List<MedicationScheduleOverride>,
            overrideId: Long,
            effectiveDate: String,
            scheduledTime: String
        ): Boolean {
            return when (alarmType) {
                MedicationNotificationHelper.ALARM_TYPE_OVERRIDE -> {
                    val override = if (overrideId != -1L) {
                        overrides.find { it.id == overrideId }
                    } else {
                        null
                    }
                    override != null &&
                        override.overrideType == MedicationScheduleOverride.TYPE_RESCHEDULED &&
                        override.originalDate == effectiveDate &&
                        override.newHour != null &&
                        override.newMinute != null &&
                        MedicationScheduleResolver.formatTime(override.newHour, override.newMinute) == scheduledTime
                }

                MedicationNotificationHelper.ALARM_TYPE_MAIN -> {
                    // Do not ring on dates overridden (CANCELLED or RESCHEDULED to another time)
                    overrides.none { it.originalDate == effectiveDate }
                }

                MedicationNotificationHelper.ALARM_TYPE_SNOOZE -> {
                    if (overrideId != -1L) {
                        val override = overrides.find { it.id == overrideId }
                        override != null &&
                            override.overrideType == MedicationScheduleOverride.TYPE_RESCHEDULED &&
                            override.originalDate == effectiveDate &&
                            override.newHour != null &&
                            override.newMinute != null &&
                            MedicationScheduleResolver.formatTime(override.newHour, override.newMinute) == scheduledTime
                    } else {
                        overrides.none { it.originalDate == effectiveDate } &&
                            MedicationScheduleResolver.formatTime(
                                matchingReminder.hour,
                                matchingReminder.minute
                            ) == scheduledTime
                    }
                }

                else -> false
            }
        }
    }
}
