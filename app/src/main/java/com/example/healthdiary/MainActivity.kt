
package com.example.healthdiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthdiary.database.HealthDatabase
import com.example.healthdiary.database.Measurement
import com.example.healthdiary.ui.theme.HealthDiaryTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = HealthDatabase
            .getDatabase(applicationContext)
            .measurementDao()

        setContent {
            HealthDiaryTheme {
                HealthDiaryScreen(dao)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDiaryScreen(
    dao: com.example.healthdiary.database.MeasurementDao
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

@Composable
fun MeasurementCard(
    title: String,
    unit: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                unit,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun HistoryCard(
    measurement: Measurement,
    onDelete: () -> Unit
) {
    val formatter = remember {
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale("el", "GR")
        )
    }

    val displayedValue = if (
        measurement.type == "Αρτηριακή πίεση"
    ) {
        "${measurement.value.toInt()}/" +
                "${measurement.secondaryValue?.toInt() ?: "?"} mmHg"
    } else {
        val unit = when (measurement.type) {
            "Παλμοί" -> "bpm"
            "Σάκχαρο" -> "mg/dL"
            "Βάρος" -> "kg"
            else -> ""
        }
        "${measurement.value} $unit"
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                measurement.type,
                fontWeight = FontWeight.Bold
            )

            Text(
                displayedValue,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                formatter.format(Date(measurement.timestamp)),
                style = MaterialTheme.typography.bodySmall
            )

            TextButton(onClick = onDelete) {
                Text("Διαγραφή")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HealthDiaryPreview() {
    HealthDiaryTheme {
        Text("HealthDiary")
    }
}
