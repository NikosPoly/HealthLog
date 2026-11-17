package com.example.healthlog.ui.export

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.format.formatLocale
import com.example.healthlog.ui.history.filter.DateFilter
import com.example.healthlog.ui.history.filter.DateFilterSaver
import com.example.healthlog.ui.theme.icon

// Options modal bottom sheet for selecting export format, scope, and date range.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    selectedType: MeasurementType,
    measurementsByType: Map<MeasurementType, List<Measurement>>,
    onExport: (ExportConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = formatLocale()

    var draftFormat by rememberSaveable { mutableStateOf(ExportFormat.PDF) }
    var draftScope by rememberSaveable { mutableStateOf(ExportScope.CURRENT_TYPE) }
    var draftDateFilter by rememberSaveable(stateSaver = DateFilterSaver) {
        mutableStateOf<DateFilter?>(DateFilter.Last30Days)
    }

    var showDateRangePicker by rememberSaveable { mutableStateOf(false) }

    val config = remember(draftFormat, draftScope, selectedType, draftDateFilter) {
        ExportConfig(
            format = draftFormat,
            scope = draftScope,
            selectedType = selectedType,
            dateFilter = draftDateFilter,
        )
    }

    val exportMeasurements = remember(measurementsByType, config) {
        filterExportMeasurements(measurementsByType, config)
    }

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("export_bottom_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            // Header: Title and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.export_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("export_close_button"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.export_action_cancel),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Format (PDF vs CSV)
            Text(
                text = stringResource(R.string.export_section_format),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // PDF Option Card
            FormatOptionCard(
                title = stringResource(R.string.export_format_pdf),
                description = stringResource(R.string.export_format_pdf_desc),
                iconRes = R.drawable.ic_format_pdf,
                isSelected = draftFormat == ExportFormat.PDF,
                onClick = { draftFormat = ExportFormat.PDF },
                testTag = "export_format_pdf",
            )

            Spacer(modifier = Modifier.height(8.dp))

            // CSV Option Card
            FormatOptionCard(
                title = stringResource(R.string.export_format_csv),
                description = stringResource(R.string.export_format_csv_desc),
                iconRes = R.drawable.ic_format_csv,
                isSelected = draftFormat == ExportFormat.CSV,
                onClick = { draftFormat = ExportFormat.CSV },
                testTag = "export_format_csv",
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Measurement Scope (Current metric vs All tracked metrics)
            Text(
                text = stringResource(R.string.export_section_scope),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Scope: Current Metric
            ScopeOptionRow(
                title = stringResource(
                    R.string.export_scope_current,
                ) + " (${stringResource(selectedType.label)})",
                iconRes = selectedType.icon,
                isSelected = draftScope == ExportScope.CURRENT_TYPE,
                onClick = { draftScope = ExportScope.CURRENT_TYPE },
                testTag = "export_scope_current",
            )

            // Scope: All Metrics
            ScopeOptionRow(
                title = stringResource(R.string.export_scope_all),
                iconRes = R.drawable.ic_brand_logo,
                isSelected = draftScope == ExportScope.ALL_TYPES,
                onClick = { draftScope = ExportScope.ALL_TYPES },
                testTag = "export_scope_all",
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Date Range
            Text(
                text = stringResource(R.string.export_section_date_range),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Last 7 Days
                FilterChip(
                    selected = draftDateFilter is DateFilter.Last7Days,
                    onClick = { draftDateFilter = DateFilter.Last7Days },
                    label = { Text(stringResource(R.string.date_filter_7_days)) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("export_date_7_days"),
                )

                // Last 30 Days (standard clinical default)
                FilterChip(
                    selected = draftDateFilter is DateFilter.Last30Days,
                    onClick = { draftDateFilter = DateFilter.Last30Days },
                    label = { Text(stringResource(R.string.date_filter_30_days)) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("export_date_30_days"),
                )

                // All Time
                FilterChip(
                    selected = draftDateFilter == null,
                    onClick = { draftDateFilter = null },
                    label = { Text(stringResource(R.string.date_filter_all_time)) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("export_date_all_time"),
                )

                // Pick Dates / Custom range
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
                    label = { Text(customDateLabel) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("export_date_custom"),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Entries matching status text
            if (exportMeasurements.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.export_entries_count, exportMeasurements.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.export_no_entries),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            // Action Buttons: Cancel and Download
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_cancel_button"),
                ) {
                    Text(stringResource(R.string.export_action_cancel))
                }

                Button(
                    onClick = {
                        onExport(config)
                        onDismiss()
                    },
                    enabled = exportMeasurements.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_action_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.export_action_export))
                }
            }
        }
    }

    // Material 3 Date Range Picker Dialog
    if (showDateRangePicker) {
        val pickerState = rememberDateRangePickerState()

        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = pickerState.selectedStartDateMillis
                        val endMillis = pickerState.selectedEndDateMillis
                        if (startMillis != null && endMillis != null) {
                            draftDateFilter = DateFilter.CustomRange.fromUtcMillis(
                                startUtcMillis = minOf(startMillis, endMillis),
                                endUtcMillis = maxOf(startMillis, endMillis),
                            )
                        }
                        showDateRangePicker = false
                    },
                    enabled = pickerState.selectedStartDateMillis != null && pickerState.selectedEndDateMillis != null,
                ) {
                    Text(stringResource(R.string.filter_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DateRangePicker(
                state = pickerState,
                title = {
                    Text(
                        text = stringResource(R.string.filter_section_date),
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FormatOptionCard(
    title: String,
    description: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
        ),
        border = if (isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color.Unspecified,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScopeOptionRow(
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = Color.Unspecified,
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
