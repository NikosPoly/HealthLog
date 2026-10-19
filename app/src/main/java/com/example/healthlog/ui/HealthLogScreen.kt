package com.example.healthlog.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

// Connector: reads the ViewModel's state, shows its messages,
// and hands everything to the layout as plain values and callbacks.
@Composable
fun HealthLogScreen(
    viewModel: HealthLogViewModel
) {
    val measurements by viewModel.measurements.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val measurementToDelete by viewModel.measurementToDelete.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    HealthLogContent(
        measurements = measurements,
        selectedCategory = selectedCategory,
        measurementToDelete = measurementToDelete,
        snackbarHostState = snackbarHostState,
        onOpenEntry = viewModel::openEntry,
        onDismissEntry = viewModel::closeEntry,
        onSave = viewModel::save,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete
    )
}
