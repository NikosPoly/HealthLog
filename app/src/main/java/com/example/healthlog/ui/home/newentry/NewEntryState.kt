package com.example.healthlog.ui.home.newentry

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.BodyPosition
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementArm
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.ValidationError
import com.example.healthlog.model.validateMeasurement

// The state of the entry dialog: what is typed, which fields the user has left,
// which field the user is in, and any selected measurement context or custom timestamp.
// Immutable: every action returns a new state.
// No Compose in here, so all the dialog's rules can be checked with plain unit tests.
data class NewEntryState(
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
    val focusedIndex: Int?,

    // Context options (unselected by default; required to save for the relevant types).
    val hasArrhythmia: Boolean? = null,
    val arm: MeasurementArm? = null,
    val bodyPosition: BodyPosition? = null,
    val mealContext: MealContext? = null,

    // Whether to record the current date and time (checked by default).
    val useCurrentDateTime: Boolean = true,

    // Custom date/time in milliseconds, used when useCurrentDateTime is false.
    val customTimestamp: Long = System.currentTimeMillis()
) {
    // The typed texts with "." as separator, e.g. "72,5" → "72.5". The validation and
    // the saved values always work with "."; only what is typed and shown uses the
    // language's separator. Both "," and "." are converted, so a text typed before
    // switching the language is still read correctly.
    val normalizedTexts: List<String> by lazy { texts.map { it.replace(',', '.') } }

    // One result per field: its error, or null = valid.
    val errors: List<ValidationError?> by lazy { validateMeasurement(type, normalizedTexts) }

    // Whether all required context selections have been made for this measurement type.
    val isOptionsValid: Boolean
        get() = when (type) {
            MeasurementType.BLOOD_PRESSURE ->
                hasArrhythmia != null && arm != null && bodyPosition != null
            MeasurementType.PULSE ->
                hasArrhythmia != null
            MeasurementType.BLOOD_SUGAR ->
                mealContext != null
            MeasurementType.WEIGHT,
            MeasurementType.HEIGHT -> true
        }

    // Save is possible only when every numeric field passes every check AND
    // all required option selections are made.
    val isValid: Boolean get() = errors.all { it == null } && isOptionsValid

    // The error of a field, only if it should be shown right now (see isErrorVisible).
    fun visibleError(index: Int): ValidationError? =
        errors[index]?.takeIf { isErrorVisible(it, normalizedTexts[index], leftFields[index]) }

    // The user typed (an allowed) text into a field.
    fun withText(index: Int, text: String): NewEntryState =
        copy(texts = texts.toMutableList().also { it[index] = text })

    // The user leaves the field they were in: from now on its errors are shown.
    // Only used for the user's own actions, never when focus is lost by itself
    // (e.g. on rotation), so rotating doesn't trigger errors.
    fun leaveFocusedField(): NewEntryState {
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
    fun focusField(index: Int): NewEntryState =
        if (focusedIndex == index) this
        else leaveFocusedField().copy(focusedIndex = index)

    // Option selectors
    fun withArrhythmia(hasArrhythmia: Boolean): NewEntryState =
        copy(hasArrhythmia = hasArrhythmia)

    fun withArm(arm: MeasurementArm): NewEntryState =
        copy(arm = arm)

    fun withBodyPosition(position: BodyPosition): NewEntryState =
        copy(bodyPosition = position)

    fun withMealContext(context: MealContext): NewEntryState =
        copy(mealContext = context)

    // When the user unticks "use current date and time", snapshot the current time (now)
    // as the starting point so the picker reflects the actual current moment.
    fun withUseCurrentDateTime(useCurrent: Boolean, now: Long = System.currentTimeMillis()): NewEntryState =
        if (!useCurrent) {
            copy(useCurrentDateTime = false, customTimestamp = now)
        } else {
            copy(useCurrentDateTime = true)
        }

    fun withCustomTimestamp(timestamp: Long): NewEntryState =
        copy(customTimestamp = timestamp)

    // The language changed while the dialog is open: convert what is already typed
    // to the new separator (e.g. "82.56" → "82,56"), so it can still be edited.
    fun withSeparator(separator: Char): NewEntryState =
        copy(texts = texts.map { text -> text.replace(',', separator).replace('.', separator) })

    // The measurement to save, or null while any field or required option is invalid.
    // The values are stored in order: value, secondaryValue, tertiaryValue.
    fun toMeasurement(): Measurement? {
        if (!isValid) return null

        val values = normalizedTexts.map { it.toDouble() }
        val timestamp = if (useCurrentDateTime) System.currentTimeMillis() else customTimestamp

        return Measurement(
            type = type,
            value = values[0],
            secondaryValue = values.getOrNull(1),
            tertiaryValue = values.getOrNull(2),
            hasArrhythmia = hasArrhythmia,
            mealContext = mealContext?.name,
            arm = arm?.name,
            bodyPosition = bodyPosition?.name,
            timestamp = timestamp
        )
    }

    companion object {
        // The state when the dialog opens: all fields empty, none left, none focused,
        // options unselected, current date/time enabled.
        fun initial(type: MeasurementType, now: Long = System.currentTimeMillis()): NewEntryState =
            NewEntryState(
                type = type,
                texts = List(type.fields.size) { "" },
                leftFields = List(type.fields.size) { false },
                focusedIndex = null,
                hasArrhythmia = null,
                arm = null,
                bodyPosition = null,
                mealContext = null,
                useCurrentDateTime = true,
                customTimestamp = now
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

