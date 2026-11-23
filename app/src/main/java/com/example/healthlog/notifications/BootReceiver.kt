package com.example.healthlog.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.healthlog.database.HealthDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val isRescheduleAction = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        if (isRescheduleAction) {
            val pendingResult = goAsync()

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "HealthLog:BootReceiver"
            )?.apply {
                setReferenceCounted(false)
                acquire(10_000L)
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = HealthDatabase.getDatabase(context)
                    val activeMedications = db.medicationDao().getActiveMedicationsWithReminders().first()

                    activeMedications.forEach { medWithReminders ->
                        medWithReminders.reminders
                            .filter { it.isEnabled }
                            .forEach { reminder ->
                                MedicationAlarmScheduler.scheduleReminder(
                                    context = context,
                                    medication = medWithReminders.medication,
                                    reminder = reminder
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
}
