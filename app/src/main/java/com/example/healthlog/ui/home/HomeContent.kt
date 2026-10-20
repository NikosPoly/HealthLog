package com.example.healthlog.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.components.MeasurementEntryDialog
import com.example.healthlog.ui.components.MeasurementPickerCard
import com.example.healthlog.ui.theme.HealthLogTheme

// Layout only: the home screen with the measurement picker,
// drawn from the values it receives; user actions are reported through the callbacks.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    selectedType: MeasurementType?,
    snackbarHostState: SnackbarHostState,
    onOpenEntry: (MeasurementType) -> Unit,
    onDismissEntry: () -> Unit,
    onSave: (Measurement) -> Unit,
    onOpenHistory: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ημερολόγιο Υγείας") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            painter = painterResource(R.drawable.ic_history),
                            contentDescription = "Ιστορικό μετρήσεων"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenEntry(MeasurementType.BLOOD_PRESSURE) }
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

            // One card per measurement type, in the order they are declared in the enum.
            // The card shows the unit of the first value (mmHg for blood pressure).
            items(MeasurementType.entries) { type ->
                MeasurementPickerCard(type.label, type.fields.first().unit) {
                    onOpenEntry(type)
                }
            }
        }
    }

    selectedType?.let { type ->
        MeasurementEntryDialog(
            type = type,
            onDismiss = onDismissEntry,
            onSave = onSave
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    HealthLogTheme {
        HomeContent(
            selectedType = null,
            snackbarHostState = remember { SnackbarHostState() },
            onOpenEntry = {},
            onDismissEntry = {},
            onSave = {},
            onOpenHistory = {}
        )
    }
}
