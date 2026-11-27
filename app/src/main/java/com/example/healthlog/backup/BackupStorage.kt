package com.example.healthlog.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface BackupStorage {
    val schedule: StateFlow<BackupSchedule>
    val lastBackupTimestamp: StateFlow<Long>
    val customFolderUri: StateFlow<Uri?>
    val isAutoBackupPasswordEnabled: StateFlow<Boolean>

    fun setSchedule(schedule: BackupSchedule)
    fun setLastBackupTimestamp(timestamp: Long)
    fun setCustomFolderUri(uri: Uri?)
    fun getAutoBackupPassword(): String?
    fun setAutoBackupPassword(password: String?)

    fun isBackupOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean
}

class SharedPrefsBackupStorage(context: Context) : BackupStorage {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _schedule = MutableStateFlow(readSchedule())
    override val schedule: StateFlow<BackupSchedule> = _schedule.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L))
    override val lastBackupTimestamp: StateFlow<Long> = _lastBackupTimestamp.asStateFlow()

    private val _customFolderUri = MutableStateFlow(readCustomFolderUri())
    override val customFolderUri: StateFlow<Uri?> = _customFolderUri.asStateFlow()

    private val _isAutoBackupPasswordEnabled =
        MutableStateFlow(!prefs.getString(KEY_AUTO_BACKUP_PASSWORD, null).isNullOrBlank())
    override val isAutoBackupPasswordEnabled: StateFlow<Boolean> = _isAutoBackupPasswordEnabled.asStateFlow()

    override fun setSchedule(schedule: BackupSchedule) {
        prefs.edit().putString(KEY_BACKUP_SCHEDULE, schedule.name).apply()
        _schedule.value = schedule
    }

    override fun setLastBackupTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP_TIMESTAMP, timestamp).apply()
        _lastBackupTimestamp.value = timestamp
    }

    override fun setCustomFolderUri(uri: Uri?) {
        if (uri != null) {
            prefs.edit().putString(KEY_CUSTOM_FOLDER_URI, uri.toString()).apply()
        } else {
            prefs.edit().remove(KEY_CUSTOM_FOLDER_URI).apply()
        }
        _customFolderUri.value = uri
    }

    override fun getAutoBackupPassword(): String? {
        val pw = prefs.getString(KEY_AUTO_BACKUP_PASSWORD, null)
        return if (pw.isNullOrBlank()) null else pw
    }

    override fun setAutoBackupPassword(password: String?) {
        if (password.isNullOrBlank()) {
            prefs.edit().remove(KEY_AUTO_BACKUP_PASSWORD).apply()
            _isAutoBackupPasswordEnabled.value = false
        } else {
            prefs.edit().putString(KEY_AUTO_BACKUP_PASSWORD, password).apply()
            _isAutoBackupPasswordEnabled.value = true
        }
    }

    /**
     * Determines whether a backup is considered overdue.
     * True if:
     * - Auto-backup is disabled (OFF), OR
     * - A backup was never taken (lastBackupTimestamp == 0L), OR
     * - The time elapsed since the last backup exceeds the active schedule interval.
     */
    override fun isBackupOverdue(nowMillis: Long): Boolean {
        val currentSchedule = _schedule.value
        val lastTime = _lastBackupTimestamp.value

        if (!currentSchedule.isEnabled) {
            return true // Warning if auto-backup is completely turned off
        }
        if (lastTime <= 0L) {
            return true // Never backed up
        }

        val elapsed = nowMillis - lastTime
        return elapsed > currentSchedule.intervalMillis
    }

    private fun readSchedule(): BackupSchedule {
        val name = prefs.getString(KEY_BACKUP_SCHEDULE, null)
        return BackupSchedule.fromNameOrNull(name) ?: BackupSchedule.DEFAULT
    }

    private fun readCustomFolderUri(): Uri? {
        val str = prefs.getString(KEY_CUSTOM_FOLDER_URI, null) ?: return null
        return try {
            Uri.parse(str)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val PREFS_NAME = "backup_settings"
        const val KEY_BACKUP_SCHEDULE = "key_backup_schedule"
        const val KEY_LAST_BACKUP_TIMESTAMP = "key_last_backup_timestamp"
        const val KEY_CUSTOM_FOLDER_URI = "key_custom_folder_uri"
        const val KEY_AUTO_BACKUP_PASSWORD = "key_auto_backup_password"
    }
}
