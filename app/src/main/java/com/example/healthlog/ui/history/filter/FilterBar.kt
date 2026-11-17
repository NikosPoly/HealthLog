package com.example.healthlog.ui.history.filter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.ui.details.references.bloodGlucoseCategoryColors
import com.example.healthlog.ui.details.references.bloodPressureStageColors
import com.example.healthlog.ui.details.references.bmiCategoryColors
import com.example.healthlog.ui.details.references.pulseZoneColors

// Resolves theme-aware container, content, and indicator colors for a clinical stage filter.
fun stageFilterColors(stage: ClinicalStageFilter, dark: Boolean) = when (stage) {
    is ClinicalStageFilter.BloodPressure -> bloodPressureStageColors(stage.stage, dark)
    is ClinicalStageFilter.Glucose -> bloodGlucoseCategoryColors(stage.category, dark)
    is ClinicalStageFilter.Pulse -> pulseZoneColors(stage.zone, dark)
    is ClinicalStageFilter.Weight -> bmiCategoryColors(stage.category, dark)
}

// Compact, space-saving filter bar placed directly above the history entry list.
// Takes ~40dp vertical space, keeping 85%+ of the screen clear for measurement cards.
@Composable
fun FilterBar(
    filterState: HistoryFilterState,
    onOpenFilterSheet: () -> Unit,
    onRemoveStage: (ClinicalStageFilter) -> Unit,
    onRemoveCondition: (MeasurementConditionFilter) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Main action button that opens the filter bottom sheet.
        FilterChip(
            selected = filterState.isActive,
            onClick = onOpenFilterSheet,
            label = {
                val activeCount = filterState.stages.size + filterState.conditions.size
                val labelText = if (activeCount > 0) {
                    stringResource(R.string.filter_badge_count, activeCount)
                } else {
                    stringResource(R.string.action_filter)
                }
                Text(
                    text = labelText,
                    fontWeight = if (filterState.isActive) FontWeight.SemiBold else FontWeight.Normal,
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_filter),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (filterState.isActive) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        )

        // Active clinical stage chips with dynamic clinical color badges.
        filterState.stages.forEach { stage ->
            val colors = stageFilterColors(stage, isDark)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.container,
                contentColor = colors.content,
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(stage.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.content,
                    )
                    IconButton(
                        onClick = { onRemoveStage(stage) },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.filter_stage_chip_desc),
                            modifier = Modifier.size(12.dp),
                            tint = colors.content,
                        )
                    }
                }
            }
        }

        // Active measurement condition chips with native multi-color icons.
        filterState.conditions.forEach { condition ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(condition.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Unspecified,
                    )
                    Text(
                        text = stringResource(condition.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    IconButton(
                        onClick = { onRemoveCondition(condition) },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.filter_condition_chip_desc),
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Quick reset button when any filter is active.
        if (filterState.isActive) {
            TextButton(
                onClick = onClearAll,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.filter_reset),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
