package com.example.healthlog.ui.chart.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.healthlog.database.Measurement
import com.example.healthlog.ui.format.formatMeasurementValue

// Which number of a blood pressure value ("140/80 mmHg") is bold.
internal enum class Emphasis { NONE, SYSTOLIC, DIASTOLIC }

// One stat: the name on the left, the value on the right (on several lines if needed,
// aligned to the right). Used by the period stats and the non-period stats.
// Values are medium weight; a value with an emphasis is regular, with that number bold,
// so it stands out.
@Composable
internal fun StatLine(label: String, value: String, emphasis: Emphasis) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = buildAnnotatedString {
                append(value)
                emphasizedRange(value, emphasis)?.let { range ->
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold), range.first, range.last + 1)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasis == Emphasis.NONE) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.End
        )
    }
}

// A measurement's value as the stats show it: as in the history list, without the pulse
// (it has its own tab). Used by the period stats and the non-period stats.
internal fun statValueText(measurement: Measurement, decimalSeparator: Char): String =
    formatMeasurementValue(measurement.copy(tertiaryValue = null), decimalSeparator)

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
