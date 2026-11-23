package com.example.healthlog.ui.medications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationWithReminders
import com.example.healthlog.model.MedicationScheduleResolver
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.notifications.MedicationAlarmReceiver
import com.example.healthlog.notifications.MedicationAlarmScheduler
import com.example.healthlog.notifications.MedicationNotificationHelper
import com.example.healthlog.repository.MedicationRepository
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/**
 * Shared medication persistence and alarm-sync actions used across Medications, Calendar,
 * and Day Timeline ViewModels so creating, editing, and logging doses behave identically
 * everywhere in the app.
 */
object MedicationActions {

    /**
     * Saves or updates a medication (recurring `DAILY` with alarms, `AS_NEEDED`, or alarm-free
     * `ONE_TIME`) and synchronizes OS alarms cleanly.
     *
     * Returns the medication name if an immediate/slot dose was logged (for toast feedback),
     * or null otherwise.
     */
    suspend fun saveMedication(
        repository: MedicationRepository,
        appContext: Context,
        existing: MedicationWithReminders?,
        name: String,
        dosage: String,
        unit: String,
        frequency: String,
        instructions: String?,
        reminderTimes: List<Pair<Int, Int>>,
        saveToPrescriptions: Boolean = true,
        logDoseNow: Boolean = false,
        targetDate: LocalDate = LocalDate.now(),
        oneTimeHourMinute: Pair<Int, Int>? = null
    ): String? {
        val effectiveFrequency = if (frequency == "DAILY") {
            "DAILY"
        } else if (saveToPrescriptions) {
            "AS_NEEDED"
        } else {
            "ONE_TIME"
        }

        val medToSave = existing?.medication?.copy(
            name = name.trim(),
            dosage = dosage.trim(),
            unit = unit,
            frequency = effectiveFrequency,
            instructions = instructions?.trim()?.ifBlank { null }
        ) ?: Medication(
            name = name.trim(),
            dosage = dosage.trim(),
            unit = unit,
            frequency = effectiveFrequency,
            instructions = instructions?.trim()?.ifBlank { null },
            isActive = true
        )

        val defaultStartDate = existing?.reminders?.firstOrNull()?.startDate
            ?: targetDate.toString()

        val remindersToSave = if (effectiveFrequency == "DAILY") {
            reminderTimes.map { (hour, minute) ->
                MedicationReminder(
                    medicationId = medToSave.id,
                    hour = hour,
                    minute = minute,
                    daysOfWeekMask = 127,
                    isEnabled = true,
                    startDate = defaultStartDate
                )
            }
        } else {
            emptyList()
        }

        val savedId = if (existing == null) {
            repository.saveMedication(medToSave, remindersToSave)
        } else {
            val oldReminders = repository.getRemindersForMedication(medToSave.id)
            oldReminders.forEach { reminder ->
                val overrides = repository.getOverridesForReminder(reminder.id)
                MedicationAlarmScheduler.cancelReminder(appContext, reminder.id, overrides)
            }
            existing.reminders.forEach { reminder ->
                MedicationAlarmScheduler.cancelReminder(appContext, reminder.id)
            }

            repository.updateMedication(medToSave, remindersToSave)
            medToSave.id
        }

        val updatedMed = repository.getMedicationById(savedId)
        if (updatedMed != null && updatedMed.isActive && effectiveFrequency == "DAILY") {
            val actualReminders = repository.getRemindersForMedication(savedId)
            actualReminders.forEach { reminder ->
                val overrides = repository.getOverridesForReminder(reminder.id)
                MedicationAlarmScheduler.scheduleReminder(appContext, updatedMed, reminder, overrides)
            }
        }

        // One-time intake (no alarm) or as-needed med with immediate/slot log
        if (effectiveFrequency == "ONE_TIME" || (effectiveFrequency == "AS_NEEDED" && logDoseNow)) {
            val logTime = if (oneTimeHourMinute != null) {
                MedicationScheduleResolver.formatTime(oneTimeHourMinute.first, oneTimeHourMinute.second)
            } else {
                SimpleDateFormat("HH:mm", Locale.US).format(Date())
            }
            repository.recordAsNeededDose(
                medicationId = savedId,
                scheduledDate = targetDate.toString(),
                scheduledTime = logTime,
                status = "TAKEN"
            )
            return medToSave.name
        }

        return null
    }

    /**
     * Marks a scheduled dose as `TAKEN` or `SKIPPED`, dismissing any active tray notification
     * and cancelling any pending snooze alarm for that dose (both main and override slots).
     */
    suspend fun recordScheduledDoseStatus(
        repository: MedicationRepository,
        appContext: Context?,
        doseItem: ScheduledDoseItem,
        status: String
    ) {
        repository.recordDose(
            medicationId = doseItem.medicationId,
            scheduledDate = doseItem.scheduledDate,
            scheduledTime = doseItem.scheduledTime,
            status = status
        )
        if (appContext != null) {
            dismissNotificationAndSnooze(appContext, doseItem)
        }
    }

