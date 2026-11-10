package com.example.healthlog.ui.home.newentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.theme.icon

@Composable
fun NewEntryDialog(
    type: MeasurementType,
    onDismiss: () -> Unit,
    onSave: (Measurement) -> Unit
) {
    // Everything the user has typed and done in the dialog, with the dialog's rules
    // (see NewEntryState.kt). rememberSaveable keeps it when the screen is rotated.
    var state by rememberSaveable(stateSaver = entryStateSaver) {
        mutableStateOf(NewEntryState.initial(type))
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
            // The type's icon next to its name, as on its home card.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(type.icon),
                    // The title next to it says the type.
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    // Multicolored icon: keep its own colors.
                    tint = Color.Unspecified
                )
                Text(stringResource(type.label))
            }
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
                    EntryFieldInput(
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

                // Measurement options depending on type
                when (type) {
                    MeasurementType.BLOOD_PRESSURE -> {
                        OptionSelectorGroup(
                            title = stringResource(R.string.section_arm),
                            options = listOf(
                                SelectorOption(com.example.healthlog.model.MeasurementArm.LEFT, stringResource(R.string.arm_left)),
                                SelectorOption(com.example.healthlog.model.MeasurementArm.RIGHT, stringResource(R.string.arm_right))
                            ),
                            selectedValue = state.arm,
                            onSelect = { state = state.withArm(it) }
                        )

                        OptionSelectorGroup(
                            title = stringResource(R.string.section_position),
                            options = listOf(
                                SelectorOption(com.example.healthlog.model.BodyPosition.SITTING, stringResource(R.string.position_sitting)),
                                SelectorOption(com.example.healthlog.model.BodyPosition.STANDING, stringResource(R.string.position_standing)),
                                SelectorOption(com.example.healthlog.model.BodyPosition.LYING_DOWN, stringResource(R.string.position_lying_down))
                            ),
                            selectedValue = state.bodyPosition,
                            onSelect = { state = state.withBodyPosition(it) }
                        )

                        OptionSelectorGroup(
                            title = stringResource(R.string.section_arrhythmia),
                            options = listOf(
                                SelectorOption(false, stringResource(R.string.arrhythmia_no)),
                                SelectorOption(true, stringResource(R.string.arrhythmia_yes))
                            ),
                            selectedValue = state.hasArrhythmia,
                            onSelect = { state = state.withArrhythmia(it) }
                        )
                    }

                    MeasurementType.PULSE -> {
                        OptionSelectorGroup(
                            title = stringResource(R.string.section_arrhythmia),
                            options = listOf(
                                SelectorOption(false, stringResource(R.string.arrhythmia_no)),
                                SelectorOption(true, stringResource(R.string.arrhythmia_yes))
                            ),
                            selectedValue = state.hasArrhythmia,
                            onSelect = { state = state.withArrhythmia(it) }
                        )
                    }

                    MeasurementType.BLOOD_SUGAR -> {
                        OptionSelectorGroup(
                            title = stringResource(R.string.section_meal_context),
                            options = listOf(
                                SelectorOption(com.example.healthlog.model.MealContext.BEFORE_MEAL, stringResource(R.string.meal_before)),
                                SelectorOption(com.example.healthlog.model.MealContext.AFTER_MEAL_2H, stringResource(R.string.meal_after_2h))
                            ),
                            selectedValue = state.mealContext,
                            onSelect = { state = state.withMealContext(it) }
                        )
                    }

                    MeasurementType.WEIGHT,
                    MeasurementType.HEIGHT -> {
                        // No extra clinical context options
                    }
                }

                // Date and time selection (automatic or custom)
                DateTimePickerSection(
                    useCurrentDateTime = state.useCurrentDateTime,
                    timestamp = state.customTimestamp,
                    onUseCurrentChange = { state = state.withUseCurrentDateTime(it) },
                    onTimestampChange = { state = state.withCustomTimestamp(it) }
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

// Saves the dialog's state across rotation: the type by name, the typed texts,
// the left fields, focused field, and all clinical options and custom datetime.
private val entryStateSaver = listSaver<NewEntryState, Any?>(
    save = {
        listOf(
            it.type.name,
            it.texts,
            it.leftFields,
            it.focusedIndex,
            it.hasArrhythmia,
            it.arm?.name,
            it.bodyPosition?.name,
            it.mealContext?.name,
            it.useCurrentDateTime,
            it.customTimestamp
        )
    },
    restore = {
        @Suppress("UNCHECKED_CAST")
        NewEntryState(
            type = MeasurementType.valueOf(it[0] as String),
            texts = it[1] as List<String>,
            leftFields = it[2] as List<Boolean>,
            focusedIndex = it[3] as Int?,
            hasArrhythmia = it[4] as Boolean?,
            arm = (it[5] as String?)?.let { armName -> com.example.healthlog.model.MeasurementArm.valueOf(armName) },
            bodyPosition = (it[6] as String?)?.let { posName -> com.example.healthlog.model.BodyPosition.valueOf(posName) },
            mealContext = (it[7] as String?)?.let { mealName -> com.example.healthlog.model.MealContext.valueOf(mealName) },
            useCurrentDateTime = it[8] as Boolean,
            customTimestamp = it[9] as Long
        )
    }
)
