package com.example.healthlog.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_reminders",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicationId"])]
)
data class MedicationReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val medicationId: Long,

    // Hour of day (0..23).
    val hour: Int,

    // Minute of hour (0..59).
    val minute: Int,

    // Bitmask for days of week (1=Mon, 2=Tue, 4=Wed, 8=Thu, 16=Fri, 32=Sat, 64=Sun).
    // Default 127 = every day.
    val daysOfWeekMask: Int = 127,

    // Whether this reminder alarm is enabled.
    val isEnabled: Boolean = true,

    // Optional inclusive ISO-8601 start date (e.g. "2026-11-23").
    // Null falls back to the medication's creation date.
    val startDate: String? = null,

    // Optional inclusive ISO-8601 end date (e.g. "2026-12-01").
    // Null means the recurring reminder continues indefinitely.
    val endDate: String? = null
)
