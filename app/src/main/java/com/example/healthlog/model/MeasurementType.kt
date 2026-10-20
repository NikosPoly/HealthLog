package com.example.healthlog.model

// The kinds of measurement the app records.
// The constant name (e.g. BLOOD_PRESSURE) is what the database stores, so renaming
// a constant needs a database migration. The label and unit are only for display
// and can be changed freely.
enum class MeasurementType(
    val label: String,
    val unit: String,
    val allowsDecimals: Boolean
) {
    BLOOD_PRESSURE("Αρτηριακή πίεση", "mmHg", false),
    PULSE("Παλμοί", "bpm", false),
    BLOOD_SUGAR("Σάκχαρο", "mg/dL", false),
    WEIGHT("Βάρος", "kg", true),
    HEIGHT("Ύψος", "cm", false)
}
