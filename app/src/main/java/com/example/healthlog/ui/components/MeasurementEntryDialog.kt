package com.example.healthlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.healthlog.database.Measurement

@Composable
fun MeasurementEntryDialog(
    category: String,
    onDismiss: () -> Unit,
    onSave: (Measurement) -> Unit
) {
    var value1 by remember { mutableStateOf("") }
    var value2 by remember { mutableStateOf("") }
    var value3 by remember { mutableStateOf("") }

    val isPressure = category == "Αρτηριακή πίεση"

    val firstNumber = parsePositiveNumber(value1)
    val secondNumber = parsePositiveNumber(value2)
    val thirdNumber = parsePositiveNumber(value3)

    val valid = if (isPressure) {
        firstNumber != null &&
                secondNumber != null &&
                firstNumber > secondNumber &&
                thirdNumber != null
    } else {
        firstNumber != null
    }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
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

                    OutlinedTextField(
                        value = value3,
                        onValueChange = { value3 = it },
                        label = {
                            Text("Παλμοί (bpm)")
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
                        },
                        tertiaryValue = if (isPressure) {
                            thirdNumber
                        } else {
                            null
                        }
                    )

                    onSave(newMeasurement)
                }
            ) {
                Text("Αποθήκευση")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text("Ακύρωση")
            }
        }
    )
}

private fun parsePositiveNumber(text: String): Double? {
    val number = text.trim()
        .replace(',', '.')
        .toDoubleOrNull()

    return number?.takeIf {
        it.isFinite() && it > 0.0
    }
}
