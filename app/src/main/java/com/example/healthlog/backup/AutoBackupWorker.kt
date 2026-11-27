package com.example.healthlog.backup

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.healthlog.database.HealthDatabase
import com.example.healthlog.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Background worker that performs periodic automatic backups.
 */
class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val storage = SharedPrefsBackupStorage(applicationContext)
        val schedule = storage.schedule.value

        if (!schedule.isEnabled) {
            return@withContext Result.success()
        }

        try {
            val database = HealthDatabase.getDatabase(applicationContext)
            val repository = BackupRepository(database)

            val passwordStr = storage.getAutoBackupPassword()
            val isEncrypted = !passwordStr.isNullOrBlank()

            val payload = repository.createBackupPayload(isEncrypted = isEncrypted)
            val plainJson = BackupSerializer.toJson(payload)

            val finalContent = if (isEncrypted) {
                BackupCrypto.encrypt(plainJson, passwordStr!!.toCharArray(), payload.metadata)
            } else {
                plainJson
            }

            val timestamp = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))
            val extension = if (isEncrypted) "hlbackup" else "json"
            val fileName = "healthlog_autobackup_$dateStr.$extension"

            val customUri = storage.customFolderUri.value
            val savedSuccessfully = if (customUri != null) {
                saveToCustomTree(customUri, fileName, finalContent)
            } else {
                saveToInternalBackups(fileName, finalContent)
            }

            if (savedSuccessfully) {
                storage.setLastBackupTimestamp(timestamp)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun saveToInternalBackups(fileName: String, content: String): Boolean {
        return try {
            val backupsDir = File(applicationContext.filesDir, "backups").apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(backupsDir, fileName)
            FileOutputStream(targetFile).use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
            // Rotate old internal backups, keep last MAX_AUTO_BACKUPS
            rotateFiles(backupsDir, MAX_AUTO_BACKUPS)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun saveToCustomTree(treeUri: android.net.Uri, fileName: String, content: String): Boolean {
        return try {
            val rootDoc = DocumentFile.fromTreeUri(applicationContext, treeUri) ?: return false
            if (!rootDoc.canWrite()) return false

            val mimeType = if (fileName.endsWith(".json")) "application/json" else "application/octet-stream"
            val newFile = rootDoc.createFile(mimeType, fileName) ?: return false

            applicationContext.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } ?: return false

            rotateDocumentFiles(rootDoc, MAX_AUTO_BACKUPS)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun rotateFiles(dir: File, maxCount: Int) {
        val files = dir.listFiles { f -> f.name.startsWith("healthlog_autobackup_") } ?: return
        if (files.size > maxCount) {
            files.sortedBy { it.lastModified() }
                .take(files.size - maxCount)
                .forEach { it.delete() }
        }
    }

    private fun rotateDocumentFiles(root: DocumentFile, maxCount: Int) {
        val files = root.listFiles().filter { it.name?.startsWith("healthlog_autobackup_") == true }
        if (files.size > maxCount) {
            files.sortedBy { it.lastModified() }
                .take(files.size - maxCount)
                .forEach { it.delete() }
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "healthlog_auto_backup_worker"
        const val MAX_AUTO_BACKUPS = 5

        /**
         * Ensures periodic auto-backup work is scheduled on app startup without resetting
         * the existing periodic timer if already scheduled.
         */
        fun ensureScheduled(context: Context, schedule: BackupSchedule) {
            val workManager = WorkManager.getInstance(context)

            if (!schedule.isEnabled) {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }

            val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                schedule.repeatIntervalDays,
                TimeUnit.DAYS
            )
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * Schedules or cancels periodic auto-backup work according to [schedule],
         * updating an existing schedule immediately when the user changes preferences.
         */
        fun updateSchedule(context: Context, schedule: BackupSchedule) {
            val workManager = WorkManager.getInstance(context)

            if (!schedule.isEnabled) {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }

            val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                schedule.repeatIntervalDays,
                TimeUnit.DAYS
            )
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
