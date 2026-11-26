package com.example.healthlog.ui.calendar.timeline

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthlog.R
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.MedicationScheduleResolver
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.model.ScheduledDoseStatus
import com.example.healthlog.model.SymptomSeverity
import com.example.healthlog.model.SymptomType
import com.example.healthlog.notifications.MedicationNotificationHelper
import com.example.healthlog.ui.calendar.AddEditSymptomSheet
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatLocale
import com.example.healthlog.ui.format.formatMeasurementValue
import com.example.healthlog.ui.medications.AddEditMedicationSheet
import com.example.healthlog.ui.medications.MedicationActions
import com.example.healthlog.ui.theme.icon
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun DayTimelineScreen(
    viewModel: DayTimelineViewModel,
    onBack: () -> Unit,
    onOpenMeasurementDetails: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    var notificationsEnabled by remember {
        mutableStateOf(MedicationNotificationHelper.areNotificationsEnabled(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = MedicationNotificationHelper.areNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.userMessage.collect { medName ->
            Toast.makeText(
                context,
                resources.getString(R.string.dose_logged_toast, medName),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    DayTimelineContent(
        uiState = uiState,
        onBack = onBack,
        onPreviousDay = viewModel::onPreviousDay,
        onNextDay = viewModel::onNextDay,
        onJumpToToday = viewModel::onJumpToToday,
        onSelectFilter = viewModel::onSelectFilter,
        onMarkDoseTaken = viewModel::onMarkDoseTaken,
        onMarkDoseSkipped = viewModel::onMarkDoseSkipped,
        onUndoDose = viewModel::onUndoDose,
        onRequestMoveDose = viewModel::onRequestMoveDose,
        onDismissMoveDose = viewModel::onDismissMoveDose,
        onPickMoveTime = viewModel::onPickMoveTime,
        onConfirmMoveScope = viewModel::onConfirmMoveScope,
        onRequestDeleteDose = viewModel::onRequestDeleteDose,
        onDismissDeleteDose = viewModel::onDismissDeleteDose,
        onConfirmDeleteDose = viewModel::onConfirmDeleteDose,
        onOpenEditSymptom = viewModel::onOpenEditSymptom,
        onDismissAddEditSymptomSheet = viewModel::onDismissAddEditSymptomSheet,
        onSaveSymptom = viewModel::onSaveSymptom,
        onRequestDeleteSymptom = viewModel::onRequestDeleteSymptom,
        onConfirmDeleteSymptom = viewModel::onConfirmDeleteSymptom,
        onDismissDeleteDialog = viewModel::onDismissDeleteDialog,
        onOpenMeasurementDetails = onOpenMeasurementDetails,
        onEmptyHourClick = viewModel::onHourSlotClick,
        onDismissSlotChooser = viewModel::onDismissSlotChooser,
        onChooseLogSymptomFromSlot = viewModel::onChooseLogSymptomFromSlot,
        onChooseAddMedicationFromSlot = viewModel::onChooseAddMedicationFromSlot,
        onDismissAddMedicationSheet = viewModel::onDismissAddMedicationSheet,
        onSaveMedication = viewModel::onSaveMedication,
        notificationsEnabled = notificationsEnabled
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayTimelineContent(
    uiState: DayTimelineUiState,
    onBack: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onJumpToToday: () -> Unit,
    onSelectFilter: (TimelineFilter) -> Unit,
    onMarkDoseTaken: (ScheduledDoseItem) -> Unit,
    onMarkDoseSkipped: (ScheduledDoseItem) -> Unit,
    onUndoDose: (ScheduledDoseItem) -> Unit,
    onOpenEditSymptom: (SymptomEntry) -> Unit,
    onDismissAddEditSymptomSheet: () -> Unit,
    onSaveSymptom: (symptomType: String, customName: String?, severity: Int, timestamp: Long, notes: String?) -> Unit,
    onRequestDeleteSymptom: (SymptomEntry) -> Unit,
    onConfirmDeleteSymptom: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onOpenMeasurementDetails: (Long) -> Unit,
    onEmptyHourClick: (hour: Int) -> Unit = {},
    onDismissSlotChooser: () -> Unit = {},
    onChooseLogSymptomFromSlot: () -> Unit = {},
    onChooseAddMedicationFromSlot: () -> Unit = {},
    onDismissAddMedicationSheet: () -> Unit = {},
    onSaveMedication: (
        name: String,
        dosage: String,
        unit: String,
        frequency: String,
        instructions: String?,
        reminderTimes: List<Pair<Int, Int>>,
        saveToPrescriptions: Boolean,
        logDoseNow: Boolean,
        targetDate: LocalDate,
        oneTimeHourMinute: Pair<Int, Int>?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onRequestMoveDose: (ScheduledDoseItem) -> Unit = {},
    onDismissMoveDose: () -> Unit = {},
    onPickMoveTime: (newHour: Int, newMinute: Int) -> Unit = { _, _ -> },
    onConfirmMoveScope: (MedicationActions.RecurringDoseScope) -> Unit = {},
    onRequestDeleteDose: (ScheduledDoseItem) -> Unit = {},
    onDismissDeleteDose: () -> Unit = {},
    onConfirmDeleteDose: (MedicationActions.RecurringDoseScope) -> Unit = {},
    today: LocalDate = LocalDate.now(),
    notificationsEnabled: Boolean = true
) {
    val locale = formatLocale()
    val isToday = uiState.selectedDate == today
    val isFutureDate = uiState.selectedDate.isAfter(today)

    val dateTitle = uiState.selectedDate.format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)
    ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    val listState = rememberLazyListState()

    // Auto-scroll on date change:
    // If today: current hour. Otherwise: first populated hour or 07:00 default.
    LaunchedEffect(uiState.selectedDate) {
        val targetHour = if (isToday) {
            LocalTime.now().hour
        } else {
            uiState.hourSlots.firstOrNull { it.isPopulated }?.hour ?: 7
        }
        val targetIndex = (targetHour - 1).coerceIn(0, 23)
        listState.scrollToItem(targetIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.day_timeline_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                            tint = Color.Unspecified
                        )
                    }
                },
                actions = {
                    if (!isToday) {
                        TextButton(onClick = onJumpToToday) {
                            Text(
                                text = stringResource(R.string.calendar_jump_today),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Pinned Day Navigation Header
            DayNavigationHeader(
                dateTitle = dateTitle,
                isToday = isToday,
                onPrevious = onPreviousDay,
                onNext = onNextDay
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Pinned Category Filter Chips Row
            TimelineFilterChipsRow(
                activeFilter = uiState.activeFilter,
                totalCount = uiState.totalEventCount,
                vitalsCount = uiState.vitalsCount,
                medsCount = uiState.medsCount,
                symptomsCount = uiState.symptomsCount,
                onSelectFilter = onSelectFilter
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Scrollable 24 Hour Slots (00:00 - 23:00)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("day_timeline_list")
            ) {
                items(
                    items = uiState.hourSlots,
                    key = { "slot_${it.hour}" }
                ) { slot ->
                    TimelineHourSlotRow(
                        slot = slot,
                        isFutureDate = isFutureDate,
                        notificationsEnabled = notificationsEnabled,
                        onAddToSlotClick = { onEmptyHourClick(slot.hour) },
                        onOpenMeasurementDetails = onOpenMeasurementDetails,
                        onMarkDoseTaken = onMarkDoseTaken,
                        onMarkDoseSkipped = onMarkDoseSkipped,
                        onUndoDose = onUndoDose,
                        onMoveDose = onRequestMoveDose,
                        onDeleteDose = onRequestDeleteDose,
                        onEditSymptom = onOpenEditSymptom,
                        onDeleteSymptom = onRequestDeleteSymptom
                    )
                }

                item(key = "bottom_space") {
                    Spacer(
                        modifier = Modifier
                            .height(80.dp)
                            .testTag("day_timeline_bottom_spacing")
                    )
                }
            }
        }
    }

    // Time-slot chooser dialog (Log Symptom vs Add Medicine)
    if (uiState.slotChooserHour != null) {
        val formattedHour = MedicationScheduleResolver.formatTime(uiState.slotChooserHour, 0)
        AlertDialog(
            onDismissRequest = onDismissSlotChooser,
            title = {
                Text(
                    text = stringResource(R.string.day_timeline_slot_chooser_title, formattedHour),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onChooseLogSymptomFromSlot,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.day_timeline_slot_chooser_symptom),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    FilledTonalButton(
                        onClick = onChooseAddMedicationFromSlot,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_medications),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.day_timeline_slot_chooser_medication),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissSlotChooser) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Add / Edit Symptom Sheet
    if (uiState.showAddEditSymptomSheet) {
        AddEditSymptomSheet(
            editingItem = uiState.editingSymptom,
            defaultDate = uiState.selectedDate,
            defaultTime = uiState.symptomDefaultHour?.let { it to 0 },
            onDismiss = onDismissAddEditSymptomSheet,
            onSave = onSaveSymptom
        )
    }

    // Add Medicine Sheet (pre-filled with selected date & hour slot)
    if (uiState.showAddMedicationSheet) {
        AddEditMedicationSheet(
            editingItem = null,
            onDismiss = onDismissAddMedicationSheet,
            onSave = onSaveMedication,
            initialDate = uiState.selectedDate,
            initialTime = uiState.medicationDefaultHour?.let { it to 0 }
        )
    }

    // Move Dose TimePicker Dialog
    if (uiState.doseToMove != null) {
        com.example.healthlog.ui.calendar.DoseTimePickerDialog(
            dose = uiState.doseToMove,
            onDismiss = onDismissMoveDose,
            onConfirm = onPickMoveTime
        )
    }

    // Recurring Dose Move Scope Dialog ("This day only" vs "This & all future days")
    if (uiState.pendingDoseMove != null) {
        com.example.healthlog.ui.calendar.DoseMoveScopeDialog(
            pendingMove = uiState.pendingDoseMove,
            onDismiss = onDismissMoveDose,
            onConfirmScope = onConfirmMoveScope
        )
    }

    // Delete Dose Dialog (scope dialog for recurring doses, simple confirmation for one-time logs)
    if (uiState.doseToDelete != null) {
        com.example.healthlog.ui.calendar.DoseDeleteDialog(
            dose = uiState.doseToDelete,
            onDismiss = onDismissDeleteDose,
            onConfirmScope = onConfirmDeleteDose
        )
    }

    // Delete Symptom Confirmation Dialog
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
            title = { Text(text = stringResource(R.string.symptom_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.symptom_delete_confirm_desc, title)) },
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
private fun DayNavigationHeader(
    dateTitle: String,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.day_timeline_prev_day),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isToday) {
                    Text(
                        text = stringResource(R.string.calendar_jump_today),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onNext) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.day_timeline_next_day),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineFilterChipsRow(
    activeFilter: TimelineFilter,
    totalCount: Int,
    vitalsCount: Int,
    medsCount: Int,
    symptomsCount: Int,
    onSelectFilter: (TimelineFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = activeFilter == TimelineFilter.ALL,
            onClick = { onSelectFilter(TimelineFilter.ALL) },
            label = { Text("${stringResource(R.string.timeline_filter_all)} ($totalCount)") },
            shape = RoundedCornerShape(10.dp)
        )
        FilterChip(
            selected = activeFilter == TimelineFilter.VITALS,
            onClick = { onSelectFilter(TimelineFilter.VITALS) },
            label = { Text("${stringResource(R.string.calendar_legend_vitals)} ($vitalsCount)") },
            shape = RoundedCornerShape(10.dp)
        )
        FilterChip(
            selected = activeFilter == TimelineFilter.MEDICATIONS,
            onClick = { onSelectFilter(TimelineFilter.MEDICATIONS) },
            label = { Text("${stringResource(R.string.calendar_legend_meds)} ($medsCount)") },
            shape = RoundedCornerShape(10.dp)
        )
        FilterChip(
            selected = activeFilter == TimelineFilter.SYMPTOMS,
            onClick = { onSelectFilter(TimelineFilter.SYMPTOMS) },
            label = { Text("${stringResource(R.string.calendar_legend_symptoms)} ($symptomsCount)") },
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
private fun TimelineHourSlotRow(
    slot: TimelineHourSlot,
    isFutureDate: Boolean,
    notificationsEnabled: Boolean = true,
    onAddToSlotClick: () -> Unit,
    onOpenMeasurementDetails: (Long) -> Unit,
    onMarkDoseTaken: (ScheduledDoseItem) -> Unit,
    onMarkDoseSkipped: (ScheduledDoseItem) -> Unit,
    onUndoDose: (ScheduledDoseItem) -> Unit,
    onMoveDose: (ScheduledDoseItem) -> Unit,
    onDeleteDose: (ScheduledDoseItem) -> Unit,
    onEditSymptom: (SymptomEntry) -> Unit,
    onDeleteSymptom: (SymptomEntry) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Hour label (tappable to add to any hour) + vertical timeline track
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(52.dp)
                .clickable(onClick = onAddToSlotClick)
        ) {
            Text(
                text = slot.timeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (slot.isPopulated) FontWeight.Bold else FontWeight.Medium,
                color = if (slot.isPopulated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (slot.isPopulated) 36.dp else 24.dp)
                    .background(
                        if (slot.isPopulated) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Column: Populated events or compact empty row
        if (slot.isPopulated) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                slot.events.forEach { event ->
                    when (event) {
                        is TimelineEvent.Vital -> {
                            TimelineVitalCard(
                                event = event,
                                onClick = { onOpenMeasurementDetails(event.measurement.id) }
                            )
                        }
                        is TimelineEvent.Medication -> {
                            TimelineMedicationCard(
                                event = event,
                                isFutureDate = isFutureDate,
                                notificationsEnabled = notificationsEnabled,
                                onMarkTaken = { onMarkDoseTaken(event.doseItem) },
                                onMarkSkipped = { onMarkDoseSkipped(event.doseItem) },
                                onUndo = { onUndoDose(event.doseItem) },
                                onMove = { onMoveDose(event.doseItem) },
                                onDelete = { onDeleteDose(event.doseItem) }
                            )
                        }
                        is TimelineEvent.Symptom -> {
                            TimelineSymptomCard(
                                event = event,
                                onEdit = { onEditSymptom(event.symptom) },
                                onDelete = { onDeleteSymptom(event.symptom) }
                            )
                        }
                    }
                }
            }
        } else {
            // Compact empty slot row
            Surface(
                onClick = onAddToSlotClick,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("timeline_empty_slot_${slot.hour}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.day_timeline_empty_slot),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.day_timeline_action_add_to_slot),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineVitalCard(
    event: TimelineEvent.Vital,
    onClick: () -> Unit
) {
    val m = event.measurement
    val decimalSep = decimalSeparator()
    val valueText = formatMeasurementValue(m, decimalSep)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(m.type.icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${stringResource(m.type.label)} • ${event.timeString}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TimelineMedicationCard(
    event: TimelineEvent.Medication,
    isFutureDate: Boolean,
    notificationsEnabled: Boolean = true,
    onMarkTaken: () -> Unit,
    onMarkSkipped: () -> Unit,
    onUndo: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val dose = event.doseItem
    val takenGreen = Color(0xFF387A40)
    val displayName = dose.medicationName.ifBlank { stringResource(R.string.nav_medications) }
    val dosageText = listOf(dose.dosage, dose.unit).filter { it.isNotBlank() }.joinToString(" ")

    val iconTint = when (dose.status) {
        ScheduledDoseStatus.TAKEN -> takenGreen
        ScheduledDoseStatus.SKIPPED -> MaterialTheme.colorScheme.error
        ScheduledDoseStatus.PENDING -> MaterialTheme.colorScheme.primary
        ScheduledDoseStatus.UNLOGGED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_medications),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = iconTint
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${event.timeString}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val subtitle = buildString {
                        if (dosageText.isNotBlank()) append(dosageText)
                        if (!dose.instructions.isNullOrBlank()) {
                            if (isNotEmpty()) append(" — ")
                            append(dose.instructions)
                        }
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!dose.originalTime.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.dose_moved_from, dose.originalTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!notificationsEnabled && dose.isRecurring && dose.status == ScheduledDoseStatus.PENDING) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.testTag("dose_alerts_muted_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_notifications_off),
                                    contentDescription = stringResource(R.string.dose_alerts_disabled_desc),
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.dose_alerts_disabled_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onMove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_history),
                        contentDescription = stringResource(R.string.dose_action_move_time),
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.dose_action_delete),
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (dose.status) {
                ScheduledDoseStatus.PENDING -> {
                    if (!isFutureDate) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onMarkSkipped,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dose_action_mark_skipped),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onMarkTaken,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = takenGreen,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dose_action_mark_taken),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.dose_status_scheduled),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                ScheduledDoseStatus.UNLOGGED -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = stringResource(R.string.dose_status_unlogged),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onMarkSkipped,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dose_action_mark_skipped),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onMarkTaken,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = takenGreen,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dose_action_mark_taken),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                ScheduledDoseStatus.TAKEN, ScheduledDoseStatus.SKIPPED -> {
                    val isTaken = dose.status == ScheduledDoseStatus.TAKEN
                    val statusText = stringResource(
                        if (isTaken) R.string.dose_status_taken else R.string.dose_status_skipped
                    )
                    val badgeBg = if (isTaken) takenGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                    val badgeTextColor = if (isTaken) takenGreen else MaterialTheme.colorScheme.onErrorContainer

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeTextColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (dose.isRecurring) {
                            TextButton(
                                onClick = onUndo,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dose_action_undo),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else if (dose.canToggleStatus) {
                            TextButton(
                                onClick = if (isTaken) onMarkSkipped else onMarkTaken,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        if (isTaken) {
                                            R.string.dose_action_switch_to_skipped
                                        } else {
                                            R.string.dose_action_switch_to_taken
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineSymptomCard(
    event: TimelineEvent.Symptom,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val symptom = event.symptom
    val symptomType = SymptomType.fromNameOrNull(symptom.symptomType) ?: SymptomType.CUSTOM
    val title = if (symptomType == SymptomType.CUSTOM && !symptom.customName.isNullOrBlank()) {
        symptom.customName
    } else {
        stringResource(symptomType.titleRes)
    }
    val severity = SymptomSeverity.fromLevel(symptom.severity)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(severity.lightColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = severity.level.toString(),
                    fontWeight = FontWeight.Bold,
                    color = severity.lightColor,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${event.timeString}",
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

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_format_pdf),
                    contentDescription = stringResource(R.string.symptom_action_edit),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.symptom_action_delete),
                    modifier = Modifier.size(18.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}
