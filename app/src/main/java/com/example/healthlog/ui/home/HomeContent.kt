package com.example.healthlog.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.home.newmeasurement.MeasurementEntryDialog
import com.example.healthlog.ui.theme.HealthLogTheme
import com.example.healthlog.ui.theme.TypeTheme

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
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            painter = painterResource(R.drawable.ic_history),
                            contentDescription = stringResource(R.string.history_title),
                            // Multicolored icon: keep its own colors.
                            tint = Color.Unspecified
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
                    stringResource(R.string.home_new_measurement),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // One card per measurement type, in the order they are declared in the enum.
            // The card shows the unit of the first value (mmHg for blood pressure),
            // and is in the type's color (see TypeTheme).
            items(MeasurementType.entries) { type ->
                TypeTheme(type) {
                    MeasurementPickerCard(stringResource(type.label), type.fields.first().unit) {
                        onOpenEntry(type)
                    }
                }
            }
        }
    }

    selectedType?.let { type ->
        // In the color of the chosen type, like its card.
        TypeTheme(type) {
            MeasurementEntryDialog(
                type = type,
                onDismiss = onDismissEntry,
                onSave = onSave
            )
        }
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
