package com.example.healthlog.ui.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

// Handles persisting exported clinical reports directly into the device's public Downloads directory.
object ExportFileManager {

    private const val SUBFOLDER = "HealthLog"

    // Saves the provided file into Downloads/HealthLog and returns the resulting content/file URI, or null on failure.
    fun saveToDownloads(
        context: Context,
        file: File,
        mimeType: String,
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, file, mimeType)
        } else {
            saveViaLegacyStorage(file)
        }
    }

    private fun saveViaMediaStore(
        context: Context,
        file: File,
        mimeType: String,
    ): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + File.separator + SUBFOLDER,
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: return null

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun saveViaLegacyStorage(file: File): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(downloadsDir, SUBFOLDER).apply { mkdirs() }
        val targetFile = File(targetDir, file.name)

        return try {
            file.inputStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(targetFile)
        } catch (e: Exception) {
            null
        }
    }
}
