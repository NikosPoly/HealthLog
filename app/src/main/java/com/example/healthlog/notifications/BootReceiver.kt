package com.example.healthlog.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.healthlog.database.HealthDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
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
                    pendingResult.finish()
                }
            }
        }
    }
}
