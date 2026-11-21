package com.example.healthlog.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.guidelines.evaluateMeasurement
import com.example.healthlog.model.SymptomSeverity
import com.example.healthlog.model.SymptomType
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatLocale
import com.example.healthlog.ui.format.formatMeasurementValue
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onOpenMeasurementDetails: (Long) -> Unit = {},
    bottomBar: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CalendarContent(
        uiState = uiState,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onJumpToToday = viewModel::onJumpToToday,
        onSelectMonthYear = viewModel::onSelectMonthYear,
        onSelectDate = viewModel::onSelectDate,
        onOpenAddSymptom = viewModel::onOpenAddSymptom,
        onOpenEditSymptom = viewModel::onOpenEditSymptom,
        onRequestDeleteSymptom = viewModel::onRequestDeleteSymptom,
        onConfirmDeleteSymptom = viewModel::onConfirmDeleteSymptom,
        onDismissDeleteDialog = viewModel::onDismissDeleteDialog,
        onOpenMeasurementDetails = onOpenMeasurementDetails,
        bottomBar = bottomBar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarContent(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onSelectMonthYear: (YearMonth) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenAddSymptom: () -> Unit,
    onOpenEditSymptom: (SymptomEntry) -> Unit,
    onRequestDeleteSymptom: (SymptomEntry) -> Unit,
    onConfirmDeleteSymptom: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onOpenMeasurementDetails: (Long) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val locale = formatLocale()
    val monthTitle = uiState.displayedMonth.format(
        DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    val selectedSummary = uiState.daySummaries[uiState.selectedDate]
        ?: CalendarDaySummary(date = uiState.selectedDate)

    var showMonthYearPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.calendar_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = onJumpToToday) {
                        Text(
                            text = stringResource(R.string.calendar_jump_today),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddSymptom,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.calendar_action_log_symptom)
                )
            }
        },
        bottomBar = bottomBar
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Month Header with Prev/Next navigation and clickable Month/Year picker
            item(key = "month_header") {
                MonthHeader(
                    monthTitle = monthTitle,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onMonthTitleClick = { showMonthYearPicker = true }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Calendar Grid Card
            item(key = "calendar_grid") {
                CalendarGridCard(
                    displayedMonth = uiState.displayedMonth,
                    selectedDate = uiState.selectedDate,
                    daySummaries = uiState.daySummaries,
                    onSelectDate = onSelectDate
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Legend Row
            item(key = "calendar_legend") {
                CalendarLegendRow()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Day Detail Header
            item(key = "day_detail_header") {
                val fullDateText = uiState.selectedDate.format(
                    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)
                ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

                Text(
                    text = fullDateText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Empty state for selected date
            if (!selectedSummary.hasAnyData) {
                item(key = "day_empty") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_day_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        )
                    }
                }
            }

            // Symptoms section
            if (selectedSummary.hasSymptoms) {
                item(key = "section_symptoms_title") {
                    SectionTitle(title = stringResource(R.string.calendar_section_symptoms))
                }
                items(selectedSummary.symptoms, key = { "sym_${it.id}" }) { symptom ->
                    SymptomItemCard(
                        symptom = symptom,
                        onEdit = { onOpenEditSymptom(symptom) },
                        onDelete = { onRequestDeleteSymptom(symptom) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Vitals section
            if (selectedSummary.hasMeasurements) {
                item(key = "section_vitals_title") {
                    SectionTitle(title = stringResource(R.string.calendar_section_vitals))
                }
                items(selectedSummary.measurements, key = { "meas_${it.id}" }) { measurement ->
                    MeasurementDayItemCard(
                        measurement = measurement,
                        latestHeightCm = uiState.latestHeightCm,
                        onClick = { onOpenMeasurementDetails(measurement.id) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Medications section
            if (selectedSummary.hasMedications) {
                item(key = "section_meds_title") {
                    SectionTitle(title = stringResource(R.string.calendar_section_medications))
                }
                items(selectedSummary.doseLogs, key = { "dose_${it.id}" }) { dose ->
                    MedicationDoseDayItemCard(doseLog = dose)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Extra space at bottom so FAB doesn't obscure cards
            item(key = "bottom_spacing") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            initialYearMonth = uiState.displayedMonth,
            onDismiss = { showMonthYearPicker = false },
            onConfirm = { selectedYm ->
                onSelectMonthYear(selectedYm)
                showMonthYearPicker = false
            }
        )
    }

    // Delete symptom confirmation dialog
    if (uiState.symptomToDelete != null) {
        val symptom = uiState.symptomToDelete
        val title = if (symptom.symptomType == SymptomType.CUSTOM.name && !symptom.customName.isNullOrBlank()) {
            symptom.customName
        } else {
            val type = SymptomType.fromNameOrNull(symptom.symptomType) ?: SymptomType.CUSTOM
            stringResource(type.titleRes)
        }

        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            title = { Text(text = stringResource(R.string.medications_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.medications_delete_confirm_desc, title)) },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteSymptom) {
                    Text(
                        text = stringResource(R.string.medications_action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun MonthHeader(
    monthTitle: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMonthTitleClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        Surface(
            onClick = onMonthTitleClick,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        IconButton(onClick = onNext) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MonthYearPickerDialog(
    initialYearMonth: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialYearMonth.year) }
    var selectedMonth by remember { mutableStateOf(initialYearMonth.monthValue) }
    val locale = formatLocale()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.calendar_select_month_year),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Year Selector Header with chevrons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_left),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = { selectedYear++ }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 4x3 Month Chips Grid
                val months = (1..12).toList()
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 3) {
                            val monthNum = months[row * 3 + col]
                            val monthName = java.time.Month.of(monthNum)
                                .getDisplayName(TextStyle.SHORT, locale)
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

                            val isSelected = monthNum == selectedMonth
                            val chipBg = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                            val chipTextColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(chipBg)
                                    .clickable { selectedMonth = monthNum }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monthName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = chipTextColor
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(YearMonth.of(selectedYear, selectedMonth)) }) {
                Text(
                    text = stringResource(android.R.string.ok),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun CalendarGridCard(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    daySummaries: Map<LocalDate, CalendarDaySummary>,
    onSelectDate: (LocalDate) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val locale = formatLocale()
            val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
            val daysOfWeek = (0..6).map { firstDayOfWeek.plus(it.toLong()) }

            // Weekday Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { dayOfWeek ->
                    Text(
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Day Cells
            val firstDayOfMonth = displayedMonth.atDay(1)
            val firstDayIndex = (firstDayOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
            val totalDays = displayedMonth.lengthOfMonth()
            val totalCells = ((firstDayIndex + totalDays + 6) / 7) * 7

            val rows = totalCells / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayIndex + 1

                        if (dayNumber in 1..totalDays) {
                            val date = displayedMonth.atDay(dayNumber)
                            val summary = daySummaries[date] ?: CalendarDaySummary(date)
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()

                            CalendarDayCell(
                                dayNumber = dayNumber,
                                isSelected = isSelected,
                                isToday = isToday,
                                summary = summary,
                                onClick = { onSelectDate(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (row < rows - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    summary: CalendarDaySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Indicator dots row (Vitals, Symptoms, Meds)
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(6.dp)
        ) {
            // Vital sign dot
            if (summary.hasMeasurements) {
                val dotColor = if (isSelected) Color.White else (summary.vitalDotColor ?: Color.Gray)
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(dotColor, CircleShape)
                )
            }

            // Symptom dot
            if (summary.hasSymptoms) {
                val dotColor = if (isSelected) Color.White.copy(alpha = 0.9f) else (summary.symptomDotColor ?: Color(0xFFFFB300))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(dotColor, CircleShape)
                )
            }

            // Medication dot
            if (summary.hasMedications) {
                val dotColor = if (isSelected) Color.White.copy(alpha = 0.7f) else (summary.medicationDotColor ?: Color(0xFF1E88E5))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(dotColor, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun CalendarLegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = Color(0xFF387A40), label = stringResource(R.string.calendar_legend_vitals))
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem(color = Color(0xFFFFB300), label = stringResource(R.string.calendar_legend_symptoms))
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem(color = Color(0xFF1E88E5), label = stringResource(R.string.calendar_legend_meds))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SymptomItemCard(
    symptom: SymptomEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val symptomType = SymptomType.fromNameOrNull(symptom.symptomType) ?: SymptomType.CUSTOM
    val title = if (symptomType == SymptomType.CUSTOM && !symptom.customName.isNullOrBlank()) {
        symptom.customName
    } else {
        stringResource(symptomType.titleRes)
    }

    val severity = SymptomSeverity.fromLevel(symptom.severity)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val timeText = Instant.ofEpochMilli(symptom.timestamp)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Severity indicator circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(severity.lightColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = severity.level.toString(),
                    fontWeight = FontWeight.Bold,
                    color = severity.lightColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $timeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(severity.labelRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = severity.lightColor,
                        fontWeight = FontWeight.Medium
                    )
                    if (!symptom.notes.isNullOrBlank()) {
                        Text(
                            text = " — ${symptom.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    painter = painterResource(R.drawable.ic_format_pdf),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MeasurementDayItemCard(
    measurement: Measurement,
    latestHeightCm: Double?,
    onClick: () -> Unit
) {
    val decimalSep = decimalSeparator()
    val valueText = formatMeasurementValue(measurement, decimalSep)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val timeText = Instant.ofEpochMilli(measurement.timestamp)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)

    val evaluation = evaluateMeasurement(measurement, latestHeightCm)
    val rank = evaluation?.toClinicalRank() ?: VitalClinicalRank.NORMAL

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(rank.dotColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${stringResource(measurement.type.label)} • $timeText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MedicationDoseDayItemCard(
    doseLog: MedicationDoseLog
) {
    val isTaken = doseLog.status == "TAKEN"
    val statusText = stringResource(
        if (isTaken) R.string.dose_status_taken else R.string.dose_status_skipped
    )
    val statusColor = if (isTaken) Color(0xFF387A40) else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF1E88E5), CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${stringResource(R.string.nav_medications)} (${doseLog.scheduledTime})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
