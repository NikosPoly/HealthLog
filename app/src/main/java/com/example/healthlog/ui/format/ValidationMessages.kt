package com.example.healthlog.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.healthlog.R
import com.example.healthlog.model.ValidationError

// The message shown to the user for a validation error, from strings.xml.
@Composable
fun validationErrorMessage(error: ValidationError): String = when (error) {
    ValidationError.Required ->
        stringResource(R.string.error_required)

    ValidationError.IncompleteDecimal ->
        stringResource(R.string.error_incomplete_decimal)

    ValidationError.InvalidNumber ->
        stringResource(R.string.error_invalid_number)

    is ValidationError.OutOfRange ->
        stringResource(
            R.string.error_value_out_of_range,
            formatLimit(error.min, decimalSeparator()),
            formatLimit(error.max, decimalSeparator())
        )

    ValidationError.DiastolicNotBelowSystolic ->
        stringResource(R.string.error_diastolic_not_below_systolic)
}

// Range limits without ".0" (30, not 30.0); a limit with real decimals keeps them
// with the language's separator (1.5, or 1,5 in Greek).
// internal (not private) so that unit tests can check it.
internal fun formatLimit(value: Double, decimalSeparator: Char): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else value.toString().replace('.', decimalSeparator)
