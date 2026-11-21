package com.example.healthlog.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.healthlog.R
import com.example.healthlog.database.HealthDatabase
import com.example.healthlog.repository.MedicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(MedicationNotificationHelper.EXTRA_MEDICATION_ID, -1L)
        val notificationId = intent.getIntExtra(MedicationNotificationHelper.EXTRA_NOTIFICATION_ID, -1)

        if (notificationId != -1) {
            MedicationNotificationHelper.cancelNotification(context, notificationId)
        }

        if (medicationId == -1L) return

        when (intent.action) {
            MedicationNotificationHelper.ACTION_MARK_TAKEN -> {
                val medicationName = intent.getStringExtra(MedicationNotificationHelper.EXTRA_MEDICATION_NAME) ?: ""
                val scheduledTime = intent.getStringExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_TIME) ?: ""

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = HealthDatabase.getDatabase(context)
                        val repository = MedicationRepository(db.medicationDao())
                        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        repository.recordDose(
                            medicationId = medicationId,
                            scheduledDate = todayDate,
                            scheduledTime = scheduledTime,
                            status = "TAKEN"
                        )

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context.applicationContext,
                                context.getString(R.string.notification_dose_taken_toast, medicationName),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (_: Exception) {
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            MedicationNotificationHelper.ACTION_SNOOZE -> {
                val medicationName = intent.getStringExtra(MedicationNotificationHelper.EXTRA_MEDICATION_NAME) ?: ""
                val dosage = intent.getStringExtra(MedicationNotificationHelper.EXTRA_DOSAGE) ?: ""
                val unit = intent.getStringExtra(MedicationNotificationHelper.EXTRA_UNIT) ?: ""
                val instructions = intent.getStringExtra(MedicationNotificationHelper.EXTRA_INSTRUCTIONS)
                val scheduledTime = intent.getStringExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_TIME) ?: ""

                MedicationAlarmScheduler.scheduleSnooze(
                    context = context,
                    medicationId = medicationId,
                    medicationName = medicationName,
                    dosage = dosage,
                    unit = unit,
                    instructions = instructions,
                    scheduledTime = scheduledTime,
                    notificationId = notificationId
                )
            }
        }
    }
}
