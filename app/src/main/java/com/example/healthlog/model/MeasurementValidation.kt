package com.example.healthlog.model

// What can be wrong with a typed value.
// Only says *what* is wrong; turning it into a message is the UI's job.
sealed interface ValidationError {
    // The field is empty.
    data object Required : ValidationError

    // The value ends with a dot, e.g. "72."
    data object IncompleteDecimal : ValidationError

    // The text is not a number. The input filter should prevent this.
    data object InvalidNumber : ValidationError

    // The value is outside the field's range (both limits included).
    data class OutOfRange(val min: Double, val max: Double) : ValidationError

    // Blood pressure: the diastolic value must be lower than the systolic.
    data object DiastolicNotBelowSystolic : ValidationError
}

// Checks one field on its own. Returns null when the value is valid.
fun validateField(field: MeasurementField, text: String): ValidationError? {
    if (text.isEmpty()) return ValidationError.Required
    if (text.endsWith(".")) return ValidationError.IncompleteDecimal

    val number = text.toDoubleOrNull() ?: return ValidationError.InvalidNumber

    return if (number < field.minValue || number > field.maxValue) {
        ValidationError.OutOfRange(field.minValue, field.maxValue)
    } else {
        null
    }
}

// Checks all fields of a measurement type.
// Returns one entry per field, in the order of type.fields: null = valid.
// Each field is checked on its own first; the blood pressure comparison runs
// only when systolic and diastolic are both valid, and its error goes on the diastolic field.
fun validateMeasurement(type: MeasurementType, texts: List<String>): List<ValidationError?> {
    val errors = type.fields.mapIndexed { index, field ->
        validateField(field, texts[index])
    }.toMutableList()

    if (type == MeasurementType.BLOOD_PRESSURE && errors[0] == null && errors[1] == null) {
        val systolic = texts[0].toDouble()
        val diastolic = texts[1].toDouble()

        if (diastolic >= systolic) {
            errors[1] = ValidationError.DiastolicNotBelowSystolic
        }
    }

    return errors
}