    suspend fun undoScheduledDose(
        repository: MedicationRepository,
        doseItem: ScheduledDoseItem
    ) {
        if (doseItem.doseLogId != null) {
            repository.deleteDoseById(doseItem.doseLogId)
        } else {
            repository.deleteDose(
                medicationId = doseItem.medicationId,
                scheduledDate = doseItem.scheduledDate,
                scheduledTime = doseItem.scheduledTime
            )
        }
    }

    enum class RecurringDoseScope {
        THIS_DAY_ONLY,
        THIS_AND_FUTURE
    }

    data class PendingDoseMove(
        val dose: ScheduledDoseItem,
        val newHour: Int,
        val newMinute: Int
    )

    /**
     * Moves a scheduled or logged dose to [newHour]:[newMinute] and keeps OS alarms in sync.
     *
     * - For recurring doses with [RecurringDoseScope.THIS_DAY_ONLY]: creates/updates a single-day
     *   `RESCHEDULED` override (or clears it if moved back to the base time) and reschedules alarms.
     * - For recurring doses with [RecurringDoseScope.THIS_AND_FUTURE]: updates the reminder in place
     *   (on today/startDate) or splits it from `scheduledDate` onward, clearing future overrides
     *   and rescheduling alarms for both the capped and split reminders.
     * - For one-time/logged-only doses: moves the dose log's `scheduledTime`.
     */
    suspend fun moveScheduledDose(
        repository: MedicationRepository,
        appContext: Context?,
        doseItem: ScheduledDoseItem,
        newHour: Int,
        newMinute: Int,
        scope: RecurringDoseScope,
        today: LocalDate = LocalDate.now()
    ) {
        val newTime = MedicationScheduleResolver.formatTime(newHour, newMinute)

        if (doseItem.isRecurring && doseItem.reminderId != null) {
            val reminderId = doseItem.reminderId
            val oldOverrides = repository.getOverridesForReminder(reminderId)

            when (scope) {
                RecurringDoseScope.THIS_DAY_ONLY -> {
                    if (appContext != null) {
                        oldOverrides
                            .filter { it.originalDate == doseItem.scheduledDate }
                            .forEach { MedicationAlarmScheduler.cancelOverrideAlarm(appContext, it.id) }
                    }

                    repository.rescheduleDoseForDate(
                        reminderId = reminderId,
                        date = doseItem.scheduledDate,
                        newHour = newHour,
                        newMinute = newMinute
                    )

                    if (doseItem.doseLogId != null) {
                        repository.deleteDoseById(doseItem.doseLogId)
                        repository.recordDose(
                            medicationId = doseItem.medicationId,
                            scheduledDate = doseItem.scheduledDate,
                            scheduledTime = newTime,
                            status = doseItem.status.name
                        )
                    }

                    if (appContext != null) {
                        val med = repository.getMedicationById(doseItem.medicationId)
                        val reminder = repository.getReminderById(reminderId)
                        if (med != null && reminder != null) {
                            val updatedOverrides = repository.getOverridesForReminder(reminderId)
                            MedicationAlarmScheduler.scheduleReminder(appContext, med, reminder, updatedOverrides)
                        }
                    }
                }

                RecurringDoseScope.THIS_AND_FUTURE -> {
                    val fromDate = MedicationScheduleResolver.parseIsoDateOrNull(doseItem.scheduledDate) ?: today
                    if (appContext != null) {
                        oldOverrides
                            .filter { it.originalDate >= doseItem.scheduledDate }
                            .forEach { MedicationAlarmScheduler.cancelOverrideAlarm(appContext, it.id) }
                    }

                    val (updatedOriginal, splitNew) = repository.rescheduleReminderFromDate(
                        reminderId = reminderId,
                        fromDate = fromDate,
                        newHour = newHour,
                        newMinute = newMinute,
                        today = today
                    )

                    if (doseItem.doseLogId != null) {
                        repository.deleteDoseById(doseItem.doseLogId)
                        repository.recordDose(
                            medicationId = doseItem.medicationId,
                            scheduledDate = doseItem.scheduledDate,
                            scheduledTime = newTime,
                            status = doseItem.status.name
                        )
                    }

                    if (appContext != null) {
                        val med = repository.getMedicationById(doseItem.medicationId)
                        if (med != null) {
                            if (updatedOriginal != null) {
                                val remainingOverrides = repository.getOverridesForReminder(updatedOriginal.id)
                                MedicationAlarmScheduler.scheduleReminder(appContext, med, updatedOriginal, remainingOverrides)
                            }
                            if (splitNew != null) {
                                MedicationAlarmScheduler.scheduleReminder(appContext, med, splitNew, emptyList())
                            }
                        }
                    }
                }
            }
        } else if (doseItem.doseLogId != null) {
            repository.deleteDoseById(doseItem.doseLogId)
            repository.recordAsNeededDose(
                medicationId = doseItem.medicationId,
                scheduledDate = doseItem.scheduledDate,
                scheduledTime = newTime,
                status = doseItem.status.name
            )
        }
    }

