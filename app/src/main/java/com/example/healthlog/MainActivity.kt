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

        setContent {
            HealthLogTheme {
                HealthLogNavHost(
                    repository = measurementRepository,
                    medicationRepository = medicationRepository
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If the user stopped giving notification permission, immediately disable active alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val db = HealthDatabase.getDatabase(applicationContext)
                CoroutineScope(Dispatchers.IO).launch {
                    val activeMeds = db.medicationDao().getActiveMedicationsWithReminders().first()
                    activeMeds.forEach { medWithReminders ->
                        if (medWithReminders.medication.frequency == "DAILY" && medWithReminders.reminders.isNotEmpty()) {
                            medWithReminders.reminders.forEach { reminder ->
                                MedicationAlarmScheduler.cancelReminder(applicationContext, reminder.id)
                            }
                            db.medicationDao().updateMedication(medWithReminders.medication.copy(isActive = false))
                        }
                    }
                }
            }
        }
    }
}
