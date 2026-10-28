package com.example.healthlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.ValidationError
import com.example.healthlog.model.validateMeasurement
import com.example.healthlog.ui.format.decimalSeparator
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

    // Which fields the user has already left: moved to another field, tapped elsewhere
    // in the dialog, or pressed Done. Some errors are only shown after that, so they
    // don't appear while still typing.
    var leftFields by rememberSaveable { mutableStateOf(List(type.fields.size) { false }) }

    // The field the user is in (null = none). Saved across rotation, so the field still
    // counts as "the one being edited" even though Android removes the focus on rotation.
    var focusedIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    // The user leaves the field they were in: from now on its errors are shown.
    // Only called for the user's own actions, never when focus is lost by itself
    // (e.g. on rotation), so rotating doesn't trigger errors.
    fun leaveFocusedField() {
        val index = focusedIndex ?: return
        if (!leftFields[index]) {
            leftFields = leftFields.toMutableList().also { it[index] = true }
        }
        focusedIndex = null
    }

    // The decimal separator of the current language: "," in Greek, "." in English.
    val separator = decimalSeparator()

    // If the language changes while the dialog is open, convert what is already typed
    // to the new separator (e.g. "82.56" → "82,56"), so it can still be edited.
    LaunchedEffect(separator) {
        texts = texts.map { text -> text.replace(',', separator).replace('.', separator) }
    }

    // The typed texts with "." as separator, e.g. "72,5" → "72.5". The validation and
    // the saved values always work with "."; only what is typed and shown uses the
    // language's separator. Both "," and "." are converted, so a text typed before
    // switching the language is still read correctly.
    val normalizedTexts = texts.map { it.replace(',', '.') }

    // One entry per field: null = valid.
    val errors = validateMeasurement(type, normalizedTexts)

    // Save is possible only when every field passes every check.
    val valid = errors.all { it == null }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        // A tap anywhere in the dialog outside a text field leaves the current field,
        // so its errors appear (see clearFocusOnTap in FocusModifiers.kt).
        modifier = Modifier.clearFocusOnTap(onTap = { leaveFocusedField() }),
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
                        isErrorVisible(it, normalizedTexts[index], leftFields[index])
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        OutlinedTextField(
                            value = texts[index],
                            onValueChange = { input ->
                                if (isAllowedInput(input, field.maxDecimals, separator)) {
                                    texts = texts.toMutableList().also { it[index] = input }
                                }
                            },
                            label = {
                                Text(
                                    stringResource(
                                        R.string.field_label_with_unit,
                                        stringResource(field.label),
                                        field.unit
                                    )
                                )
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
                                onDone = {
                                    leaveFocusedField()
                                    focusManager.clearFocus()
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    // Only gaining focus matters. Moving here from another
                                    // field leaves that one; tapping back into the same field
                                    // (e.g. after rotation) doesn't count as leaving it.
                                    // Losing focus is ignored: it also happens on rotation.
                                    if (focusState.isFocused && focusedIndex != index) {
                                        leaveFocusedField()
                                        focusedIndex = index
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
            // A disabled button swallows taps, so this Box notices them first.
            Box(modifier = Modifier.clearFocusOnPress(onPress = { leaveFocusedField() })) {
                TextButton(
                    enabled = valid,
                    onClick = {
                        if (!valid) return@TextButton

                        // All fields are valid numbers here, stored in order:
                        // value, secondaryValue, tertiaryValue.
                        val values = normalizedTexts.map { it.toDouble() }

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

// When an error is shown. Save is blocked by every error, but some are only shown
// once the field has been left, so they don't appear while the user is still typing:
// - empty, or ending with the decimal separator → after leaving the field
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
