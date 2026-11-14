package com.example.healthlog.ui.details.references

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.healthlog.R
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.PulseZone

// Container and text colors for a clinical reference badge or table highlight.
data class StageColor(
    val container: Color,
    val content: Color,
    val indicator: Color = content,
)

// Resolves theme-aware colors for blood pressure stages (ISH 2020).
@Composable
fun bloodPressureStageColor(stage: BloodPressureStage): StageColor {
    val dark = isSystemInDarkTheme()
    return when (stage) {
        BloodPressureStage.LOW -> if (dark) {
            StageColor(Color(0xFF102838), Color(0xFF81D4FA), Color(0xFF29B6F6))
        } else {
            StageColor(Color(0xFFE1F5FE), Color(0xFF0277BD), Color(0xFF0288D1))
        }
        BloodPressureStage.NORMAL -> if (dark) {
            StageColor(Color(0xFF1B3B22), Color(0xFFA5D6A7), Color(0xFF4CAF50))
        } else {
            StageColor(Color(0xFFE8F5E9), Color(0xFF1B5E20), Color(0xFF2E7D32))
        }
        BloodPressureStage.HIGH_NORMAL -> if (dark) {
            StageColor(Color(0xFF3E2D12), Color(0xFFFFE082), Color(0xFFFFB300))
        } else {
            StageColor(Color(0xFFFFF8E1), Color(0xFFE65100), Color(0xFFF57F17))
        }
        BloodPressureStage.GRADE_1_HYPERTENSION -> if (dark) {
            StageColor(Color(0xFF3E1F18), Color(0xFFFFAB91), Color(0xFFFF7043))
        } else {
            StageColor(Color(0xFFFBE9E7), Color(0xFFBF360C), Color(0xFFE64A19))
        }
        BloodPressureStage.GRADE_2_HYPERTENSION -> if (dark) {
            StageColor(Color(0xFF4A121A), Color(0xFFEF9A9A), Color(0xFFE53935))
        } else {
            StageColor(Color(0xFFFFEBEE), Color(0xFFB71C1C), Color(0xFFD32F2F))
        }
        BloodPressureStage.HYPERTENSIVE_CRISIS -> if (dark) {
            StageColor(Color(0xFF5A0C16), Color(0xFFFF8A80), Color(0xFFFF1744))
        } else {
            StageColor(Color(0xFFFFCDD2), Color(0xFF880E4F), Color(0xFFC2185B))
        }
    }
}

// Resolves theme-aware colors for blood glucose categories (WHO / IDF).
@Composable
fun bloodGlucoseCategoryColor(category: BloodGlucoseCategory): StageColor {
    val dark = isSystemInDarkTheme()
    return when (category) {
        BloodGlucoseCategory.HYPOGLYCEMIA -> if (dark) {
            StageColor(Color(0xFF102838), Color(0xFF81D4FA), Color(0xFF0288D1))
        } else {
            StageColor(Color(0xFFE1F5FE), Color(0xFF0277BD), Color(0xFF0288D1))
        }
        BloodGlucoseCategory.NORMAL -> if (dark) {
            StageColor(Color(0xFF1B3B22), Color(0xFFA5D6A7), Color(0xFF4CAF50))
        } else {
            StageColor(Color(0xFFE8F5E9), Color(0xFF1B5E20), Color(0xFF2E7D32))
        }
        BloodGlucoseCategory.PREDIABETES -> if (dark) {
            StageColor(Color(0xFF3E2723), Color(0xFFFFCC80), Color(0xFFFB8C00))
        } else {
            StageColor(Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFFEF6C00))
        }
        BloodGlucoseCategory.DIABETES -> if (dark) {
            StageColor(Color(0xFF4A121A), Color(0xFFEF9A9A), Color(0xFFE53935))
        } else {
            StageColor(Color(0xFFFFEBEE), Color(0xFFB71C1C), Color(0xFFD32F2F))
        }
    }
}

