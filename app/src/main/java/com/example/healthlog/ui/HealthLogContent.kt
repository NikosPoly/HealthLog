package com.example.healthlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.database.Measurement
import com.example.healthlog.ui.components.MeasurementHistoryCard
import com.example.healthlog.ui.components.MeasurementPickerCard
import com.example.healthlog.ui.components.MeasurementEntryDialog
import com.example.healthlog.ui.theme.HealthLogTheme

// Layout only: draws the screen from the values it receives
// and reports user actions through the callbacks.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthLogContent(
    measurements: List<Measurement>,
    selectedCategory: String?,
    snackbarHostState: SnackbarHostState,
    onOpenEntry: (String) -> Unit,
    onDismissEntry: () -> Unit,
    onSave: (Measurement) -> Unit,
    onDelete: (Measurement) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ημερολόγιο Υγείας") }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenEntry("Αρτηριακή πίεση") }
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
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
                bottom = 90.dp
            )
        ) {
            item {
                Text(
                    "Νέα μέτρηση",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                MeasurementPickerCard("Αρτηριακή πίεση", "mmHg") {
                    onOpenEntry("Αρτηριακή πίεση")
                }
            }

            item {
                MeasurementPickerCard("Παλμοί", "bpm") {
                    onOpenEntry("Παλμοί")
                }
            }

            item {
                MeasurementPickerCard("Σάκχαρο", "mg/dL") {
                    onOpenEntry("Σάκχαρο")
                }
            }

            item {
                MeasurementPickerCard("Βάρος", "kg") {
                    onOpenEntry("Βάρος")
                }
            }

            item {
                MeasurementPickerCard("Ύψος", "cm") {
                    onOpenEntry("Ύψος")
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Ιστορικό μετρήσεων",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

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
                        onDelete(measurement)
                    }
                )
            }
        }
    }

    selectedCategory?.let { category ->
        MeasurementEntryDialog(
            category = category,
            onDismiss = onDismissEntry,
            onSave = onSave
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HealthLogContentPreview() {
    HealthLogTheme {
        HealthLogContent(
            measurements = listOf(
                Measurement(
                    id = 1,
                    type = "Αρτηριακή πίεση",
                    value = 120.0,
                    secondaryValue = 80.0
                ),
                Measurement(
                    id = 2,
                    type = "Βάρος",
                    value = 80.0
                )
            ),
            selectedCategory = null,
            snackbarHostState = remember { SnackbarHostState() },
            onOpenEntry = {},
            onDismissEntry = {},
            onSave = {},
            onDelete = {}
        )
    }
}
