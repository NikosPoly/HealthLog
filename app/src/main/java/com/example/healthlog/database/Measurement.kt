package com.example.healthlog.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.healthlog.model.MeasurementType

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Stored by name, e.g. "BLOOD_PRESSURE".
    val type: MeasurementType,

    val value: Double,

    val secondaryValue: Double? = null,

    // Pulse recorded together with blood pressure.
    // Nullable because entries saved before this column existed have no pulse.
    val tertiaryValue: Double? = null,

    val timestamp: Long = System.currentTimeMillis()
)
