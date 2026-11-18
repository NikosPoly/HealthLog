package com.example.healthlog.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.healthlog.database.HealthDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(MedicationNotificationHelper.EXTRA_MEDICATION_ID, -1L)
        if (medicationId == -1L) return

        val medicationName = intent.getStringExtra(MedicationNotificationHelper.EXTRA_MEDICATION_NAME) ?: return
        val dosage = intent.getStringExtra(MedicationNotificationHelper.EXTRA_DOSAGE) ?: ""
        val unit = intent.getStringExtra(MedicationNotificationHelper.EXTRA_UNIT) ?: ""
        val instructions = intent.getStringExtra(MedicationNotificationHelper.EXTRA_INSTRUCTIONS)
        val scheduledTime = intent.getStringExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_TIME) ?: ""
        val notificationId = intent.getIntExtra(MedicationNotificationHelper.EXTRA_NOTIFICATION_ID, medicationId.toInt())

        // Show the high-priority reminder notification
        MedicationNotificationHelper.showMedicationNotification(
            context = context,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            unit = unit,
            instructions = instructions,
            scheduledTime = scheduledTime,
            notificationId = notificationId
        )

        // Reschedule the recurring alarm for its next occurrence in the future
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = HealthDatabase.getDatabase(context)
                val medication = db.medicationDao().getMedicationById(medicationId)
                val reminders = db.medicationDao().getRemindersForMedication(medicationId)
                val matchingReminder = reminders.find { it.id.toInt() == notificationId }

                if (medication != null && matchingReminder != null && medication.isActive && matchingReminder.isEnabled) {
                    MedicationAlarmScheduler.scheduleReminder(context, medication, matchingReminder)
                }
            } catch (_: Exception) {
                // Catch any background scheduling exception
            } finally {
                pendingResult.finish()
            }
        }
    }
}
