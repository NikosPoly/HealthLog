package com.example.healthlog.ui.format

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import java.util.Locale

// The value text of a measurement, e.g. "120/80 mmHg · 72 bpm" or "78.00 kg".
// Used by the history card and the delete confirmation, so both show the same text.
fun formatMeasurementValue(measurement: Measurement): String {
    val fields = measurement.type.fields

    return if (measurement.type == MeasurementType.BLOOD_PRESSURE) {
        val pressure = "${measurement.value.toInt()}/" +
                "${measurement.secondaryValue?.toInt() ?: "?"} ${fields[0].unit}"

        // Older entries have no pulse, so show it only when present.
        val pulse = measurement.tertiaryValue?.let { " · ${it.toInt()} ${fields[2].unit}" } ?: ""

        pressure + pulse
    } else {
        val field = fields.first()

        // Values with decimals (weight) always show all of them: 72 → "72.00", 72.4 → "72.40".
        // Locale.ROOT keeps "." as the separator, whatever the phone's language.
        val number = if (field.allowsDecimals) {
            String.format(Locale.ROOT, "%.${field.maxDecimals}f", measurement.value)
        } else {
            formatNumber(measurement.value)
        }

        "$number ${field.unit}"
    }
}

// Whole numbers without ".0" (180 cm); older entries with real decimals keep them (5.6).
private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
