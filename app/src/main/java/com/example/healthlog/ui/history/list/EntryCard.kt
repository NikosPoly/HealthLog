package com.example.healthlog.ui.history.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

// One entry in the history list: its date and time, its value, and a bin to delete it.
// The type isn't repeated on the card, since the tab above already shows it.
@Composable
fun EntryCard(
    measurement: Measurement,
    onDelete: () -> Unit
) {
    // Value and date in the formats of the current language.
    val displayedValue = formatMeasurementValue(measurement, decimalSeparator())
    val dateFormatter = rememberDateTimeFormatter()

    Card(
        modifier = Modifier.fillMaxWidth(),
        // Same color as the chart's cards, so both modes look alike.
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            // Less space on the right: the bin button has its own padding.
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    formatTimestamp(measurement.timestamp, dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    displayedValue,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    // Read aloud by screen readers, and how tests find the button.
                    contentDescription = stringResource(R.string.action_delete),
                    // Multicolored icon: keep its own colors.
                    tint = Color.Unspecified
                )
            }
        }
    }
}

// Previews: blood pressure with pulse, and weight with decimals.
@Preview(showBackground = true)
@Composable
private fun EntryCardPreview() {
    HealthLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EntryCard(
                measurement = Measurement(
                    type = MeasurementType.BLOOD_PRESSURE,
                    value = 120.0,
                    secondaryValue = 80.0,
                    tertiaryValue = 72.0
                ),
                onDelete = {}
            )

            EntryCard(
                measurement = Measurement(type = MeasurementType.WEIGHT, value = 72.5),
                onDelete = {}
            )
        }
    }
}