    /**
     * Deletes/cancels a scheduled or logged dose and keeps OS alarms in sync.
     *
     * - For recurring doses with [RecurringDoseScope.THIS_DAY_ONLY]: removes any dose log for that
     *   day, records a `CANCELLED` override, dismisses active notifications/snooze, and reschedules.
     * - For recurring doses with [RecurringDoseScope.THIS_AND_FUTURE]: deletes or caps the reminder
     *   from `scheduledDate` onward (preserving past dose history) and cancels/reschedules alarms.
     * - For one-time/logged-only doses: removes the single `MedicationDoseLog`.
     */
    suspend fun deleteScheduledDose(
        repository: MedicationRepository,
        appContext: Context?,
        doseItem: ScheduledDoseItem,
        scope: RecurringDoseScope,
        today: LocalDate = LocalDate.now()
    ) {
        if (doseItem.isRecurring && doseItem.reminderId != null) {
            val reminderId = doseItem.reminderId
            val oldOverrides = repository.getOverridesForReminder(reminderId)

            if (appContext != null) {
                dismissNotificationAndSnooze(appContext, doseItem)
            }

            when (scope) {
                RecurringDoseScope.THIS_DAY_ONLY -> {
                    if (appContext != null) {
                        oldOverrides
                            .filter { it.originalDate == doseItem.scheduledDate }
                            .forEach { MedicationAlarmScheduler.cancelOverrideAlarm(appContext, it.id) }
                    }
                    if (doseItem.doseLogId != null) {
                        repository.deleteDoseById(doseItem.doseLogId)
                    }
                    repository.cancelDoseForDate(reminderId, doseItem.scheduledDate)

                    if (appContext != null) {
                        val med = repository.getMedicationById(doseItem.medicationId)
                        val reminder = repository.getReminderById(reminderId)
                        if (med != null && reminder != null) {
                            val updatedOverrides = repository.getOverridesForReminder(reminderId)
                            MedicationAlarmScheduler.scheduleReminder(appContext, med, reminder, updatedOverrides)
                        }
                    }
                }

                RecurringDoseScope.THIS_AND_FUTURE -> {
                    val fromDate = MedicationScheduleResolver.parseIsoDateOrNull(doseItem.scheduledDate) ?: today
                    val capped = repository.deleteReminderFromDate(
                        reminderId = reminderId,
                        fromDate = fromDate,
                        today = today
                    )

                    if (appContext != null) {
                        if (capped == null) {
                            MedicationAlarmScheduler.cancelReminder(appContext, reminderId, oldOverrides)
                        } else {
                            oldOverrides
                                .filter { it.originalDate >= doseItem.scheduledDate }
                                .forEach { MedicationAlarmScheduler.cancelOverrideAlarm(appContext, it.id) }
                            val med = repository.getMedicationById(doseItem.medicationId)
                            if (med != null) {
                                val remainingOverrides = repository.getOverridesForReminder(capped.id)
                                MedicationAlarmScheduler.scheduleReminder(appContext, med, capped, remainingOverrides)
                            }
                        }
                    }
                }
            }
        } else if (doseItem.doseLogId != null) {
            repository.deleteDoseById(doseItem.doseLogId)
        }
    }

    fun dismissNotificationAndSnooze(
        appContext: Context,
        doseItem: ScheduledDoseItem
    ) {
        val notificationId = when {
            doseItem.overrideId != null -> doseItem.overrideId.toInt() + MedicationAlarmScheduler.OVERRIDE_OFFSET
            doseItem.reminderId != null -> doseItem.reminderId.toInt()
            else -> return
        }

        MedicationNotificationHelper.cancelNotification(appContext, notificationId)

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeIntent = Intent(appContext, MedicationAlarmReceiver::class.java)
        val snoozePendingIntent = PendingIntent.getBroadcast(
            appContext,
            notificationId + MedicationAlarmScheduler.SNOOZE_OFFSET,
            snoozeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePendingIntent != null) {
            alarmManager.cancel(snoozePendingIntent)
            snoozePendingIntent.cancel()
        }
    }
}
