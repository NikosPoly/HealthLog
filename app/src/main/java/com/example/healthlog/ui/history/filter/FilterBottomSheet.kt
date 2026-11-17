package com.example.healthlog.ui.history.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.model.MeasurementType

// Bottom sheet allowing users to filter measurements by clinical stage (and later conditions & date).
// Selection is kept in a local draft state and only applied to the history list when "Done" is clicked.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    type: MeasurementType,
    filterState: HistoryFilterState,
    onApply: (Set<ClinicalStageFilter>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()
    val availableStages = availableStagesForType(type)

    // Local draft of selected stages; only committed when "Done" is clicked.
    var draftStages by remember(filterState.stages) { mutableStateOf(filterState.stages) }
    val isDraftActive = draftStages.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            // Header: Title & Reset Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.filter_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                if (isDraftActive) {
                    TextButton(onClick = { draftStages = emptySet() }) {
                        Text(
                            text = stringResource(R.string.filter_reset),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Section 1: Clinical Stage (only shown if type has clinical stages)
            if (availableStages.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.filter_section_stage),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 10.dp),
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // "All Stages" chip (active when no stage is selected)
                    FilterChip(
                        selected = draftStages.isEmpty(),
                        onClick = { draftStages = emptySet() },
                        label = {
                            Text(stringResource(R.string.filter_all_stages))
                        },
                        shape = RoundedCornerShape(8.dp),
                    )

                    // Individual stage chips (supports multi-selection)
                    availableStages.forEach { stage ->
                        val isSelected = stage in draftStages
                        val stageColor = stageFilterColors(stage, isDark)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                draftStages = if (isSelected) {
                                    draftStages - stage
                                } else {
                                    draftStages + stage
                                }
                            },
                            label = {
                                Text(stringResource(stage.labelRes))
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(stageColor.indicator),
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = stageColor.container,
                                selectedLabelColor = stageColor.content,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Apply / Done Action (commits draft state to history screen)
            Button(
                onClick = {
                    onApply(draftStages)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.filter_done))
            }
        }
    }
}
