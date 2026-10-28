package com.example.healthlog.ui.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.model.MeasurementField
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.ValidationError
import com.example.healthlog.ui.format.validationErrorMessage
import com.example.healthlog.ui.theme.HealthLogTheme

// One input field of the entry dialog: the text field with its label, unit and error message,
// and the red mandatory marker next to it. The dialog decides the text, which error to show
// and what happens on focus and Done; this only draws the field and reports what the user does.
@Composable
fun MeasurementFieldInput(
    field: MeasurementField,
    text: String,
    // The error shown under the field; null = no error shown.
    error: ValidationError?,
    // The decimal separator of the current language: "," in Greek, "." in English.
    separator: Char,
    // The last field gets "Done" on the keyboard, the others "Next".
    isLast: Boolean,
    onTextChange: (String) -> Unit,
    // Called when the field gains focus.
    onFocused: () -> Unit,
    // Called when "Done" is pressed on the keyboard.
    onDone: () -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                if (isAllowedInput(input, field.maxDecimals, separator)) {
                    onTextChange(input)
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
            isError = error != null,
            supportingText = error?.let {
                { Text(validationErrorMessage(it)) }
            },
            singleLine = true,
            keyboardOptions = numberKeyboard(
                allowDecimals = field.allowsDecimals,
                isLast = isLast
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone() }
            ),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFocused()
                    }
                }
        )

        // Mandatory marker, outside the field.
        Text(
            "*",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 8.dp, top = 25.dp)
        )
    }
}

// Previews: a field without an error, and one with an error under it.
@Preview(showBackground = true)
@Composable
private fun MeasurementFieldInputPreview() {
    HealthLogTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MeasurementFieldInput(
                field = MeasurementType.WEIGHT.fields[0],
                text = "72.5",
                error = null,
                separator = '.',
                isLast = false,
                onTextChange = {},
                onFocused = {},
                onDone = {}
            )

            MeasurementFieldInput(
                field = MeasurementType.HEIGHT.fields[0],
                text = "18",
                error = ValidationError.OutOfRange(30.0, 250.0),
                separator = '.',
                isLast = true,
                onTextChange = {},
                onFocused = {},
                onDone = {}
            )
        }
    }
}
