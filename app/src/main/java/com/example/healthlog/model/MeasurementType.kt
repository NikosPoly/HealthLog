package com.example.healthlog.model

// One input value of a measurement, e.g. "Συστολική (mmHg)".
data class MeasurementField(
    val label: String,
    val unit: String,
    val allowsDecimals: Boolean
)

// The kinds of measurement the app records, each with the values it is made of.
// The constant name (e.g. BLOOD_PRESSURE) is what the database stores, so renaming
// or deleting a constant needs a database migration.
// The fields are stored in order in value, secondaryValue and tertiaryValue,
// so a type can have at most three: don't reorder fields or remove one in the middle.
// Labels, units and allowsDecimals are only in the code and can be changed freely.
enum class MeasurementType(
    val label: String,
    val fields: List<MeasurementField>
) {
    BLOOD_PRESSURE(
        "Αρτηριακή πίεση",
        listOf(
            MeasurementField("Συστολική", "mmHg", allowsDecimals = false),
            MeasurementField("Διαστολική", "mmHg", allowsDecimals = false),
            MeasurementField("Παλμοί", "bpm", allowsDecimals = false)
        )
    ),
    PULSE(
        "Παλμοί",
        listOf(MeasurementField("Παλμοί", "bpm", allowsDecimals = false))
    ),
    BLOOD_SUGAR(
        "Σάκχαρο",
        listOf(MeasurementField("Σάκχαρο", "mg/dL", allowsDecimals = false))
    ),
    WEIGHT(
        "Βάρος",
        listOf(MeasurementField("Βάρος", "kg", allowsDecimals = true))
    ),
    HEIGHT(
        "Ύψος",
        listOf(MeasurementField("Ύψος", "cm", allowsDecimals = false))
    )
}
