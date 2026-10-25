package com.example.healthlog.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.ValidationError
import com.example.healthlog.model.validateMeasurement
import com.example.healthlog.ui.format.validationErrorMessage

@Composable
fun MeasurementEntryDialog(
    type: MeasurementType,
    onDismiss: () -> Unit,
    onSave: (Measurement) -> Unit
) {
    // The typed text of each field, in the order of type.fields.
    // rememberSaveable keeps the typed text when the screen is rotated.
    var texts by rememberSaveable { mutableStateOf(List(type.fields.size) { "" }) }

    // Which fields the user has already left (had focus, then lost it).
    // Some errors are only shown after that, so they don't appear while still typing.
    var leftFields by rememberSaveable { mutableStateOf(List(type.fields.size) { false }) }

    // One entry per field: null = valid.
    val errors = validateMeasurement(type, texts)

    // Save is possible only when every field passes every check.
    val valid = errors.all { it == null }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        // A tap anywhere in the dialog outside a text field leaves the current field,
        // so its errors appear (see clearFocusOnTap below).
        modifier = Modifier.clearFocusOnTap(),
        title = {
            Text(stringResource(type.label))
        },
        text = {
            // Taken inside the dialog: the dialog has its own window and its own focus.
            // Used by the "Done" key below.
            val focusManager = LocalFocusManager.current

            // Scrollable, so every field and the note stay reachable when the
            // keyboard leaves little space (e.g. landscape with three fields).
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // One text field per value of the type.
                type.fields.forEachIndexed { index, field ->
                    // The error of this field, only if it should be shown right now.
                    val visibleError = errors[index]?.takeIf {
                        isErrorVisible(it, texts[index], leftFields[index])
                    }

                    // Remembers whether this field has had focus, to notice when it is left.
                    var hadFocus by remember { mutableStateOf(false) }

                    Row(verticalAlignment = Alignment.Top) {
                        OutlinedTextField(
                            value = texts[index],
                            onValueChange = { input ->
                                if (isAllowedInput(input, field.allowsDecimals)) {
                                    texts = texts.toMutableList().also { it[index] = input }
                                }
                            },
                            label = {
                                Text("${stringResource(field.label)} (${field.unit})")
                            },
                            isError = visibleError != null,
                            supportingText = visibleError?.let { error ->
                                { Text(validationErrorMessage(error)) }
                            },
                            singleLine = true,
                            keyboardOptions = numberKeyboard(
                                allowDecimals = field.allowsDecimals,
                                isLast = index == type.fields.lastIndex
                            ),
                            // "Done" on the last field also leaves it, so its errors appear.
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        hadFocus = true
                                    } else if (hadFocus && !leftFields[index]) {
                                        leftFields = leftFields.toMutableList().also { it[index] = true }
                                    }
                                }
                        )

                        // Mandatory marker, outside the field.
                        Text(
                            "*",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 8.dp, top = 16.dp)
                        )
                    }
                }

                Text(
                    stringResource(R.string.entry_timestamp_note),
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    stringResource(R.string.entry_required_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    if (!valid) return@TextButton

                    // All fields are valid numbers here, stored in order:
                    // value, secondaryValue, tertiaryValue.
                    val values = texts.map { it.toDouble() }

                    val newMeasurement = Measurement(
                        type = type,
                        value = values[0],
                        secondaryValue = values.getOrNull(1),
                        tertiaryValue = values.getOrNull(2)
                    )

                    onSave(newMeasurement)
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

// Leaves the focused text field when this element is tapped.
// "composed" reads the focus manager where the modifier is used, i.e. inside the dialog's
// own window; otherwise it would clear the focus of the screen behind the dialog.
private fun Modifier.clearFocusOnTap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    pointerInput(focusManager) {
        detectTapGestures { focusManager.clearFocus() }
    }
}

// When an error is shown. Save is blocked by every error, but some are only shown
// once the field has been left, so they don't appear while the user is still typing:
// - empty, or ending with "." → after leaving the field
// - too high → immediately, since typing more digits can't fix it
// - too low → after leaving the field, since the user may still be typing (e.g. "12" on the way to "120")
// - systolic/diastolic comparison and invalid text → immediately
private fun isErrorVisible(error: ValidationError, text: String, fieldLeft: Boolean): Boolean =
    when (error) {
        ValidationError.Required,
        ValidationError.IncompleteDecimal -> fieldLeft

        is ValidationError.OutOfRange -> {
            val number = text.toDoubleOrNull()
            (number != null && number > error.max) || fieldLeft
        }

        ValidationError.InvalidNumber,
        ValidationError.DiastolicNotBelowSystolic -> true
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
