package com.example.healthlog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.healthlog.database.HealthDatabase
import com.example.healthlog.notifications.MedicationNotificationHelper
import com.example.healthlog.repository.MeasurementRepository
import com.example.healthlog.repository.MedicationRepository
import com.example.healthlog.ui.HealthLogNavHost
import com.example.healthlog.ui.theme.HealthLogTheme

import com.example.healthlog.model.MedicationScheduleResolver
import com.example.healthlog.notifications.MedicationAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local medication notification channel
        MedicationNotificationHelper.createNotificationChannel(applicationContext)

        // Prompt for POST_NOTIFICATIONS only on the very first launch ever
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val hasRequestedNotificationsStartup = prefs.getBoolean("has_requested_notifications_startup", false)
        if (!hasRequestedNotificationsStartup && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            prefs.edit().putBoolean("has_requested_notifications_startup", true).apply()
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        val database = HealthDatabase.getDatabase(applicationContext)
        val measurementRepository = MeasurementRepository(database.measurementDao())
        val medicationRepository = MedicationRepository(database.medicationDao())
        val symptomRepository = com.example.healthlog.repository.SymptomRepository(database.symptomDao())

        setContent {
            HealthLogTheme {
                HealthLogNavHost(
                    repository = measurementRepository,
                    medicationRepository = medicationRepository,
                    symptomRepository = symptomRepository
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Synchronize scheduled alarms with the current notification permission state:
        // - If notifications are disabled: cancel all alarms from the OS scheduler without altering the
        //   user's medication isActive status in the database.
        // - If notifications are enabled: ensure all active medications have their alarms scheduled.
        val notificationsEnabled = MedicationNotificationHelper.areNotificationsEnabled(this)
        val db = HealthDatabase.getDatabase(applicationContext)
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        CoroutineScope(Dispatchers.IO).launch {
            val dao = db.medicationDao()
            val medicationRepository = MedicationRepository(dao)

            // Materialize any unlogged past recurring reminder slots up through yesterday
            val lastSweptDate = MedicationScheduleResolver.parseIsoDateOrNull(
                prefs.getString(KEY_LAST_UNLOGGED_DOSE_SWEEP_DATE, null)
            )
            val newSweptDate = medicationRepository.sweepUnloggedPastDoses(
                lastSweptDate = lastSweptDate
            )
            if (newSweptDate != lastSweptDate) {
                prefs.edit().putString(KEY_LAST_UNLOGGED_DOSE_SWEEP_DATE, newSweptDate.toString()).apply()
            }

            val activeMeds = dao.getActiveMedicationsWithReminders().first()
            activeMeds.forEach { medWithReminders ->
                if (medWithReminders.medication.frequency == "DAILY") {
                    medWithReminders.reminders.forEach { reminder ->
                        val overrides = dao.getOverridesForReminder(reminder.id)
                        if (!notificationsEnabled || !reminder.isEnabled) {
                            MedicationAlarmScheduler.cancelReminder(
                                applicationContext,
                                reminder.id,
                                overrides
                            )
                        } else {
                            MedicationAlarmScheduler.scheduleReminder(
                                applicationContext,
                                medWithReminders.medication,
                                reminder,
                                overrides
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_LAST_UNLOGGED_DOSE_SWEEP_DATE = "last_unlogged_dose_sweep_date"
    }
}
