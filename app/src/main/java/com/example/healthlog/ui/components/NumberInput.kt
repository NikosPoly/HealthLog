package com.example.healthlog.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

// What can be typed in a field. Anything else typed or pasted is ignored,
// so it never appears in the field:
// - digits only, at most 6 before the decimal separator
// - no leading zero, and the value can't start with the separator
// - with maxDecimals > 0: one separator ("," in Greek, "." in English) followed by
//   at most maxDecimals digits (e.g. "72,55" / "72.55")
// "72," / "72." is allowed while typing; the validation reports it if the field is left like that.
internal fun isAllowedInput(text: String, maxDecimals: Int, separator: Char): Boolean {
    if (text.isEmpty()) return true

    val wholePart = "[1-9][0-9]{0,5}"
    val pattern = if (maxDecimals > 0) {
        Regex("$wholePart(${Regex.escape(separator.toString())}[0-9]{0,$maxDecimals})?")
    } else {
        Regex(wholePart)
    }
    return pattern.matches(text)
}

// Number pad for whole numbers, number pad with a decimal separator for decimals.
// The action key moves to the next field ("Next"), and closes the keyboard
// on the last field ("Done"), so the Save button becomes visible again.
internal fun numberKeyboard(allowDecimals: Boolean, isLast: Boolean) = KeyboardOptions(
    keyboardType = if (allowDecimals) KeyboardType.Decimal else KeyboardType.Number,
    imeAction = if (isLast) ImeAction.Done else ImeAction.Next
)
