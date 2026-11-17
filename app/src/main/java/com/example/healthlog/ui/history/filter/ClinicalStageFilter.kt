package com.example.healthlog.ui.history.filter

import androidx.annotation.StringRes
import com.example.healthlog.R
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.PulseZone
import com.example.healthlog.model.MeasurementType

// Represents a clinical stage or zone that a user can filter by in the history list.
sealed interface ClinicalStageFilter {
    @get:StringRes
    val labelRes: Int

    // Blood pressure stages according to ISH 2020.
    data class BloodPressure(val stage: BloodPressureStage) : ClinicalStageFilter {
        override val labelRes: Int = when (stage) {
            BloodPressureStage.LOW -> R.string.bp_stage_low
            BloodPressureStage.NORMAL -> R.string.bp_stage_normal
            BloodPressureStage.HIGH_NORMAL -> R.string.bp_stage_high_normal
            BloodPressureStage.GRADE_1_HYPERTENSION -> R.string.bp_stage_grade_1
            BloodPressureStage.GRADE_2_HYPERTENSION -> R.string.bp_stage_grade_2
            BloodPressureStage.HYPERTENSIVE_CRISIS -> R.string.bp_stage_crisis
        }
    }

    // Blood glucose categories according to WHO / IDF.
    data class Glucose(val category: BloodGlucoseCategory) : ClinicalStageFilter {
        override val labelRes: Int = when (category) {
            BloodGlucoseCategory.HYPOGLYCEMIA -> R.string.glucose_category_hypoglycemia
            BloodGlucoseCategory.NORMAL -> R.string.glucose_category_normal
            BloodGlucoseCategory.PREDIABETES -> R.string.glucose_category_prediabetes
            BloodGlucoseCategory.DIABETES -> R.string.glucose_category_diabetes
        }
    }

    // Resting heart rate zones according to WHF / WHO.
    data class Pulse(val zone: PulseZone) : ClinicalStageFilter {
        override val labelRes: Int = when (zone) {
            PulseZone.BRADYCARDIA -> R.string.pulse_zone_bradycardia
            PulseZone.NORMAL -> R.string.pulse_zone_normal
            PulseZone.TACHYCARDIA -> R.string.pulse_zone_tachycardia
        }
    }

    // BMI categories according to WHO.
    data class Weight(val category: BmiCategory) : ClinicalStageFilter {
        override val labelRes: Int = when (category) {
            BmiCategory.UNDERWEIGHT -> R.string.bmi_category_underweight
            BmiCategory.NORMAL -> R.string.bmi_category_normal
            BmiCategory.OVERWEIGHT -> R.string.bmi_category_overweight
            BmiCategory.OBESE -> R.string.bmi_category_obese
        }
    }
}

// Returns the list of standard clinical stages available for filtering the given measurement type.
fun availableStagesForType(type: MeasurementType): List<ClinicalStageFilter> {
    return when (type) {
        MeasurementType.BLOOD_PRESSURE -> BloodPressureStage.entries.map { ClinicalStageFilter.BloodPressure(it) }
        MeasurementType.BLOOD_SUGAR -> BloodGlucoseCategory.entries.map { ClinicalStageFilter.Glucose(it) }
        MeasurementType.PULSE -> PulseZone.entries.map { ClinicalStageFilter.Pulse(it) }
        MeasurementType.WEIGHT -> BmiCategory.entries.map { ClinicalStageFilter.Weight(it) }
        MeasurementType.HEIGHT -> emptyList()
    }
}
