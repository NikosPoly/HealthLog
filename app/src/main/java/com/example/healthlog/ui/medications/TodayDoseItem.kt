package com.example.healthlog.ui.medications

import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationReminder

enum class DoseStatus {
    PENDING,
    TAKEN,
    SKIPPED
}

data class TodayDoseItem(
    val logId: Long? = null,
    val medication: Medication,
    val reminder: MedicationReminder?,
    val scheduledTime: String,
    val scheduledDate: String,
    val status: DoseStatus,
    val actionTimestamp: Long? = null,
    val isAsNeeded: Boolean = false
)
