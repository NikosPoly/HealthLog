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

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    HealthLogContent(
        measurements = measurements,
        selectedCategory = selectedCategory,
        snackbarHostState = snackbarHostState,
        onOpenEntry = viewModel::openEntry,
        onDismissEntry = viewModel::closeEntry,
        onSave = viewModel::save,
        onDelete = viewModel::delete
    )
}
