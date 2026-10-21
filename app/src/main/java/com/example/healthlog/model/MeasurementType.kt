package com.example.healthlog.model

import androidx.annotation.StringRes
import com.example.healthlog.R

// One input value of a measurement, e.g. "Συστολική (mmHg)".
// The label is a string resource, so it can be translated.
data class MeasurementField(
    @param:StringRes val label: Int,
    val unit: String,
    val allowsDecimals: Boolean
)

// The kinds of measurement the app records, each with the values it is made of.
// The constant name (e.g. BLOOD_PRESSURE) is what the database stores, so renaming
// or deleting a constant needs a database migration.
// The fields are stored in order in value, secondaryValue and tertiaryValue,
// so a type can have at most three: don't reorder fields or remove one in the middle.
// Labels, units and allowsDecimals are only in the code and can be changed freely.
enum class MeasurementType(
    @param:StringRes val label: Int,
    val fields: List<MeasurementField>
) {
    BLOOD_PRESSURE(
        R.string.type_blood_pressure,
        listOf(
            MeasurementField(R.string.field_systolic, "mmHg", allowsDecimals = false),
            MeasurementField(R.string.field_diastolic, "mmHg", allowsDecimals = false),
            MeasurementField(R.string.type_pulse, "bpm", allowsDecimals = false)
        )
    ),
    PULSE(
        R.string.type_pulse,
        listOf(MeasurementField(R.string.type_pulse, "bpm", allowsDecimals = false))
    ),
    BLOOD_SUGAR(
        R.string.type_blood_sugar,
        listOf(MeasurementField(R.string.type_blood_sugar, "mg/dL", allowsDecimals = false))
    ),
    WEIGHT(
        R.string.type_weight,
        listOf(MeasurementField(R.string.type_weight, "kg", allowsDecimals = true))
    ),
    HEIGHT(
        R.string.type_height,
        listOf(MeasurementField(R.string.type_height, "cm", allowsDecimals = false))
    )
}
