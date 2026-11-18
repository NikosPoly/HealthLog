package com.example.healthlog.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Name of the medication (e.g. "Amlodipine", "Metformin").
    val name: String,

    // Numerical dosage or amount as string (e.g. "5", "500", "1").
    val dosage: String,

    // Unit of measurement (e.g. "mg", "ml", "tablet", "drops", "puff").
    val unit: String,

    // Recurrence frequency description (e.g. "DAILY", "TWICE_DAILY", "AS_NEEDED").
    val frequency: String,

    // Optional patient notes or instructions (e.g. "Take with food / after dinner").
    val instructions: String? = null,

    // Whether this medication is currently being taken (false archives it without losing history).
    val isActive: Boolean = true,

    // Timestamp when this medication entry was created.
    val createdAt: Long = System.currentTimeMillis()
)
