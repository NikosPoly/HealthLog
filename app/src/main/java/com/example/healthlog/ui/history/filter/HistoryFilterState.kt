package com.example.healthlog.ui.history.filter

import com.example.healthlog.database.Measurement
import com.example.healthlog.guidelines.GuidelineEvaluation
import com.example.healthlog.guidelines.evaluateMeasurement

// Represents the active filter criteria applied to the history list of a measurement type.
data class HistoryFilterState(
    val stages: Set<ClinicalStageFilter> = emptySet(),
    // Note: conditions (Commit 2) and date range (Commit 3) will be added here.
) {
    // True when at least one filtering constraint is active.
    val isActive: Boolean
        get() = stages.isNotEmpty()
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

// Checks whether an individual measurement satisfies the active filter criteria.
fun matchesFilter(
    measurement: Measurement,
    filter: HistoryFilterState,
    latestHeightCm: Double? = null,
): Boolean {
    if (filter.stages.isEmpty()) return true
    val evaluation = evaluateMeasurement(measurement, latestHeightCm) ?: return false

    return filter.stages.any { stageFilter ->
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
}
