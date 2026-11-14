package com.example.healthlog.ui.details.references

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// One segment in a clinical gauge bar.
data class ScaleSegment(
    val label: String,
    val color: Color,
    val isActive: Boolean,
    val weight: Float = 1f,
)

// A segmented visual gauge bar where the current stage is highlighted.
@Composable
fun SegmentedScale(
    segments: List<ScaleSegment>,
    activeCriteria: String,
    modifier: Modifier = Modifier,
) {
    val activeSegment = segments.firstOrNull { it.isActive }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            segments.forEach { segment ->
                val alpha = if (segment.isActive) 1f else 0.28f
                Box(
                    modifier = Modifier
                        .weight(segment.weight)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(segment.color.copy(alpha = alpha))
                )
            }
        }

        activeSegment?.let { active ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = active.color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color = active.color, shape = CircleShape)
                    )
                    Text(
                        text = active.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = activeCriteria,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Continuous spectrum gauge for Body Mass Index (BMI).
@Composable
fun BmiContinuousScale(
    bmi: Double,
    activeCategoryLabel: String,
    activeCategoryColor: Color,
    modifier: Modifier = Modifier,
) {
    val minBmi = 15.0
    val maxBmi = 35.0
    val clampedBmi = bmi.coerceIn(minBmi, maxBmi)
    val fraction = ((clampedBmi - minBmi) / (maxBmi - minBmi)).toFloat()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            val totalWidth = maxWidth

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(3.5f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp))
                            .background(Color(0xFF0288D1).copy(alpha = if (bmi < 18.5) 1f else 0.3f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(6.5f)
                            .height(14.dp)
                            .background(Color(0xFF2E7D32).copy(alpha = if ((bmi >= 18.5) && (bmi < 25.0)) 1f else 0.3f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(5f)
                            .height(14.dp)
                            .background(Color(0xFFEF6C00).copy(alpha = if ((bmi >= 25.0) && (bmi < 30.0)) 1f else 0.3f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(5f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp))
                            .background(Color(0xFFD32F2F).copy(alpha = if (bmi >= 30.0) 1f else 0.3f))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "18.5",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset(x = (totalWidth * 0.175f) - 12.dp)
                    )
                    Text(
                        text = "25.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset(x = totalWidth * 0.05f)
                    )
                    Text(
                        text = "30.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val pointerOffset = (totalWidth - 14.dp) * fraction
            Box(
                modifier = Modifier
                    .offset(x = pointerOffset, y = (-8).dp)
                    .size(14.dp)
                    .background(color = activeCategoryColor, shape = CircleShape)
            )
        }

        Text(
            text = activeCategoryLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = activeCategoryColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
