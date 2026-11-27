package com.example.healthlog.backup

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class FakeBackupStorage(
    initialSchedule: BackupSchedule = BackupSchedule.WEEKLY,
    initialLastBackup: Long = 0L
) : BackupStorage {
    private val _schedule = MutableStateFlow(initialSchedule)
    override val schedule: StateFlow<BackupSchedule> = _schedule.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow(initialLastBackup)
    override val lastBackupTimestamp: StateFlow<Long> = _lastBackupTimestamp.asStateFlow()

    private val _customFolderUri = MutableStateFlow<Uri?>(null)
    override val customFolderUri: StateFlow<Uri?> = _customFolderUri.asStateFlow()

    private var autoBackupPassword: String? = null
    private val _isAutoBackupPasswordEnabled = MutableStateFlow(false)
    override val isAutoBackupPasswordEnabled: StateFlow<Boolean> = _isAutoBackupPasswordEnabled.asStateFlow()

    override fun setSchedule(schedule: BackupSchedule) {
        _schedule.value = schedule
    }

    override fun setLastBackupTimestamp(timestamp: Long) {
        _lastBackupTimestamp.value = timestamp
    }

    override fun setCustomFolderUri(uri: Uri?) {
        _customFolderUri.value = uri
    }

    override fun getAutoBackupPassword(): String? = autoBackupPassword

    override fun setAutoBackupPassword(password: String?) {
        autoBackupPassword = password
        _isAutoBackupPasswordEnabled.value = !password.isNullOrBlank()
    }

    override fun isBackupOverdue(nowMillis: Long): Boolean {
        val currentSchedule = _schedule.value
        val lastTime = _lastBackupTimestamp.value

        if (!currentSchedule.isEnabled) {
            return true
        }
        if (lastTime <= 0L) {
            return true
        }

        val elapsed = nowMillis - lastTime
        return elapsed > currentSchedule.intervalMillis
    }
}

class BackupScheduleAndStorageTest {

    private lateinit var storage: FakeBackupStorage

    @Before
    fun setUp() {
        storage = FakeBackupStorage()
    }

    @Test
    fun backupSchedule_valuesAndIntervals() {
        assertEquals(1L, BackupSchedule.DAILY.days)
        assertEquals(3L, BackupSchedule.THREE_DAYS.days)
        assertEquals(7L, BackupSchedule.WEEKLY.days)
        assertEquals(14L, BackupSchedule.TWO_WEEKS.days)
        assertEquals(30L, BackupSchedule.MONTHLY.days)
        assertEquals(0L, BackupSchedule.OFF.days)

        assertTrue(BackupSchedule.DAILY.isEnabled)
        assertTrue(BackupSchedule.WEEKLY.isEnabled)
        assertFalse(BackupSchedule.OFF.isEnabled)

        assertEquals(TimeUnit.DAYS.toMillis(7), BackupSchedule.WEEKLY.intervalMillis)
        assertEquals(BackupSchedule.WEEKLY, BackupSchedule.fromNameOrNull("WEEKLY"))
        assertNull(BackupSchedule.fromNameOrNull("UNKNOWN"))
    }

    @Test
    fun isBackupOverdue_neverBackedUp_returnsTrue() {
        storage.setSchedule(BackupSchedule.WEEKLY)
        storage.setLastBackupTimestamp(0L)

        assertTrue(storage.isBackupOverdue(nowMillis = 100_000_000L))
    }

    @Test
    fun isBackupOverdue_scheduleOff_returnsTrue() {
        storage.setSchedule(BackupSchedule.OFF)
        storage.setLastBackupTimestamp(100_000_000L)

        // If auto-backup is disabled, it should alert the user to back up
        assertTrue(storage.isBackupOverdue(nowMillis = 100_000_000L))
    }

    @Test
    fun isBackupOverdue_recentBackup_returnsFalse() {
        storage.setSchedule(BackupSchedule.WEEKLY)
        val now = 1_000_000_000L
        val threeDaysAgo = now - TimeUnit.DAYS.toMillis(3)
        storage.setLastBackupTimestamp(threeDaysAgo)

        assertFalse(storage.isBackupOverdue(nowMillis = now))
    }

    @Test
    fun isBackupOverdue_elapsedExceedsInterval_returnsTrue() {
        storage.setSchedule(BackupSchedule.WEEKLY)
        val now = 1_000_000_000L
        val eightDaysAgo = now - TimeUnit.DAYS.toMillis(8)
        storage.setLastBackupTimestamp(eightDaysAgo)

        assertTrue(storage.isBackupOverdue(nowMillis = now))
    }

    @Test
    fun isBackupOverdue_dailySchedule_respectsOneDay() {
        storage.setSchedule(BackupSchedule.DAILY)
        val now = 1_000_000_000L
        val twelveHoursAgo = now - TimeUnit.HOURS.toMillis(12)
        storage.setLastBackupTimestamp(twelveHoursAgo)

        assertFalse(storage.isBackupOverdue(nowMillis = now))

        val thirtyHoursAgo = now - TimeUnit.HOURS.toMillis(30)
        storage.setLastBackupTimestamp(thirtyHoursAgo)

        assertTrue(storage.isBackupOverdue(nowMillis = now))
    }
}
