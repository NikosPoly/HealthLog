package com.example.healthlog.model

import androidx.annotation.StringRes
import com.example.healthlog.R

// One input value of a measurement, e.g. "Συστολική (mmHg)".
// The label is a string resource, so it can be translated.
// minValue and maxValue are the accepted range (both included).
// maxDecimals is how many decimal digits can be typed: 0 = whole numbers only.
data class MeasurementField(
    @param:StringRes val label: Int,
    val unit: String,
    val minValue: Double,
    val maxValue: Double,
    val maxDecimals: Int = 0
) {
    val allowsDecimals: Boolean
        get() = maxDecimals > 0
}

// The kinds of measurement the app records, each with the values it is made of.
// The constant name (e.g. BLOOD_PRESSURE) is what the database stores, so renaming
// or deleting a constant needs a database migration.
// The fields are stored in order in value, secondaryValue and tertiaryValue,
// so a type can have at most three: don't reorder fields or remove one in the middle.
// Labels, units, ranges and decimals are only in the code and can be changed freely.
enum class MeasurementType(
    @param:StringRes val label: Int,
    val fields: List<MeasurementField>
) {
    BLOOD_PRESSURE(
        R.string.type_blood_pressure,
        listOf(
            MeasurementField(R.string.field_systolic, "mmHg", minValue = 31.0, maxValue = 300.0),
            MeasurementField(R.string.field_diastolic, "mmHg", minValue = 30.0, maxValue = 150.0),
            MeasurementField(R.string.type_pulse, "bpm", minValue = 30.0, maxValue = 300.0)
        )
    ),
    PULSE(
        R.string.type_pulse,
        listOf(MeasurementField(R.string.type_pulse, "bpm", minValue = 30.0, maxValue = 300.0))
    ),
    BLOOD_SUGAR(
        R.string.type_blood_sugar,
        listOf(MeasurementField(R.string.type_blood_sugar, "mg/dL", minValue = 30.0, maxValue = 650.0))
    ),
    WEIGHT(
        R.string.type_weight,
        listOf(MeasurementField(R.string.type_weight, "kg", minValue = 1.0, maxValue = 300.0, maxDecimals = 2))
    ),
    HEIGHT(
        R.string.type_height,
        listOf(MeasurementField(R.string.type_height, "cm", minValue = 30.0, maxValue = 250.0))
    )
}
