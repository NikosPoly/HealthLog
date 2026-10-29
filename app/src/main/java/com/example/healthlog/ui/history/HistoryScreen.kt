package com.example.healthlog.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import com.example.healthlog.model.MeasurementType

// Connector: reads the HistoryViewModel's state, shows its messages,
// and hands everything to the layout as plain values and callbacks.
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    // The type whose page is shown, and the callback when another page is shown.
    // Kept by HealthLogNavHost, so it is remembered when the history is opened again.
    selectedType: MeasurementType,
    onSelectType: (MeasurementType) -> Unit,
    onBack: () -> Unit
) {
    // The list of every type, for the pages of the history.
    val measurementsByType = MeasurementType.entries.associateWith { type ->
        viewModel.measurementsOf(type).collectAsState().value
    }
    val measurementToDelete by viewModel.measurementToDelete.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { messageId ->
            snackbarHostState.showSnackbar(resources.getString(messageId))
        }
    }

    HistoryContent(
        measurementsByType = measurementsByType,
        selectedType = selectedType,
        measurementToDelete = measurementToDelete,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSelectType = onSelectType,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete
    )
}
