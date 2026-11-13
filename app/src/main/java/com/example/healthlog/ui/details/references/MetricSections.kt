package com.example.healthlog.ui.details.references

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.BmiEvaluation
import com.example.healthlog.guidelines.GuidelineEvaluation
import com.example.healthlog.guidelines.PulseZone
import com.example.healthlog.model.MealContext
import com.example.healthlog.ui.format.decimalSeparator
import java.util.Locale
import kotlin.math.abs

// Visual gauge and expandable table section for ISH 2020 Blood Pressure.
@Composable
fun BloodPressureSection(evaluation: GuidelineEvaluation.BloodPressure) {
    var showAllStages by rememberSaveable { mutableStateOf(value = false) }

    val stages = listOf(
        Pair(BloodPressureStage.LOW, R.string.bp_criteria_low),
        Pair(BloodPressureStage.NORMAL, R.string.bp_criteria_normal),
        Pair(BloodPressureStage.HIGH_NORMAL, R.string.bp_criteria_high_normal),
        Pair(BloodPressureStage.GRADE_1_HYPERTENSION, R.string.bp_criteria_grade_1),
        Pair(BloodPressureStage.GRADE_2_HYPERTENSION, R.string.bp_criteria_grade_2),
        Pair(BloodPressureStage.HYPERTENSIVE_CRISIS, R.string.bp_criteria_crisis),
    )

    val activeCriteria = stages.firstOrNull { it.first == evaluation.stage }?.second?.let {
        stringResource(it)
    }.orEmpty()

    val segments = stages.map { (stage, _) ->
        ScaleSegment(
            label = stringResource(stage.labelRes),
            color = bloodPressureStageColor(stage).indicator,
            isActive = stage == evaluation.stage
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SegmentedScale(segments = segments, activeCriteria = activeCriteria)

        if (evaluation.pulseEvaluation != null) {
            Spacer(modifier = Modifier.height(4.dp))
            PulseSection(evaluation = evaluation.pulseEvaluation)
        }

        TableToggle(
            isExpanded = showAllStages,
            onToggle = { showAllStages = !showAllStages }
        )

        AnimatedVisibility(visible = showAllStages) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                stages.forEach { (stage, criteriaRes) ->
                    ReferenceRow(
                        title = stringResource(stage.labelRes),
                        criteria = stringResource(criteriaRes),
                        colors = bloodPressureStageColor(stage),
                        isCurrent = stage == evaluation.stage
                    )
                }
            }
        }
    }
}

// Visual gauge and expandable table section for WHO / IDF Blood Glucose.
@Composable
fun BloodGlucoseSection(evaluation: GuidelineEvaluation.BloodGlucose) {
    var showAllStages by rememberSaveable { mutableStateOf(value = false) }

    val categories = when (evaluation.mealContext) {
        MealContext.BEFORE_MEAL -> listOf(
            Pair(BloodGlucoseCategory.HYPOGLYCEMIA, R.string.glucose_criteria_hypoglycemia),
            Pair(BloodGlucoseCategory.NORMAL, R.string.glucose_criteria_fasting_normal),
            Pair(BloodGlucoseCategory.PREDIABETES, R.string.glucose_criteria_fasting_prediabetes),
            Pair(BloodGlucoseCategory.DIABETES, R.string.glucose_criteria_fasting_diabetes)
        )
        MealContext.AFTER_MEAL_2H -> listOf(
            Pair(BloodGlucoseCategory.HYPOGLYCEMIA, R.string.glucose_criteria_hypoglycemia),
            Pair(BloodGlucoseCategory.NORMAL, R.string.glucose_criteria_post_normal),
            Pair(BloodGlucoseCategory.PREDIABETES, R.string.glucose_criteria_post_prediabetes),
            Pair(BloodGlucoseCategory.DIABETES, R.string.glucose_criteria_post_diabetes)
        )
        null -> listOf(
            Pair(BloodGlucoseCategory.HYPOGLYCEMIA, R.string.glucose_criteria_hypoglycemia),
            Pair(BloodGlucoseCategory.NORMAL, R.string.glucose_criteria_post_normal),
            Pair(BloodGlucoseCategory.PREDIABETES, R.string.glucose_criteria_post_prediabetes),
            Pair(BloodGlucoseCategory.DIABETES, R.string.glucose_criteria_post_diabetes)
        )
    }

    val activeCriteria = categories.firstOrNull { it.first == evaluation.category }?.second?.let {
        stringResource(it)
    }.orEmpty()

    val segments = categories.map { (cat, _) ->
        ScaleSegment(
            label = stringResource(cat.labelRes),
            color = bloodGlucoseCategoryColor(cat).indicator,
            isActive = cat == evaluation.category
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedScale(segments = segments, activeCriteria = activeCriteria)

        TableToggle(
            isExpanded = showAllStages,
            onToggle = { showAllStages = !showAllStages }
        )

        AnimatedVisibility(visible = showAllStages) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEach { (cat, criteriaRes) ->
                    ReferenceRow(
                        title = stringResource(cat.labelRes),
                        criteria = stringResource(criteriaRes),
                        colors = bloodGlucoseCategoryColor(cat),
                        isCurrent = cat == evaluation.category
                    )
                }
            }
        }
    }
}