// Resolves theme-aware colors for resting pulse zones (WHF / WHO).
@Composable
fun pulseZoneColor(zone: PulseZone): StageColor {
    val dark = isSystemInDarkTheme()
    return when (zone) {
        PulseZone.BRADYCARDIA -> if (dark) {
            StageColor(Color(0xFF102838), Color(0xFF81D4FA), Color(0xFF0288D1))
        } else {
            StageColor(Color(0xFFE1F5FE), Color(0xFF0277BD), Color(0xFF0288D1))
        }
        PulseZone.NORMAL -> if (dark) {
            StageColor(Color(0xFF1B3B22), Color(0xFFA5D6A7), Color(0xFF4CAF50))
        } else {
            StageColor(Color(0xFFE8F5E9), Color(0xFF1B5E20), Color(0xFF2E7D32))
        }
        PulseZone.TACHYCARDIA -> if (dark) {
            StageColor(Color(0xFF4A121A), Color(0xFFEF9A9A), Color(0xFFE53935))
        } else {
            StageColor(Color(0xFFFFEBEE), Color(0xFFB71C1C), Color(0xFFD32F2F))
        }
    }
}

// Resolves theme-aware colors for WHO BMI categories.
@Composable
fun bmiCategoryColor(category: BmiCategory): StageColor {
    val dark = isSystemInDarkTheme()
    return when (category) {
        BmiCategory.UNDERWEIGHT -> if (dark) {
            StageColor(Color(0xFF102838), Color(0xFF81D4FA), Color(0xFF0288D1))
        } else {
            StageColor(Color(0xFFE1F5FE), Color(0xFF0277BD), Color(0xFF0288D1))
        }
        BmiCategory.NORMAL -> if (dark) {
            StageColor(Color(0xFF1B3B22), Color(0xFFA5D6A7), Color(0xFF4CAF50))
        } else {
            StageColor(Color(0xFFE8F5E9), Color(0xFF1B5E20), Color(0xFF2E7D32))
        }
        BmiCategory.OVERWEIGHT -> if (dark) {
            StageColor(Color(0xFF3E2723), Color(0xFFFFCC80), Color(0xFFFB8C00))
        } else {
            StageColor(Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFFEF6C00))
        }
        BmiCategory.OBESE -> if (dark) {
            StageColor(Color(0xFF4A121A), Color(0xFFEF9A9A), Color(0xFFE53935))
        } else {
            StageColor(Color(0xFFFFEBEE), Color(0xFFB71C1C), Color(0xFFD32F2F))
        }
    }
}

// String resource mappings for clinical stages.
val BloodPressureStage.labelRes: Int
    get() = when (this) {
        BloodPressureStage.LOW -> R.string.bp_stage_low
        BloodPressureStage.NORMAL -> R.string.bp_stage_normal
        BloodPressureStage.HIGH_NORMAL -> R.string.bp_stage_high_normal
        BloodPressureStage.GRADE_1_HYPERTENSION -> R.string.bp_stage_grade_1
        BloodPressureStage.GRADE_2_HYPERTENSION -> R.string.bp_stage_grade_2
        BloodPressureStage.HYPERTENSIVE_CRISIS -> R.string.bp_stage_crisis
    }

val BloodGlucoseCategory.labelRes: Int
    get() = when (this) {
        BloodGlucoseCategory.HYPOGLYCEMIA -> R.string.glucose_category_hypoglycemia
        BloodGlucoseCategory.NORMAL -> R.string.glucose_category_normal
        BloodGlucoseCategory.PREDIABETES -> R.string.glucose_category_prediabetes
        BloodGlucoseCategory.DIABETES -> R.string.glucose_category_diabetes
    }

val PulseZone.labelRes: Int
    get() = when (this) {
        PulseZone.BRADYCARDIA -> R.string.pulse_zone_bradycardia
        PulseZone.NORMAL -> R.string.pulse_zone_normal
        PulseZone.TACHYCARDIA -> R.string.pulse_zone_tachycardia
    }

val BmiCategory.labelRes: Int
    get() = when (this) {
        BmiCategory.UNDERWEIGHT -> R.string.bmi_category_underweight
        BmiCategory.NORMAL -> R.string.bmi_category_normal
        BmiCategory.OVERWEIGHT -> R.string.bmi_category_overweight
        BmiCategory.OBESE -> R.string.bmi_category_obese
    }
