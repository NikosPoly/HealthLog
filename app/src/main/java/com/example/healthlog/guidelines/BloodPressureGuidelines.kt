package com.example.healthlog.guidelines

// Blood pressure stages according to the 2020 International Society of Hypertension (ISH)
// Global Practice Guidelines, endorsed by the World Health Organization (WHO).
enum class BloodPressureStage {
    LOW,                    // Systolic < 90 or Diastolic < 60 mmHg
    NORMAL,                 // Systolic < 130 and Diastolic < 85 mmHg
    HIGH_NORMAL,            // Systolic 130–139 or Diastolic 85–89 mmHg
    GRADE_1_HYPERTENSION,   // Systolic 140–159 or Diastolic 90–99 mmHg
    GRADE_2_HYPERTENSION,   // Systolic ≥ 160 or Diastolic ≥ 100 mmHg
    HYPERTENSIVE_CRISIS     // Systolic > 180 and/or Diastolic > 120 mmHg
}

// Evaluates systolic and diastolic blood pressure values into an ISH 2020 stage.
// Evaluated with priority to higher cardiovascular risk.
fun evaluateBloodPressure(systolic: Double, diastolic: Double): BloodPressureStage {
    return when {
        (systolic > 180.0) || (diastolic > 120.0) -> BloodPressureStage.HYPERTENSIVE_CRISIS
        (systolic >= 160.0) || (diastolic >= 100.0) -> BloodPressureStage.GRADE_2_HYPERTENSION
        (systolic >= 140.0) || (diastolic >= 90.0) -> BloodPressureStage.GRADE_1_HYPERTENSION
        (systolic >= 130.0) || (diastolic >= 85.0) -> BloodPressureStage.HIGH_NORMAL
        (systolic < 90.0) || (diastolic < 60.0) -> BloodPressureStage.LOW
        else -> BloodPressureStage.NORMAL
    }
}
