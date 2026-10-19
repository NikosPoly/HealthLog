package com.example.healthlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthlog.database.Measurement

@Composable
fun MeasurementEntryDialog(
    category: String,
    onDismiss: () -> Unit,
    onSave: (Measurement) -> Unit
) {
    // rememberSaveable keeps the typed text when the screen is rotated.
    var value1 by rememberSaveable { mutableStateOf("") }
    var value2 by rememberSaveable { mutableStateOf("") }
    var value3 by rememberSaveable { mutableStateOf("") }

    val isPressure = category == "Αρτηριακή πίεση"

    // Weight allows decimals (72,5); every other value is a whole number.
    // This applies to the first field; the blood pressure fields are always whole numbers.
    val firstFieldAllowsDecimals = category == "Βάρος"

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
                    onValueChange = { input ->
                        if (isAllowedInput(input, firstFieldAllowsDecimals)) value1 = input
                    },
                    label = {
                        Text(
                            if (isPressure) "Συστολική (mmHg)"
                            else when (category) {
                                "Παλμοί" -> "Παλμοί (bpm)"
                                "Σάκχαρο" -> "Σάκχαρο (mg/dL)"
                                "Ύψος" -> "Ύψος (cm)"
                                else -> "Βάρος (kg)"
                            }
                        )
                    },
                    singleLine = true,
                    keyboardOptions = numberKeyboard(firstFieldAllowsDecimals)
                )

                if (isPressure) {
                    OutlinedTextField(
                        value = value2,
                        onValueChange = { input ->
                            if (isAllowedInput(input, allowDecimals = false)) value2 = input
                        },
                        label = {
                            Text("Διαστολική (mmHg)")
                        },
                        singleLine = true,
                        keyboardOptions = numberKeyboard(allowDecimals = false)
                    )

                    OutlinedTextField(
                        value = value3,
                        onValueChange = { input ->
                            if (isAllowedInput(input, allowDecimals = false)) value3 = input
                        },
                        label = {
                            Text("Παλμοί (bpm)")
                        },
                        singleLine = true,
                        keyboardOptions = numberKeyboard(allowDecimals = false)
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

// Only lets digits through; with allowDecimals, also one ',' or '.' (e.g. "72,5").
// Anything else typed or pasted is ignored, so letters never appear in the field.
private fun isAllowedInput(text: String, allowDecimals: Boolean): Boolean {
    val pattern = if (allowDecimals) Regex("[0-9]*([.][0-9]*)?") else Regex("[0-9]*")
    return pattern.matches(text)
}

// Number pad for whole numbers, number pad with a decimal separator for decimals.
private fun numberKeyboard(allowDecimals: Boolean) = KeyboardOptions(
    keyboardType = if (allowDecimals) KeyboardType.Decimal else KeyboardType.Number
)

private fun parsePositiveNumber(text: String): Double? {
    val number = text.trim()
        .replace(',', '.')
        .toDoubleOrNull()

    return number?.takeIf {
        it.isFinite() && it > 0.0
    }
}
