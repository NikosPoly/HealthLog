package com.example.healthlog.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.healthlog.MainActivity
import com.example.healthlog.R

object MedicationNotificationHelper {

    const val CHANNEL_ID = "medication_reminders_channel"

    const val EXTRA_MEDICATION_ID = "extra_medication_id"
    const val EXTRA_MEDICATION_NAME = "extra_medication_name"
    const val EXTRA_DOSAGE = "extra_dosage"
    const val EXTRA_UNIT = "extra_unit"
    const val EXTRA_INSTRUCTIONS = "extra_instructions"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_SCHEDULED_DATE = "extra_scheduled_date"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_OVERRIDE_ID = "extra_override_id"
    const val EXTRA_ALARM_TYPE = "extra_alarm_type"

    const val ALARM_TYPE_MAIN = "MAIN"
    const val ALARM_TYPE_SNOOZE = "SNOOZE"
    const val ALARM_TYPE_OVERRIDE = "OVERRIDE"

    const val ACTION_MARK_TAKEN = "com.example.healthlog.ACTION_MEDICATION_MARK_TAKEN"
    const val ACTION_SNOOZE = "com.example.healthlog.ACTION_MEDICATION_SNOOZE"

    fun areNotificationsEnabled(context: Context): Boolean {
        // 1. App-level permission check across all Android versions (including POST_NOTIFICATIONS on Android 13+)
        if (!androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }

        // 2. Channel-level check: ensure the medication channel exists and is not blocked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }

        return true
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_medications_name)
            val descriptionText = context.getString(R.string.notification_channel_medications_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMedicationNotification(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        unit: String,
        instructions: String?,
        scheduledTime: String,
        notificationId: Int,
        scheduledDate: String? = null,
        reminderId: Long = notificationId.toLong(),
        overrideId: Long = -1L
    ) {
        createNotificationChannel(context)

        // Tapping the notification body opens MainActivity
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Mark Taken
        val markTakenIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_SCHEDULED_DATE, scheduledDate)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_REMINDER_ID, reminderId)
            if (overrideId != -1L) {
                putExtra(EXTRA_OVERRIDE_ID, overrideId)
            }
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 30 minutes
        val snoozeIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_UNIT, unit)
            putExtra(EXTRA_INSTRUCTIONS, instructions)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_SCHEDULED_DATE, scheduledDate)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_REMINDER_ID, reminderId)
            if (overrideId != -1L) {
                putExtra(EXTRA_OVERRIDE_ID, overrideId)
            }
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentTitle = context.getString(R.string.notification_medication_title, medicationName)
        val contentText = if (!instructions.isNullOrBlank()) {
            context.getString(R.string.notification_medication_text, dosage, unit, instructions)
        } else {
            context.getString(R.string.notification_medication_text_no_instructions, dosage, unit)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_medications)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_nav_medications,
                context.getString(R.string.notification_action_mark_taken),
                markTakenPendingIntent
            )
            .addAction(
                R.drawable.ic_nav_calendar,
                context.getString(R.string.notification_action_snooze),
                snoozePendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
