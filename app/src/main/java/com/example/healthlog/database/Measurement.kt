package com.example.healthlog.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: String,

    val value: Double,

    val secondaryValue: Double? = null,

    // Pulse recorded together with blood pressure.
    // Nullable because entries saved before this column existed have no pulse.
    val tertiaryValue: Double? = null,

    val timestamp: Long = System.currentTimeMillis()
)
