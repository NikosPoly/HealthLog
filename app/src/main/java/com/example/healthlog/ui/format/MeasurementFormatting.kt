package com.example.healthlog.ui.format

import com.example.healthlog.database.Measurement

// The value text of a measurement, e.g. "120/80 mmHg · 72 bpm" or "78.0 kg".
// Used by the history card and the delete confirmation, so both show the same text.
fun formatMeasurementValue(measurement: Measurement): String =
    if (measurement.type == "Αρτηριακή πίεση") {
        val pressure = "${measurement.value.toInt()}/" +
                "${measurement.secondaryValue?.toInt() ?: "?"} mmHg"

        // Older entries have no pulse, so show it only when present.
        val pulse = measurement.tertiaryValue?.let { " · ${it.toInt()} bpm" } ?: ""

        pressure + pulse
    } else {
        val unit = when (measurement.type) {
            "Παλμοί" -> "bpm"
            "Σάκχαρο" -> "mg/dL"
            "Βάρος" -> "kg"
            "Ύψος" -> "cm"
            else -> ""
        }
        "${if (measurement.type == "Βάρος") measurement.value else formatNumber(measurement.value)} $unit"
    }

// Whole numbers without ".0" (180 cm); older entries with real decimals keep them (5.6).
private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
