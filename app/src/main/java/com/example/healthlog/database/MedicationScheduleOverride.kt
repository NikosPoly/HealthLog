package com.example.healthlog.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_schedule_overrides",
    foreignKeys = [
        ForeignKey(
            entity = MedicationReminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medicationId"]),
        Index(value = ["originalDate"]),
        Index(value = ["reminderId", "originalDate"], unique = true)
    ]
)
data class MedicationScheduleOverride(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // The recurring reminder being overridden for a single date.
    val reminderId: Long,

    // Denormalized medication ID for fast querying and cascade deletion.
    val medicationId: Long,

    // ISO-8601 calendar date of the occurrence being overridden (e.g. "2026-11-23").
    val originalDate: String,

    // Override type: "RESCHEDULED" (moved to a new time on originalDate) or "CANCELLED" (removed for originalDate).
    val overrideType: String,

    // New hour (0..23) when overrideType == "RESCHEDULED"; null when "CANCELLED".
    val newHour: Int? = null,

    // New minute (0..59) when overrideType == "RESCHEDULED"; null when "CANCELLED".
    val newMinute: Int? = null
) {
    companion object {
        const val TYPE_RESCHEDULED = "RESCHEDULED"
        const val TYPE_CANCELLED = "CANCELLED"
    }
}
