package com.example.healthlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.format.decimalSeparator

@Composable
fun MeasurementEntryDialog(
    type: MeasurementType,
    onDismiss: () -> Unit,
    onSave: (Measurement) -> Unit
) {
    // Everything the user has typed and done in the dialog, with the dialog's rules
    // (see MeasurementEntryState.kt). rememberSaveable keeps it when the screen is rotated.
    var state by rememberSaveable(stateSaver = entryStateSaver) {
        mutableStateOf(MeasurementEntryState.initial(type))
    }

    // The decimal separator of the current language: "," in Greek, "." in English.
    val separator = decimalSeparator()

    // If the language changes while the dialog is open, convert what is already typed
    // to the new separator, so it can still be edited.
    LaunchedEffect(separator) {
        state = state.withSeparator(separator)
    }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        // A tap anywhere in the dialog outside a text field leaves the current field,
        // so its errors appear (see clearFocusOnTap in FocusModifiers.kt).
        modifier = Modifier.clearFocusOnTap(onTap = { state = state.leaveFocusedField() }),
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
                    MeasurementFieldInput(
                        field = field,
                        text = state.texts[index],
                        error = state.visibleError(index),
                        separator = separator,
                        isLast = index == type.fields.lastIndex,
                        onTextChange = { input -> state = state.withText(index, input) },
                        // Only gaining focus matters; losing focus is ignored,
                        // since it also happens on rotation.
                        onFocused = { state = state.focusField(index) },
                        // "Done" on the last field also leaves it, so its errors appear.
                        onDone = {
                            state = state.leaveFocusedField()
                            focusManager.clearFocus()
                        }
                    )
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
            Box(modifier = Modifier.clearFocusOnPress(onPress = { state = state.leaveFocusedField() })) {
                TextButton(
                    // Save is possible only when every field passes every check.
                    enabled = state.isValid,
                    onClick = {
                        state.toMeasurement()?.let(onSave)
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

// Saves the dialog's state across rotation: the type by name, plus the typed texts,
// the left fields and the focused field, which Android can store as they are.
private val entryStateSaver = listSaver<MeasurementEntryState, Any?>(
    save = { listOf(it.type.name, it.texts, it.leftFields, it.focusedIndex) },
    restore = {
        @Suppress("UNCHECKED_CAST")
        MeasurementEntryState(
            type = MeasurementType.valueOf(it[0] as String),
            texts = it[1] as List<String>,
            leftFields = it[2] as List<Boolean>,
            focusedIndex = it[3] as Int?
        )
    }
)
