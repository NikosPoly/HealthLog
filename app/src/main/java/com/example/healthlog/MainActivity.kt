package com.example.healthlog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.healthlog.database.HealthDatabase
import com.example.healthlog.model.MedicationScheduleResolver
import com.example.healthlog.notifications.MedicationAlarmScheduler
import com.example.healthlog.notifications.MedicationNotificationHelper
import com.example.healthlog.repository.MeasurementRepository
import com.example.healthlog.repository.MedicationRepository
import com.example.healthlog.repository.SymptomRepository
import com.example.healthlog.security.AndroidBiometricAuthenticator
import com.example.healthlog.security.AppLockManager
import com.example.healthlog.security.AppLockScreen
import com.example.healthlog.ui.HealthLogNavHost
import com.example.healthlog.ui.theme.HealthLogTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var appLockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appLockManager = AppLockManager.getInstance(applicationContext)
        updateSecureWindowFlag(appLockManager.isLockEnabled.value)

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
        val symptomRepository = SymptomRepository(database.symptomDao())
        val userProfileRepository = com.example.healthlog.repository.UserProfileRepository(database.userProfileDao())
        val backupRepository = com.example.healthlog.repository.BackupRepository(database)
        val backupStorage = com.example.healthlog.backup.SharedPrefsBackupStorage(applicationContext)

        // Ensure periodic automatic backup is scheduled according to current user preferences
        com.example.healthlog.backup.AutoBackupWorker.ensureScheduled(
            applicationContext,
            backupStorage.schedule.value
        )

        setContent {
            val isLockEnabled by appLockManager.isLockEnabled.collectAsState()
            val isLocked by appLockManager.isLocked.collectAsState()
            var hasUnlockedOnce by remember { mutableStateOf(!isLocked) }
            var lockErrorMessage by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(isLockEnabled) {
                updateSecureWindowFlag(isLockEnabled)
            }

            LaunchedEffect(isLocked) {
                if (!isLocked) {
                    hasUnlockedOnce = true
                    lockErrorMessage = null
                }
            }

            HealthLogTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Never compose the health navigation graph on cold start until unlocked.
                    // Once unlocked during the session, keep it mounted behind the opaque lock
                    // screen so returning after a background timeout preserves the user's screen.
                    if (hasUnlockedOnce) {
                        HealthLogNavHost(
                            repository = measurementRepository,
                            medicationRepository = medicationRepository,
                            symptomRepository = symptomRepository,
                            userProfileRepository = userProfileRepository,
                            appLockManager = appLockManager,
                            backupRepository = backupRepository,
                            backupStorage = backupStorage
                        )
                    }

                    if (isLocked) {
                        AppLockScreen(
                            errorMessage = lockErrorMessage,
                            onUnlockClick = {
                                lockErrorMessage = null
                                appLockManager.beginAuthentication()
                                AndroidBiometricAuthenticator.authenticate(
                                    activity = this@MainActivity,
                                    title = getString(R.string.app_lock_prompt_title),
                                    subtitle = getString(R.string.app_lock_prompt_subtitle),
                                    onSuccess = {
                                        appLockManager.endAuthentication(succeeded = true)
                                    },
                                    onError = { error ->
                                        appLockManager.endAuthentication(succeeded = false)
                                        lockErrorMessage = error
                                    }
                                )
                            },
                            onBackPressed = {
                                moveTaskToBack(true)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLockManager.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            appLockManager.onAppBackgrounded()
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

    private fun updateSecureWindowFlag(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    companion object {
        private const val KEY_LAST_UNLOGGED_DOSE_SWEEP_DATE = "last_unlogged_dose_sweep_date"
    }
}
