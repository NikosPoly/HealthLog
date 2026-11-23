package com.example.healthlog.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
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
        val reminderId = intent.getLongExtra(
            MedicationNotificationHelper.EXTRA_REMINDER_ID,
            if (notificationId != -1) notificationId.toLong() else -1L
        )

        // Dismiss the active tray notification
        if (notificationId != -1) {
            MedicationNotificationHelper.cancelNotification(context, notificationId)
        }

        if (medicationId == -1L) return

        when (intent.action) {
            MedicationNotificationHelper.ACTION_MARK_TAKEN -> {
                val medicationName = intent.getStringExtra(MedicationNotificationHelper.EXTRA_MEDICATION_NAME) ?: ""
                val scheduledTime = intent.getStringExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_TIME) ?: ""
                val scheduledDate = intent.getStringExtra(MedicationNotificationHelper.EXTRA_SCHEDULED_DATE)
                    ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                // Cancel any pending snooze for this dose since it is now taken
                if (reminderId != -1L) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    val snoozeIntent = Intent(context, MedicationAlarmReceiver::class.java)
                    val snoozePendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        reminderId.toInt() + MedicationAlarmScheduler.SNOOZE_OFFSET,
                        snoozeIntent,
                        android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    if (snoozePendingIntent != null) {
                        alarmManager.cancel(snoozePendingIntent)
                        snoozePendingIntent.cancel()
                    }
                }

                val pendingResult = goAsync()
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "HealthLog:MedicationActionReceiver"
                )?.apply {
                    setReferenceCounted(false)
                    acquire(10_000L)
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = HealthDatabase.getDatabase(context)
                        val repository = MedicationRepository(db.medicationDao())

                        repository.recordDose(
                            medicationId = medicationId,
                            scheduledDate = scheduledDate,
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
                    notificationId = notificationId,
                    reminderId = reminderId
                )
            }
        }
    }
}
