package com.example.healthlog.ui.details.references

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.BmiEvaluation
import com.example.healthlog.guidelines.GuidelineEvaluation
import com.example.healthlog.guidelines.PulseZone
import com.example.healthlog.ui.theme.HealthLogTheme

// Expandable clinical reference card displaying standard organization guidelines,
// a visual gauge scale, and where the current measurement sits.
@Composable
fun ReferenceCard(
    evaluation: GuidelineEvaluation,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(value = true) }

    val sourceBadge = when (evaluation) {
        is GuidelineEvaluation.BloodPressure -> stringResource(R.string.guidelines_source_ish)
        is GuidelineEvaluation.BloodGlucose -> stringResource(R.string.guidelines_source_who_idf)
        is GuidelineEvaluation.Pulse -> stringResource(R.string.guidelines_source_whf)
        is GuidelineEvaluation.Weight -> stringResource(R.string.guidelines_source_who_oxford)
        is GuidelineEvaluation.Height -> stringResource(R.string.guidelines_source_who)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Header with expand toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.guidelines_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = sourceBadge,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (expanded) 90f else -90f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    when (evaluation) {
                        is GuidelineEvaluation.BloodPressure -> BloodPressureSection(evaluation = evaluation)
                        is GuidelineEvaluation.BloodGlucose -> BloodGlucoseSection(evaluation = evaluation)
                        is GuidelineEvaluation.Pulse -> PulseSection(evaluation = evaluation)
                        is GuidelineEvaluation.Weight -> WeightSection(bmiEvaluation = evaluation.bmiEvaluation)
                        is GuidelineEvaluation.Height -> HeightSection(evaluation = evaluation)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReferenceCardBpPreview() {
    HealthLogTheme {
        ReferenceCard(
            evaluation = GuidelineEvaluation.BloodPressure(
                stage = BloodPressureStage.GRADE_1_HYPERTENSION,
                systolic = 145.0,
                diastolic = 92.0,
                pulseEvaluation = GuidelineEvaluation.Pulse(
                    zone = PulseZone.NORMAL,
                    rate = 74.0
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReferenceCardWeightPreview() {
    HealthLogTheme {
        ReferenceCard(
            evaluation = GuidelineEvaluation.Weight(
                weightKg = 86.5,
                bmiEvaluation = BmiEvaluation(
                    bmi = 27.2,
                    newBmi = 26.6,
                    category = BmiCategory.OVERWEIGHT,
                    heightCm = 178.0,
                    weightKg = 86.5,
                    healthyWeightMinKg = 58.6,
                    healthyWeightMaxKg = 78.9,
                    weightDeltaKg = 7.6
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
