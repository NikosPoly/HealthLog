package com.example.healthlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType

@Composable
fun MeasurementEntryDialog(
    type: MeasurementType,
    onDismiss: () -> Unit,
    onSave: (Measurement) -> Unit
) {
    // The typed text of each field, in the order of type.fields.
    // rememberSaveable keeps the typed text when the screen is rotated.
    var texts by rememberSaveable { mutableStateOf(List(type.fields.size) { "" }) }

    val numbers = texts.map { parsePositiveNumber(it) }
    val values = numbers.filterNotNull()

    // Every field is required.
    val allFilled = values.size == type.fields.size

    // Blood pressure: the systolic value must be higher than the diastolic.
    val valid = allFilled &&
            (type != MeasurementType.BLOOD_PRESSURE || values[0] > values[1])

    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        title = {
            Text(type.label)
        },
        text = {
            // Scrollable, so every field and the note stay reachable when the
            // keyboard leaves little space (e.g. landscape with three fields).
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // One text field per value of the type.
                type.fields.forEachIndexed { index, field ->
                    OutlinedTextField(
                        value = texts[index],
                        onValueChange = { input ->
                            if (isAllowedInput(input, field.allowsDecimals)) {
                                texts = texts.toMutableList().also { it[index] = input }
                            }
                        },
                        label = {
                            Text("${field.label} (${field.unit})")
                        },
                        singleLine = true,
                        keyboardOptions = numberKeyboard(
                            allowDecimals = field.allowsDecimals,
                            isLast = index == type.fields.lastIndex
                        )
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
                    if (!valid) return@TextButton

                    // The values are stored in order: value, secondaryValue, tertiaryValue.
                    val newMeasurement = Measurement(
                        type = type,
                        value = values[0],
                        secondaryValue = values.getOrNull(1),
                        tertiaryValue = values.getOrNull(2)
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
// The action key moves to the next field ("Next"), and closes the keyboard
// on the last field ("Done"), so the Save button becomes visible again.
private fun numberKeyboard(allowDecimals: Boolean, isLast: Boolean) = KeyboardOptions(
    keyboardType = if (allowDecimals) KeyboardType.Decimal else KeyboardType.Number,
    imeAction = if (isLast) ImeAction.Done else ImeAction.Next
)

private fun parsePositiveNumber(text: String): Double? {
    val number = text.trim()
        .replace(',', '.')
        .toDoubleOrNull()

    return number?.takeIf {
        it.isFinite() && it > 0.0
    }
}
