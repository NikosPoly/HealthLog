package com.example.healthlog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.database.Measurement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MeasurementHistoryCard(
    measurement: Measurement,
    onDelete: () -> Unit
) {
    val formatter = remember {
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale("el", "GR")
        )
    }

    val displayedValue = if (
        measurement.type == "Αρτηριακή πίεση"
    ) {
        val pressure = "${measurement.value.toInt()}/" +
                "${measurement.secondaryValue?.toInt() ?: "?"} mmHg"

        // Older entries have no pulse, so show it only when present.
        val pulse = measurement.tertiaryValue?.let { " - ${it.toInt()} bpm" } ?: ""

        pressure + pulse
    } else {
        val unit = when (measurement.type) {
            "Παλμοί" -> "bpm"
            "Σάκχαρο" -> "mg/dL"
            "Βάρος" -> "kg"
            else -> ""
        }
        "${measurement.value} $unit"
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                measurement.type,
                fontWeight = FontWeight.Bold
            )

            Text(
                displayedValue,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                formatter.format(Date(measurement.timestamp)),
                style = MaterialTheme.typography.bodySmall
            )

            TextButton(onClick = onDelete) {
                Text("Διαγραφή")
            }
        }
    }
}
