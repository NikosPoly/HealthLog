package com.example.healthlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatMeasurementValue
import com.example.healthlog.ui.format.formatTimestamp
import com.example.healthlog.ui.format.rememberDateTimeFormatter
import com.example.healthlog.ui.theme.HealthLogTheme

@Composable
fun MeasurementHistoryCard(
    measurement: Measurement,
    onDelete: () -> Unit
) {
    // Value and date in the formats of the current language.
    val displayedValue = formatMeasurementValue(measurement, decimalSeparator())
    val dateFormatter = rememberDateTimeFormatter()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                stringResource(measurement.type.label),
                fontWeight = FontWeight.Bold
            )

            Text(
                displayedValue,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                formatTimestamp(measurement.timestamp, dateFormatter),
                style = MaterialTheme.typography.bodySmall
            )

            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}

// Previews: blood pressure with pulse, and weight with decimals.
@Preview(showBackground = true)
@Composable
private fun MeasurementHistoryCardPreview() {
    HealthLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MeasurementHistoryCard(
                measurement = Measurement(
                    type = MeasurementType.BLOOD_PRESSURE,
                    value = 120.0,
                    secondaryValue = 80.0,
                    tertiaryValue = 72.0
                ),
                onDelete = {}
            )

            MeasurementHistoryCard(
                measurement = Measurement(type = MeasurementType.WEIGHT, value = 72.5),
                onDelete = {}
            )
        }
    }
}
