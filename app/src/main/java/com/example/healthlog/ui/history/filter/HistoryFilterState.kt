package com.example.healthlog.ui.history.filter

import com.example.healthlog.database.Measurement
import com.example.healthlog.guidelines.GuidelineEvaluation
import com.example.healthlog.guidelines.evaluateMeasurement
import com.example.healthlog.model.BodyPosition
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementArm

// Represents the active filter criteria applied to the history list of a measurement type.
data class HistoryFilterState(
    val stages: Set<ClinicalStageFilter> = emptySet(),
    val conditions: Set<MeasurementConditionFilter> = emptySet(),
    // Note: date range (Commit 3) will be added here.
) {
    // True when at least one filtering constraint is active.
    val isActive: Boolean
        get() = stages.isNotEmpty() || conditions.isNotEmpty()
}

// Pure function that filters a list of measurements according to the given filter criteria.
fun filterMeasurements(
    measurements: List<Measurement>,
    filter: HistoryFilterState,
    latestHeightCm: Double? = null,
): List<Measurement> {
    if (!filter.isActive) return measurements
    return measurements.filter { measurement ->
        matchesFilter(measurement, filter, latestHeightCm)
    }
}

// Checks whether an individual measurement satisfies all active filter criteria.
fun matchesFilter(
    measurement: Measurement,
    filter: HistoryFilterState,
    latestHeightCm: Double? = null,
): Boolean {
    // 1. Clinical stages check (OR among selected stages)
    if (filter.stages.isNotEmpty()) {
        val evaluation = evaluateMeasurement(measurement, latestHeightCm) ?: return false
        val matchesAnyStage = filter.stages.any { stageFilter ->
            when (stageFilter) {
                is ClinicalStageFilter.BloodPressure -> {
                    (evaluation is GuidelineEvaluation.BloodPressure) && (evaluation.stage == stageFilter.stage)
                }
                is ClinicalStageFilter.Glucose -> {
                    (evaluation is GuidelineEvaluation.BloodGlucose) && (evaluation.category == stageFilter.category)
                }
                is ClinicalStageFilter.Pulse -> {
                    (evaluation is GuidelineEvaluation.Pulse) && (evaluation.zone == stageFilter.zone)
                }
                is ClinicalStageFilter.Weight -> {
                    (evaluation is GuidelineEvaluation.Weight) && (evaluation.bmiEvaluation?.category == stageFilter.category)
                }
            }
        }
        if (!matchesAnyStage) return false
    }

    // 2. Arm condition check (OR among selected arms)
    val selectedArms = filter.conditions.filterIsInstance<MeasurementConditionFilter.Arm>().map { it.arm }
    if (selectedArms.isNotEmpty()) {
        val arm = MeasurementArm.fromNameOrNull(measurement.arm)
        if (arm !in selectedArms) return false
    }

    // 3. Body position condition check (OR among selected positions)
    val selectedPositions = filter.conditions.filterIsInstance<MeasurementConditionFilter.Position>().map { it.position }
    if (selectedPositions.isNotEmpty()) {
        val position = BodyPosition.fromNameOrNull(measurement.bodyPosition)
        if (position !in selectedPositions) return false
    }

    // 4. Meal context check (OR among selected meal contexts)
    val selectedMeals = filter.conditions.filterIsInstance<MeasurementConditionFilter.Meal>().map { it.meal }
    if (selectedMeals.isNotEmpty()) {
        val meal = MealContext.fromNameOrNull(measurement.mealContext)
        if (meal !in selectedMeals) return false
    }

    // 5. Arrhythmia check
    val requiresArrhythmia = filter.conditions.contains(MeasurementConditionFilter.Arrhythmia)
    if (requiresArrhythmia && (measurement.hasArrhythmia != true)) {
        return false
    }

    return true
}
