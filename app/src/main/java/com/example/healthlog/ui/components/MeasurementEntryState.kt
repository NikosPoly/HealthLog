package com.example.healthlog.ui.components

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.ValidationError
import com.example.healthlog.model.validateMeasurement

// The state of the entry dialog: what is typed, which fields the user has left,
// and which field the user is in. Immutable: every action returns a new state.
// No Compose in here, so all the dialog's rules can be checked with plain unit tests.
data class MeasurementEntryState(
    val type: MeasurementType,

    // The typed text of each field, in the order of type.fields,
    // with the decimal separator of the current language.
    val texts: List<String>,

    // Which fields the user has already left: moved to another field, tapped elsewhere
    // in the dialog, or pressed Done. Some errors are only shown after that, so they
    // don't appear while still typing.
    val leftFields: List<Boolean>,

    // The field the user is in (null = none). Saved across rotation, so the field still
    // counts as "the one being edited" even though Android removes the focus on rotation.
    val focusedIndex: Int?
) {
    // The typed texts with "." as separator, e.g. "72,5" → "72.5". The validation and
    // the saved values always work with "."; only what is typed and shown uses the
    // language's separator. Both "," and "." are converted, so a text typed before
    // switching the language is still read correctly.
    val normalizedTexts: List<String> by lazy { texts.map { it.replace(',', '.') } }

    // One entry per field: null = valid.
    val errors: List<ValidationError?> by lazy { validateMeasurement(type, normalizedTexts) }

    // Save is possible only when every field passes every check.
    val isValid: Boolean get() = errors.all { it == null }

    // The error of a field, only if it should be shown right now (see isErrorVisible).
    fun visibleError(index: Int): ValidationError? =
        errors[index]?.takeIf { isErrorVisible(it, normalizedTexts[index], leftFields[index]) }

    // The user typed (an allowed) text into a field.
    fun withText(index: Int, text: String): MeasurementEntryState =
        copy(texts = texts.toMutableList().also { it[index] = text })

    // The user leaves the field they were in: from now on its errors are shown.
    // Only used for the user's own actions, never when focus is lost by itself
    // (e.g. on rotation), so rotating doesn't trigger errors.
    fun leaveFocusedField(): MeasurementEntryState {
        val index = focusedIndex ?: return this
        return copy(
            leftFields = if (leftFields[index]) leftFields
            else leftFields.toMutableList().also { it[index] = true },
            focusedIndex = null
        )
    }

    // A field gained focus. Moving here from another field leaves that one;
    // focusing the same field again (e.g. tapping back in after rotation)
    // doesn't count as leaving it.
    fun focusField(index: Int): MeasurementEntryState =
        if (focusedIndex == index) this
        else leaveFocusedField().copy(focusedIndex = index)

    // The language changed while the dialog is open: convert what is already typed
    // to the new separator (e.g. "82.56" → "82,56"), so it can still be edited.
    fun withSeparator(separator: Char): MeasurementEntryState =
        copy(texts = texts.map { text -> text.replace(',', separator).replace('.', separator) })

    // The measurement to save, or null while any field is invalid.
    // The values are stored in order: value, secondaryValue, tertiaryValue.
    fun toMeasurement(): Measurement? {
        if (!isValid) return null

        val values = normalizedTexts.map { it.toDouble() }

        return Measurement(
            type = type,
            value = values[0],
            secondaryValue = values.getOrNull(1),
            tertiaryValue = values.getOrNull(2)
        )
    }

    companion object {
        // The state when the dialog opens: all fields empty, none left, none focused.
        fun initial(type: MeasurementType): MeasurementEntryState =
            MeasurementEntryState(
                type = type,
                texts = List(type.fields.size) { "" },
                leftFields = List(type.fields.size) { false },
                focusedIndex = null
            )
    }
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
