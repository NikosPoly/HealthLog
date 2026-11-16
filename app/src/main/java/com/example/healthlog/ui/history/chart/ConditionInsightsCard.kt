package com.example.healthlog.ui.history.chart

import androidx.annotation.DrawableRes


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.guidelines.protocols.LeftSpeechBubbleShape
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.arrhythmiaIcon
import com.example.healthlog.ui.history.chart.stats.ArrhythmiaStats
import com.example.healthlog.ui.history.chart.stats.ClinicalAdviceNote
import com.example.healthlog.ui.history.chart.stats.ClinicalAdviceType
import com.example.healthlog.ui.history.chart.stats.ConditionDistribution
import com.example.healthlog.ui.history.chart.stats.periodConditionStats

// Displays condition breakdown statistics (arm, posture, meal context, arrhythmia)
// over the selected period in a card with the same style as PeriodCard.
@Composable
fun ConditionInsightsCard(
    type: MeasurementType,
    inPeriod: List<Measurement>,
    modifier: Modifier = Modifier
) {
    val breakdown = remember(inPeriod, type) {
        periodConditionStats(inPeriod, type)
    }

    if (breakdown.isEmpty) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_brand_logo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.insights_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (breakdown.armDistribution.isNotEmpty()) {
                ConditionSection(
                    title = stringResource(R.string.section_arm),
                    items = breakdown.armDistribution,
                    stringRes = { it.label },
                    iconRes = { it.iconRes },
                    advice = breakdown.armAdvice
                )
            }

            if (breakdown.positionDistribution.isNotEmpty()) {
                ConditionSection(
                    title = stringResource(R.string.section_position),
                    items = breakdown.positionDistribution,
                    stringRes = { it.label },
                    iconRes = { it.iconRes },
                    advice = breakdown.positionAdvice
                )
            }

            if (breakdown.mealDistribution.isNotEmpty()) {
                ConditionSection(
                    title = stringResource(R.string.section_meal_context),
                    items = breakdown.mealDistribution,
                    stringRes = { it.label },
                    iconRes = { it.iconRes },
                    advice = breakdown.mealAdvice
                )
            }

            breakdown.arrhythmiaStats?.let { arrhythmia ->
                ArrhythmiaInsightSection(
                    stats = arrhythmia,
                    advice = breakdown.arrhythmiaAdvice
                )
            }
        }
    }
}

@Composable
private fun <T> ConditionSection(
    title: String,
    items: List<ConditionDistribution<T>>,
    stringRes: (T) -> Int,
    iconRes: (T) -> Int,
    advice: ClinicalAdviceNote? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        items.forEach { item ->
            DistributionRow(
                iconRes = iconRes(item.value),
                label = stringResource(stringRes(item.value)),
                count = item.count,
                total = item.total,
                percentage = item.percentage,
                ratio = item.ratio
            )
        }

        advice?.let {
            ClinicalAdviceBox(it)
        }
    }
}

@Composable
private fun DistributionRow(
    @DrawableRes iconRes: Int,
    label: String,
    count: Int,
    total: Int,
    percentage: Int,
    ratio: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.Unspecified
            )
            Text(
                text = " $label",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$percentage% ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "($count/$total)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerLow,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun ArrhythmiaInsightSection(
    stats: ArrhythmiaStats,
    advice: ClinicalAdviceNote? = null
) {
    val hasDetected = stats.detectedCount > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(arrhythmiaIcon(hasDetected)),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(R.string.section_arrhythmia),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = if (hasDetected) {
                stringResource(
                    R.string.insights_arrhythmia_detected,
                    stats.detectedCount,
                    stats.totalRecorded,
                    stats.percentage
                )
            } else {
                stringResource(R.string.insights_arrhythmia_none, stats.totalRecorded)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (hasDetected) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        advice?.let {
            ClinicalAdviceBox(it)
        }
    }
}

@Composable
private fun ClinicalAdviceBox(advice: ClinicalAdviceNote) {
    val isWarning = advice.type == ClinicalAdviceType.WARNING
    val containerColor = if (isWarning) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (isWarning) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Larger Doctor Avatar
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 2.dp,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(48.dp)
        ) {
            Image(
                painter = painterResource(advice.iconRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Talking Speech Cloud Bubble pointing at the doctor
        Surface(
            shape = LeftSpeechBubbleShape,
            color = containerColor,
            shadowElevation = 1.dp,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(advice.textRes),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                lineHeight = 18.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
            )
        }
    }
}
