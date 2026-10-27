package com.example.healthlog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatMeasurementValue
import com.example.healthlog.ui.format.formatTimestamp
import com.example.healthlog.ui.format.rememberDateTimeFormatter

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
