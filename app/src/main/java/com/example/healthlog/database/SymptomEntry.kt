package com.example.healthlog.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "symptoms",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class SymptomEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Standardized symptom category key (matches SymptomType.name, e.g. "HEADACHE", "CUSTOM").
    val symptomType: String,

    // Optional user-entered label when symptomType == "CUSTOM".
    val customName: String? = null,

    // Severity rating from 1 (Very Mild) to 5 (Severe).
    val severity: Int,

    // Epoch milliseconds when the symptom occurred.
    val timestamp: Long = System.currentTimeMillis(),

    // Optional short contextual notes about the symptom.
    val notes: String? = null
)
