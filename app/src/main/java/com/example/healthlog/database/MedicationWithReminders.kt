package com.example.healthlog.database

import androidx.room.Embedded
import androidx.room.Relation

data class MedicationWithReminders(
    @Embedded
    val medication: Medication,

    @Relation(
        parentColumn = "id",
        entityColumn = "medicationId"
    )
    val reminders: List<MedicationReminder> = emptyList()
)
