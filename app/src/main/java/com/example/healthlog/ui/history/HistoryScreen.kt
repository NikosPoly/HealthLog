package com.example.healthlog.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

// Connector: reads the HistoryViewModel's state, shows its messages,
// and hands everything to the layout as plain values and callbacks.
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val measurements by viewModel.measurements.collectAsState()
    val measurementToDelete by viewModel.measurementToDelete.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    HistoryContent(
        measurements = measurements,
        measurementToDelete = measurementToDelete,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete
    )
}
