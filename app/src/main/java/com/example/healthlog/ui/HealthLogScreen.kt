package com.example.healthlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.database.Measurement
import com.example.healthlog.ui.components.HistoryCard
import com.example.healthlog.ui.components.MeasurementCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthLogScreen(
    dao: com.example.healthlog.database.MeasurementDao
) {
    val measurements by dao.getAllMeasurements()
        .collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedCategory by remember {
        mutableStateOf<String?>(null)
    }

    var value1 by remember { mutableStateOf("") }
    var value2 by remember { mutableStateOf("") }

    fun openEntry(category: String) {
        value1 = ""
        value2 = ""
        selectedCategory = category
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
                        scope.launch {
                            dao.delete(measurement)
                            snackbarHostState.showSnackbar(
                                "Η μέτρηση διαγράφηκε"
                            )
                        }
                    }
                )
            }
        }
    }

    selectedCategory?.let { category ->
        val isPressure = category == "Αρτηριακή πίεση"

        val firstNumber = parsePositiveNumber(value1)
        val secondNumber = parsePositiveNumber(value2)

        val valid = if (isPressure) {
            firstNumber != null &&
                    secondNumber != null &&
                    firstNumber > secondNumber
        } else {
            firstNumber != null
        }

        AlertDialog(
            onDismissRequest = {
                selectedCategory = null
            },
            title = {
                Text(category)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = value1,
                        onValueChange = { value1 = it },
                        label = {
                            Text(
                                if (isPressure) "Συστολική (mmHg)"
                                else when (category) {
                                    "Παλμοί" -> "Παλμοί (bpm)"
                                    "Σάκχαρο" -> "Σάκχαρο (mg/dL)"
                                    else -> "Βάρος (kg)"
                                }
                            )
                        },
                        singleLine = true
                    )

                    if (isPressure) {
                        OutlinedTextField(
                            value = value2,
                            onValueChange = { value2 = it },
                            label = {
                                Text("Διαστολική (mmHg)")
                            },
                            singleLine = true
                        )
                    }

                    Text(
                        "Η ημερομηνία και η ώρα αποθηκεύονται αυτόματα.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        val first = firstNumber ?: return@TextButton

                        val newMeasurement = Measurement(
                            type = category,
                            value = first,
                            secondaryValue = if (isPressure) {
                                secondNumber
                            } else {
                                null
                            }
                        )

                        selectedCategory = null

                        scope.launch {
                            dao.insert(newMeasurement)
                            snackbarHostState.showSnackbar(
                                "Η μέτρηση αποθηκεύτηκε"
                            )
                        }
                    }
                ) {
                    Text("Αποθήκευση")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedCategory = null }
                ) {
                    Text("Ακύρωση")
                }
            }
        )
    }
}

private fun parsePositiveNumber(text: String): Double? {
    val number = text.trim()
        .replace(',', '.')
        .toDoubleOrNull()

    return number?.takeIf {
        it.isFinite() && it > 0.0
    }
}