// Visual gauge and expandable table section for WHF / WHO Pulse.
@Composable
fun PulseSection(evaluation: GuidelineEvaluation.Pulse) {
    var showAllStages by rememberSaveable { mutableStateOf(value = false) }

    val zones = listOf(
        Pair(PulseZone.BRADYCARDIA, R.string.pulse_criteria_bradycardia),
        Pair(PulseZone.NORMAL, R.string.pulse_criteria_normal),
        Pair(PulseZone.TACHYCARDIA, R.string.pulse_criteria_tachycardia)
    )

    val activeCriteria = zones.firstOrNull { it.first == evaluation.zone }?.second?.let {
        stringResource(it)
    }.orEmpty()

    val segments = zones.map { (zone, _) ->
        ScaleSegment(
            label = stringResource(zone.labelRes),
            color = pulseZoneColor(zone).indicator,
            isActive = zone == evaluation.zone
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedScale(segments = segments, activeCriteria = activeCriteria)

        Text(
            text = stringResource(R.string.pulse_note_athlete),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        TableToggle(
            isExpanded = showAllStages,
            onToggle = { showAllStages = !showAllStages }
        )

        AnimatedVisibility(visible = showAllStages) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                zones.forEach { (zone, criteriaRes) ->
                    ReferenceRow(
                        title = stringResource(zone.labelRes),
                        criteria = stringResource(criteriaRes),
                        colors = pulseZoneColor(zone),
                        isCurrent = zone == evaluation.zone
                    )
                }
            }
        }
    }
}

// Visual spectrum and insights section for Weight and BMI.
@Composable
fun WeightSection(bmiEvaluation: BmiEvaluation?) {
    if (bmiEvaluation == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(14.dp)
        ) {
            Text(
                text = stringResource(R.string.bmi_no_height_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var showAllStages by rememberSaveable { mutableStateOf(value = false) }

    val separator = decimalSeparator()
    val formattedBmi = String.format(Locale.US, "%.1f", bmiEvaluation.bmi).replace('.', separator)
    val formattedNewBmi = String.format(Locale.US, "%.1f", bmiEvaluation.newBmi).replace('.', separator)
    val heightStr = String.format(Locale.US, "%.0f", bmiEvaluation.heightCm)
    val minStr = String.format(Locale.US, "%.1f", bmiEvaluation.healthyWeightMinKg).replace('.', separator)
    val maxStr = String.format(Locale.US, "%.1f", bmiEvaluation.healthyWeightMaxKg).replace('.', separator)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BmiContinuousScale(
            bmi = bmiEvaluation.bmi,
            activeCategoryLabel = stringResource(bmiEvaluation.category.labelRes) + " · BMI $formattedBmi",
            activeCategoryColor = bmiCategoryColor(bmiEvaluation.category).indicator
        )

        // Target healthy weight window
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.bmi_healthy_range, heightStr, minStr, maxStr),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val deltaText = when {
                    bmiEvaluation.weightDeltaKg > 0.05 -> {
                        val deltaStr = String.format(Locale.US, "%.1f", bmiEvaluation.weightDeltaKg).replace('.', separator)
                        stringResource(R.string.bmi_delta_above, deltaStr)
                    }
                    bmiEvaluation.weightDeltaKg < -0.05 -> {
                        val deltaStr = String.format(Locale.US, "%.1f", abs(bmiEvaluation.weightDeltaKg)).replace('.', separator)
                        stringResource(R.string.bmi_delta_below, deltaStr)
                    }
                    else -> stringResource(R.string.bmi_in_range)
                }

                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bmiEvaluation.isHealthyWeight) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Standard BMI vs Oxford New BMI comparison
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.bmi_value_label, formattedBmi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.bmi_new_value_label, formattedNewBmi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TableToggle(
            isExpanded = showAllStages,
            onToggle = { showAllStages = !showAllStages }
        )

        AnimatedVisibility(visible = showAllStages) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val categories = listOf(
                    Triple(BmiCategory.UNDERWEIGHT, R.string.bmi_category_underweight, "< 18.5"),
                    Triple(BmiCategory.NORMAL, R.string.bmi_category_normal, "18.5–24.9"),
                    Triple(BmiCategory.OVERWEIGHT, R.string.bmi_category_overweight, "25.0–29.9"),
                    Triple(BmiCategory.OBESE, R.string.bmi_category_obese, "≥ 30.0")
                )

                categories.forEach { (cat, labelRes, criteriaText) ->
                    ReferenceRow(
                        title = stringResource(labelRes),
                        criteria = criteriaText,
                        colors = bmiCategoryColor(cat),
                        isCurrent = cat == bmiEvaluation.category
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.bmi_asian_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// Educational summary section for Height.
@Composable
fun HeightSection(evaluation: GuidelineEvaluation.Height) {
    val separator = decimalSeparator()
    val minStr = String.format(Locale.US, "%.1f", evaluation.healthyWeightMinKg).replace('.', separator)
    val maxStr = String.format(Locale.US, "%.1f", evaluation.healthyWeightMaxKg).replace('.', separator)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.height_healthy_weight_range, minStr, maxStr),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.height_bmi_baseline_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
