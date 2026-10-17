package com.example.healthlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.ui.components.HistoryCard
import com.example.healthlog.ui.components.MeasurementCard
import com.example.healthlog.ui.components.MeasurementEntryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthLogScreen(
    viewModel: HealthLogViewModel
) {
    val measurements by viewModel.measurements.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val selectedCategory by viewModel.selectedCategory.collectAsState()

    fun openEntry(category: String) {
        viewModel.openEntry(category)
    }

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
                onClick = { openEntry("Αρτηριακή πίεση") }
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
                MeasurementCard("Αρτηριακή πίεση", "mmHg") {
                    openEntry("Αρτηριακή πίεση")
                }
            }

            item {
                MeasurementCard("Παλμοί", "bpm") {
                    openEntry("Παλμοί")
                }
            }

            item {
                MeasurementCard("Σάκχαρο", "mg/dL") {
                    openEntry("Σάκχαρο")
                }
            }

            item {
                MeasurementCard("Βάρος", "kg") {
                    openEntry("Βάρος")
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
                HistoryCard(
                    measurement = measurement,
                    onDelete = {
                        viewModel.delete(measurement)
                    }
                )
            }
        }
    }

    selectedCategory?.let { category ->
        MeasurementEntryDialog(
            category = category,
            onDismiss = {
                viewModel.closeEntry()
            },
            onSave = { newMeasurement ->
                viewModel.save(newMeasurement)
            }
        )
    }
}
