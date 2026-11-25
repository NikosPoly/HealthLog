package com.example.healthlog.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medicationId"]),
        Index(value = ["scheduledDate"])
    ]
)
data class MedicationDoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val medicationId: Long,

    // ISO-8601 calendar date for the dose schedule (e.g. "2026-11-18").
    val scheduledDate: String,

    // 24-hour time string for the dose schedule (e.g. "08:00").
    val scheduledTime: String,

    // Timestamp when the action was recorded (taken or skipped).
    val actionTimestamp: Long = System.currentTimeMillis(),

    // Status: "TAKEN", "SKIPPED", or "UNLOGGED".
    val status: String,

    // Optional user note for this dose.
    val notes: String? = null
) {
    companion object {
        const val STATUS_TAKEN = "TAKEN"
        const val STATUS_SKIPPED = "SKIPPED"
        const val STATUS_UNLOGGED = "UNLOGGED"
    }
}
