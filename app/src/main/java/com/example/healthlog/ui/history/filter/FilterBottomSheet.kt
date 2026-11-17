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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.PulseZone
import com.example.healthlog.model.BodyPosition
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementArm
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.format.formatLocale
import java.time.LocalDate
import java.time.ZoneId

// Bottom sheet allowing users to filter measurements by clinical stage, conditions, and date range.
// Selection is kept in a local draft state and only applied to the history list when "Done" is clicked.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    type: MeasurementType,
    filterState: HistoryFilterState,
    onApply: (Set<ClinicalStageFilter>, Set<MeasurementConditionFilter>, DateFilter?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()
    val locale = formatLocale()
    val availableStages = availableStagesForType(type)
    val availableConditions = availableConditionsForType(type)

    // Local drafts of selected filters; preserved across configuration changes (rotation).
    // Only committed to the HistoryViewModel when "Done" is clicked.
    var draftStages by rememberSaveable(type, stateSaver = StageSetSaver) {
        mutableStateOf(filterState.stages)
    }
    var draftConditions by rememberSaveable(type, stateSaver = ConditionSetSaver) {
        mutableStateOf(filterState.conditions)
    }
    var draftDateFilter by rememberSaveable(type, stateSaver = DateFilterSaver) {
        mutableStateOf(filterState.dateFilter)
    }

    var showDateRangePicker by rememberSaveable { mutableStateOf(false) }

    val isDraftActive = draftStages.isNotEmpty() ||
        draftConditions.isNotEmpty() ||
        (draftDateFilter != null)

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
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
                    TextButton(
                        onClick = {
                            draftStages = emptySet()
                            draftConditions = emptySet()
                            draftDateFilter = null
                        },
                    ) {
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

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Section 2: Physical Measurement Conditions (if applicable to this metric)
            if (availableConditions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.filter_section_conditions),
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
                    // "All Conditions" chip (active when no condition is selected)
                    FilterChip(
                        selected = draftConditions.isEmpty(),
                        onClick = { draftConditions = emptySet() },
                        label = {
                            Text(stringResource(R.string.filter_all_conditions))
                        },
                        shape = RoundedCornerShape(8.dp),
                    )

                    availableConditions.forEach { condition ->
                        val isSelected = condition in draftConditions

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                draftConditions = if (isSelected) {
                                    draftConditions - condition
                                } else {
                                    draftConditions + condition
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(condition.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.Unspecified,
                                )
                            },
                            label = {
                                Text(stringResource(condition.labelRes))
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Section 3: Date & Date Range
            Text(
                text = stringResource(R.string.filter_section_date),
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
                // "All Time" preset
                FilterChip(
                    selected = draftDateFilter == null,
                    onClick = { draftDateFilter = null },
                    label = {
                        Text(stringResource(R.string.date_filter_all_time))
                    },
                    shape = RoundedCornerShape(8.dp),
                )

                // "Today" preset
                FilterChip(
                    selected = draftDateFilter is DateFilter.Today,
                    onClick = {
                        draftDateFilter = if (draftDateFilter is DateFilter.Today) null else DateFilter.Today
                    },
                    label = {
                        Text(stringResource(R.string.date_filter_today))
                    },
                    shape = RoundedCornerShape(8.dp),
                )

                // "Last 7 Days" preset
                FilterChip(
                    selected = draftDateFilter is DateFilter.Last7Days,
                    onClick = {
                        draftDateFilter = if (draftDateFilter is DateFilter.Last7Days) null else DateFilter.Last7Days
                    },
                    label = {
                        Text(stringResource(R.string.date_filter_7_days))
                    },
                    shape = RoundedCornerShape(8.dp),
                )

                // "Last 30 Days" preset
                FilterChip(
                    selected = draftDateFilter is DateFilter.Last30Days,
                    onClick = {
                        draftDateFilter = if (draftDateFilter is DateFilter.Last30Days) null else DateFilter.Last30Days
                    },
                    label = {
                        Text(stringResource(R.string.date_filter_30_days))
                    },
                    shape = RoundedCornerShape(8.dp),
                )

                // "Custom Range" / "Pick Dates" Calendar chip
                val isCustomDate = draftDateFilter is DateFilter.CustomRange
                val customDateLabel = if (isCustomDate) {
                    (draftDateFilter as DateFilter.CustomRange).displayLabel(locale)
                } else {
                    stringResource(R.string.date_filter_custom)
                }

                FilterChip(
                    selected = isCustomDate,
                    onClick = { showDateRangePicker = true },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Unspecified,
                        )
                    },
                    label = {
                        Text(customDateLabel)
                    },
                    shape = RoundedCornerShape(8.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply / Done Action (commits draft stages, conditions, and date filter)
            Button(
                onClick = {
                    onApply(draftStages, draftConditions, draftDateFilter)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.filter_done))
            }
        }
    }

    // Material 3 Date Range Picker Dialog
    if (showDateRangePicker) {
        val customRange = draftDateFilter as? DateFilter.CustomRange
        val initialStartUtc = customRange?.startDate?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli()
        val initialEndUtc = customRange?.endDate?.atStartOfDay(ZoneId.of("UTC"))?.toInstant()?.toEpochMilli()

        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialStartUtc,
            initialSelectedEndDateMillis = initialEndUtc,
        )

        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        if (startMillis != null) {
                            draftDateFilter = DateFilter.CustomRange.fromUtcMillis(
                                startUtcMillis = startMillis,
                                endUtcMillis = dateRangePickerState.selectedEndDateMillis,
                            )
                        }
                        showDateRangePicker = false
                    },
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// Savers for preserving draft selections across configuration changes (e.g. device rotation).
internal fun encodeStageFilter(stage: ClinicalStageFilter): String = when (stage) {
    is ClinicalStageFilter.BloodPressure -> "BP:${stage.stage.name}"
    is ClinicalStageFilter.Glucose -> "GLUCOSE:${stage.category.name}"
    is ClinicalStageFilter.Pulse -> "PULSE:${stage.zone.name}"
    is ClinicalStageFilter.Weight -> "BMI:${stage.category.name}"
}

internal fun decodeStageFilter(encoded: String): ClinicalStageFilter? = runCatching {
    val parts = encoded.split(':', limit = 2)
    when (parts[0]) {
        "BP" -> ClinicalStageFilter.BloodPressure(BloodPressureStage.valueOf(parts[1]))
        "GLUCOSE" -> ClinicalStageFilter.Glucose(BloodGlucoseCategory.valueOf(parts[1]))
        "PULSE" -> ClinicalStageFilter.Pulse(PulseZone.valueOf(parts[1]))
        "BMI" -> ClinicalStageFilter.Weight(BmiCategory.valueOf(parts[1]))
        else -> null
    }
}.getOrNull()

internal val StageSetSaver: Saver<Set<ClinicalStageFilter>, ArrayList<String>> = Saver(
    save = { set -> ArrayList(set.map(::encodeStageFilter)) },
    restore = { list -> list.mapNotNull(::decodeStageFilter).toSet() },
)

internal fun encodeConditionFilter(condition: MeasurementConditionFilter): String = when (condition) {
    is MeasurementConditionFilter.Arm -> "ARM:${condition.arm.name}"
    is MeasurementConditionFilter.Position -> "POS:${condition.position.name}"
    is MeasurementConditionFilter.Meal -> "MEAL:${condition.meal.name}"
    is MeasurementConditionFilter.Arrhythmia -> "ARRHYTHMIA:${condition.detected}"
}

internal fun decodeConditionFilter(encoded: String): MeasurementConditionFilter? = runCatching {
    val parts = encoded.split(':', limit = 2)
    when (parts[0]) {
        "ARM" -> MeasurementConditionFilter.Arm(MeasurementArm.valueOf(parts[1]))
        "POS" -> MeasurementConditionFilter.Position(BodyPosition.valueOf(parts[1]))
        "MEAL" -> MeasurementConditionFilter.Meal(MealContext.valueOf(parts[1]))
        "ARRHYTHMIA" -> MeasurementConditionFilter.Arrhythmia(parts[1].toBooleanStrict())
        else -> null
    }
}.getOrNull()

internal val ConditionSetSaver: Saver<Set<MeasurementConditionFilter>, ArrayList<String>> = Saver(
    save = { set -> ArrayList(set.map(::encodeConditionFilter)) },
    restore = { list -> list.mapNotNull(::decodeConditionFilter).toSet() },
)

internal fun encodeDateFilter(filter: DateFilter?): String = when (filter) {
    null -> ""
    is DateFilter.Today -> "TODAY"
    is DateFilter.Last7Days -> "LAST_7"
    is DateFilter.Last30Days -> "LAST_30"
    is DateFilter.CustomRange -> "CUSTOM:${filter.startDate.toEpochDay()}:${filter.endDate.toEpochDay()}"
}

internal fun decodeDateFilter(encoded: String): DateFilter? = when {
    encoded.isEmpty() -> null
    encoded == "TODAY" -> DateFilter.Today
    encoded == "LAST_7" -> DateFilter.Last7Days
    encoded == "LAST_30" -> DateFilter.Last30Days
    encoded.startsWith("CUSTOM:") -> runCatching {
        val parts = encoded.removePrefix("CUSTOM:").split(':')
        DateFilter.CustomRange(
            startDate = LocalDate.ofEpochDay(parts[0].toLong()),
            endDate = LocalDate.ofEpochDay(parts[1].toLong()),
        )
    }.getOrNull()
    else -> null
}

internal val DateFilterSaver: Saver<DateFilter?, String> = Saver(
    save = { encodeDateFilter(it) },
    restore = { decodeDateFilter(it) },
)
