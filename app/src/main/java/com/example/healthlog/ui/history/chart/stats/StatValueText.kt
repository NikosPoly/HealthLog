package com.example.healthlog.ui.history.chart.stats

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.healthlog.database.Measurement
import com.example.healthlog.ui.format.formatMeasurementValue

// Which number of a blood pressure value ("140/80 mmHg") is bold.
internal enum class Emphasis { NONE, SYSTOLIC, DIASTOLIC }

// A measurement's value as the stats show it: as in the history list, without the pulse
// (it has its own tab).
internal fun statValueText(measurement: Measurement, decimalSeparator: Char): String =
    formatMeasurementValue(measurement.copy(tertiaryValue = null), decimalSeparator)

// The value with the emphasized number in bold, e.g. the systolic in "140/80 mmHg".
// Without an emphasis, the value is returned as it is.
internal fun emphasizedValue(value: String, emphasis: Emphasis): AnnotatedString =
    buildAnnotatedString {
        append(value)
        emphasizedRange(value, emphasis)?.let { range ->
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), range.first, range.last + 1)
        }
    }

// Where the emphasized number is in a blood pressure text: before the "/" for the
// systolic, between the "/" and the space for the diastolic. Null for other texts.
private fun emphasizedRange(value: String, emphasis: Emphasis): IntRange? {
    val slash = value.indexOf('/')
    if (emphasis == Emphasis.NONE || slash < 0) return null

    return when (emphasis) {
        Emphasis.SYSTOLIC -> 0 until slash
        Emphasis.DIASTOLIC -> {
            val space = value.indexOf(' ', startIndex = slash).takeIf { it >= 0 } ?: value.length
            (slash + 1) until space
        }
        Emphasis.NONE -> null
    }
}
