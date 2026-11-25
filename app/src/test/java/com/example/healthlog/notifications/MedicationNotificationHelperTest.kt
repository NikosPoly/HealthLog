package com.example.healthlog.notifications

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationNotificationHelperTest {

    @Test
    fun applyAlertFlags_setsInsistentFlagWhenKeepRingingEnabled_preservingExistingFlags() {
        val notification = Notification().apply {
            flags = Notification.FLAG_AUTO_CANCEL
        }

        MedicationNotificationHelper.applyAlertFlags(notification, keepRinging = true)

        assertEquals(
            Notification.FLAG_AUTO_CANCEL or Notification.FLAG_INSISTENT,
            notification.flags
        )
    }

    @Test
    fun applyAlertFlags_doesNotSetInsistentFlagWhenKeepRingingDisabled() {
        val notification = Notification().apply {
            flags = Notification.FLAG_AUTO_CANCEL
        }

        MedicationNotificationHelper.applyAlertFlags(notification, keepRinging = false)

        assertEquals(Notification.FLAG_AUTO_CANCEL, notification.flags)
    }
}
