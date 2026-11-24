package com.example.healthlog.ui.calendar

import androidx.compose.ui.graphics.Color
import com.example.healthlog.database.Measurement
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.GuidelineEvaluation
import com.example.healthlog.guidelines.PulseZone
import com.example.healthlog.guidelines.evaluateMeasurement
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.model.SymptomSeverity
import java.time.LocalDate

// Fixed calendar category dot colors matching the calendar legend.
val CalendarVitalDotColor = Color(0xFF387A40)      // Green (Vitals)
val CalendarSymptomDotColor = Color(0xFFFFB300)    // Orange/Amber (Symptoms)
val CalendarMedicationDotColor = Color(0xFF1E88E5) // Blue (Medications)

// Clinical severity rank for vital sign measurements (higher = more severe).
enum class VitalClinicalRank(val priority: Int, val dotColor: Color) {
    CRISIS(priority = 5, dotColor = Color(0xFFC2185B)),       // Deep Pink/Crimson
    ALERT_RED(priority = 4, dotColor = Color(0xFFD32F2F)),    // Red (Grade 2, Diabetes, Obese)
    ALERT_ORANGE(priority = 3, dotColor = Color(0xFFFF8F00)), // Orange/Amber (Grade 1, Prediabetes, Overweight)
    ALERT_BLUE(priority = 2, dotColor = Color(0xFF0288D1)),   // Blue (Low BP, Hypoglycemia, Bradycardia, Underweight)
    NORMAL(priority = 1, dotColor = Color(0xFF387A40))        // Green (Normal/Optimal)
}

// Summary of all health events for a single calendar day.
data class CalendarDaySummary(
    val date: LocalDate,
    val measurements: List<Measurement> = emptyList(),
    val doseLogs: List<MedicationDoseLog> = emptyList(),
    val scheduledDoses: List<ScheduledDoseItem> = emptyList(),
    val symptoms: List<SymptomEntry> = emptyList(),
    val highestVitalRank: VitalClinicalRank? = null,
    val maxSymptomSeverity: SymptomSeverity? = null
) {
    val hasMeasurements: Boolean get() = measurements.isNotEmpty()
    val hasMedications: Boolean get() = scheduledDoses.isNotEmpty() || doseLogs.isNotEmpty()
    val hasSymptoms: Boolean get() = symptoms.isNotEmpty()
    val hasAnyData: Boolean get() = hasMeasurements || hasMedications || hasSymptoms

    // Category indicator dot colors matching the calendar legend (Green = Vitals, Orange = Symptoms, Blue = Meds).
    val vitalDotColor: Color? get() = if (hasMeasurements) CalendarVitalDotColor else null
    val symptomDotColor: Color? get() = if (hasSymptoms) CalendarSymptomDotColor else null
    val medicationDotColor: Color? get() = if (hasMedications) CalendarMedicationDotColor else null
}

// Maps an evaluation to a clinical rank.
fun GuidelineEvaluation.toClinicalRank(): VitalClinicalRank {
    return when (this) {
        is GuidelineEvaluation.BloodPressure -> when (stage) {
            BloodPressureStage.HYPERTENSIVE_CRISIS -> VitalClinicalRank.CRISIS
            BloodPressureStage.GRADE_2_HYPERTENSION -> VitalClinicalRank.ALERT_RED
            BloodPressureStage.GRADE_1_HYPERTENSION,
            BloodPressureStage.HIGH_NORMAL -> VitalClinicalRank.ALERT_ORANGE
            BloodPressureStage.LOW -> VitalClinicalRank.ALERT_BLUE
            BloodPressureStage.NORMAL -> VitalClinicalRank.NORMAL
        }
        is GuidelineEvaluation.BloodGlucose -> when (category) {
            BloodGlucoseCategory.DIABETES -> VitalClinicalRank.ALERT_RED
            BloodGlucoseCategory.PREDIABETES -> VitalClinicalRank.ALERT_ORANGE
            BloodGlucoseCategory.HYPOGLYCEMIA -> VitalClinicalRank.ALERT_BLUE
            BloodGlucoseCategory.NORMAL -> VitalClinicalRank.NORMAL
        }
        is GuidelineEvaluation.Pulse -> when (zone) {
            PulseZone.TACHYCARDIA -> VitalClinicalRank.ALERT_RED
            PulseZone.BRADYCARDIA -> VitalClinicalRank.ALERT_BLUE
            PulseZone.NORMAL -> VitalClinicalRank.NORMAL
        }
        is GuidelineEvaluation.Weight -> when (bmiEvaluation?.category) {
            BmiCategory.OBESE -> VitalClinicalRank.ALERT_RED
            BmiCategory.OVERWEIGHT -> VitalClinicalRank.ALERT_ORANGE
            BmiCategory.UNDERWEIGHT -> VitalClinicalRank.ALERT_BLUE
            BmiCategory.NORMAL -> VitalClinicalRank.NORMAL
            null -> VitalClinicalRank.NORMAL
        }
        is GuidelineEvaluation.Height -> VitalClinicalRank.NORMAL
    }
}

// Pure function to evaluate the highest vital clinical rank for a list of measurements.
fun calculateHighestVitalRank(
    measurements: List<Measurement>,
    latestHeightCm: Double? = null
): VitalClinicalRank? {
    if (measurements.isEmpty()) return null
    return measurements.mapNotNull { measurement ->
        evaluateMeasurement(measurement, latestHeightCm)?.toClinicalRank()
    }.maxByOrNull { it.priority }
}
