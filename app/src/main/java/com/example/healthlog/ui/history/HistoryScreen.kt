package com.example.healthlog.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.chart.ChartPeriod

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

    // List or chart. Kept here, inside the history screen: it stays while the history
    // is open (also on rotation and when changing tabs), and the history always opens
    // on the list.
    var showTrend by rememberSaveable { mutableStateOf(false) }

    // The period of the chart. All when the history opens; back to All when another tab
    // is shown (selectedType is its key), but kept on rotation.
    var period by rememberSaveable(selectedType) { mutableStateOf(ChartPeriod.ALL) }

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
        showTrend = showTrend,
        period = period,
        measurementToDelete = measurementToDelete,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSelectType = onSelectType,
        onToggleTrend = { showTrend = !showTrend },
        onSelectPeriod = { period = it },
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onCancelDelete = viewModel::cancelDelete
    )
}
