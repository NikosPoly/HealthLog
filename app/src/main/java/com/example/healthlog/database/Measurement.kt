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
    // Nullable because measurements saved before this column existed have no pulse.
    val tertiaryValue: Double? = null,

    // Whether an arrhythmia / irregular heartbeat was detected (for BP and Pulse).
    val hasArrhythmia: Boolean? = null,

    // Meal context for blood glucose (e.g. BEFORE_MEAL, AFTER_MEAL_2H).
    val mealContext: String? = null,

    // Arm used for blood pressure (e.g. LEFT, RIGHT).
    val arm: String? = null,

    // Body position during blood pressure measurement (e.g. SITTING, STANDING, LYING_DOWN).
    val bodyPosition: String? = null,

    val timestamp: Long = System.currentTimeMillis()
)
