package com.example.healthlog.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

// Connector: reads the HomeViewModel's state, shows its messages,
// and hands everything to the layout as plain values and callbacks.
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenHistory: () -> Unit
) {
    val selectedType by viewModel.selectedType.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    HomeContent(
        selectedType = selectedType,
        snackbarHostState = snackbarHostState,
        onOpenEntry = viewModel::openEntry,
        onDismissEntry = viewModel::closeEntry,
        onSave = viewModel::save,
        onOpenHistory = onOpenHistory
    )
}
