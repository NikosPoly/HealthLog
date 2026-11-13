package com.example.healthlog.ui.details.references

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.guidelines.GuidelineEvaluation
import com.example.healthlog.ui.format.decimalSeparator
import java.util.Locale

// Prominent, color-tinted clinical stage badge displayed directly below the reading.
@Composable
fun StageBadge(
    evaluation: GuidelineEvaluation,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (evaluation) {
            is GuidelineEvaluation.BloodPressure -> {
                StageChip(
                    label = stringResource(evaluation.stage.labelRes),
                    colors = bloodPressureStageColor(evaluation.stage)
                )
                evaluation.pulseEvaluation?.let { pulse ->
                    StageChip(
                        label = stringResource(R.string.type_pulse) + ": " +
                            stringResource(pulse.zone.labelRes),
                        colors = pulseZoneColor(pulse.zone)
                    )
                }
            }
            is GuidelineEvaluation.BloodGlucose -> {
                StageChip(
                    label = stringResource(evaluation.category.labelRes),
                    colors = bloodGlucoseCategoryColor(evaluation.category)
                )
            }
            is GuidelineEvaluation.Pulse -> {
                StageChip(
                    label = stringResource(evaluation.zone.labelRes),
                    colors = pulseZoneColor(evaluation.zone)
                )
            }
            is GuidelineEvaluation.Weight -> {
                val bmiEval = evaluation.bmiEvaluation
                if (bmiEval != null) {
                    val separator = decimalSeparator()
                    val formattedBmi = String.format(Locale.US, "%.1f", bmiEval.bmi).replace('.', separator)
                    StageChip(
                        label = stringResource(bmiEval.category.labelRes) + " · " +
                            stringResource(R.string.bmi_value_label, formattedBmi),
                        colors = bmiCategoryColor(bmiEval.category)
                    )
                }
            }
            is GuidelineEvaluation.Height -> {
                val separator = decimalSeparator()
                val minStr = String.format(Locale.US, "%.1f", evaluation.healthyWeightMinKg).replace('.', separator)
                val maxStr = String.format(Locale.US, "%.1f", evaluation.healthyWeightMaxKg).replace('.', separator)
                StageChip(
                    label = stringResource(R.string.height_healthy_weight_range, minStr, maxStr),
                    colors = StageColor(
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                        indicator = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

// Single tinted chip with colored indicator dot and bold label.
@Composable
fun StageChip(
    label: String,
    colors: StageColor,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(color = colors.container, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color = colors.indicator, shape = CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = colors.content
        )
    }
}

// Table expansion toggle button.
@Composable
fun TableToggle(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.guidelines_card_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (isExpanded) "▲" else "▼",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// Single row in a reference table, highlighted if it matches current measurement.
@Composable
fun ReferenceRow(
    title: String,
    criteria: String,
    colors: StageColor,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isCurrent) colors.container else MaterialTheme.colorScheme.surfaceContainer
    val textColor = if (isCurrent) colors.content else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = colors.indicator, shape = CircleShape)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (isCurrent) {
                Text(
                    text = "• " + stringResource(R.string.guidelines_current_reading),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.indicator
                )
            }
        }

        Text(
            text = criteria,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCurrent) textColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
