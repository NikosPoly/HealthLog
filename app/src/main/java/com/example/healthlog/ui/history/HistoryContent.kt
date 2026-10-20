package com.example.healthlog.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.components.DeleteConfirmationDialog
import com.example.healthlog.ui.components.MeasurementHistoryCard
import com.example.healthlog.ui.format.formatMeasurementValue
import com.example.healthlog.ui.theme.HealthLogTheme

// Layout only: the history screen, drawn from the values it receives;
// user actions are reported through the callbacks.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    measurements: List<Measurement>,
    measurementToDelete: Measurement?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRequestDelete: (Measurement) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ιστορικό μετρήσεων") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Πίσω"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 16.dp
            )
        ) {
            if (measurements.isEmpty()) {
                item {
                    Text("Δεν υπάρχουν ακόμη καταχωρίσεις.")
                }
            }

            items(
                items = measurements,
                key = { it.id }
            ) { measurement ->
                MeasurementHistoryCard(
                    measurement = measurement,
                    onDelete = {
                        onRequestDelete(measurement)
                    }
                )
            }
        }
    }

    // Shown outside the list, so it doesn't depend on the card being visible.
    measurementToDelete?.let { measurement ->
        DeleteConfirmationDialog(
            details = "${measurement.type.label}: ${formatMeasurementValue(measurement)}",
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryContentPreview() {
    HealthLogTheme {
        HistoryContent(
            measurements = listOf(
                Measurement(
                    id = 1,
                    type = MeasurementType.BLOOD_PRESSURE,
                    value = 120.0,
                    secondaryValue = 80.0,
                    tertiaryValue = 72.0
                ),
                Measurement(
                    id = 2,
                    type = MeasurementType.WEIGHT,
                    value = 78.0
                )
            ),
            measurementToDelete = null,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onCancelDelete = {}
        )
    }
}
