package com.example.healthlog.ui.calendar.timeline

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.MedicationScheduleResolver
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.repository.MeasurementRepository
import com.example.healthlog.repository.MedicationRepository
import com.example.healthlog.repository.SymptomRepository
import com.example.healthlog.ui.calendar.CalendarAggregator
import com.example.healthlog.ui.medications.MedicationActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TimelineDialogState(
    val editingSymptom: SymptomEntry? = null,
    val showAddEditSymptomSheet: Boolean = false,
    val symptomDefaultHour: Int? = null,
    val symptomToDelete: SymptomEntry? = null,
    val slotChooserHour: Int? = null,
    val showAddMedicationSheet: Boolean = false,
    val medicationDefaultHour: Int? = null,
    val doseToMove: ScheduledDoseItem? = null,
    val pendingDoseMove: PendingDoseMove? = null,
    val doseToDelete: ScheduledDoseItem? = null
)

class DayTimelineViewModel(
    initialDate: LocalDate,
    private val measurementRepository: MeasurementRepository,
    private val medicationRepository: MedicationRepository,
    private val symptomRepository: SymptomRepository,
    private val appContext: Context? = null,
    private val coroutineScope: CoroutineScope? = null,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    private val _selectedDate = MutableStateFlow(initialDate)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _activeFilter = MutableStateFlow(TimelineFilter.ALL)

    private val _dialogState = MutableStateFlow(TimelineDialogState())

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dayDataFlow = _selectedDate.flatMapLatest { date ->
        val startOfDayMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDayMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dateIso = date.toString()

        val medicationDataFlow = combine(
            medicationRepository.allMedications,
            medicationRepository.getDoseLogsForDate(dateIso),
            medicationRepository.getOverridesForDate(dateIso)
        ) { meds, logs, overrides ->
            Triple(meds, logs, overrides)
        }

        combine(
            measurementRepository.getMeasurementsInRange(startOfDayMillis, endOfDayMillis),
            medicationDataFlow,
            symptomRepository.getSymptomsInRange(startOfDayMillis, endOfDayMillis)
        ) { measurements, medData, symptoms ->
            Triple(measurements, medData, symptoms)
        }
    }

    val uiState: StateFlow<DayTimelineUiState> = combine(
        _selectedDate,
        _activeFilter,
        dayDataFlow,
        _dialogState
    ) { date, filter, dayData, dialogState ->
        val (measurements, medData, symptoms) = dayData
        val (meds, logs, overrides) = medData
        val filteredMeasurements = CalendarAggregator.filterCompanionPulses(measurements)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

        val vitalEvents = filteredMeasurements.map { m ->
            val zdt = Instant.ofEpochMilli(m.timestamp).atZone(zoneId)
            TimelineEvent.Vital(
                measurement = m,
                hour = zdt.hour,
                minute = zdt.minute,
                timeString = zdt.format(timeFormatter)
            )
        }

        val resolvedDoses = MedicationScheduleResolver.resolveForDate(
            date = date,
            medicationsWithReminders = meds,
            doseLogs = logs,
            overrides = overrides,
            today = todayProvider(),
            zoneId = zoneId
        )

        val medEvents = resolvedDoses.map { dose ->
            TimelineEvent.Medication(
                doseItem = dose,
                hour = dose.hour,
                minute = dose.minute,
                timeString = dose.scheduledTime
            )
        }

        val symptomEvents = symptoms.map { s ->
            val zdt = Instant.ofEpochMilli(s.timestamp).atZone(zoneId)
            TimelineEvent.Symptom(
                symptom = s,
                hour = zdt.hour,
                minute = zdt.minute,
                timeString = zdt.format(timeFormatter)
            )
        }

        val allEvents = mutableListOf<TimelineEvent>()
        if (filter == TimelineFilter.ALL || filter == TimelineFilter.VITALS) {
            allEvents.addAll(vitalEvents)
        }
        if (filter == TimelineFilter.ALL || filter == TimelineFilter.MEDICATIONS) {
            allEvents.addAll(medEvents)
        }
        if (filter == TimelineFilter.ALL || filter == TimelineFilter.SYMPTOMS) {
            allEvents.addAll(symptomEvents)
        }

        val eventsByHour = allEvents.groupBy { it.hour }
        val slots = (0..23).map { hour ->
            val hourEvents = eventsByHour[hour].orEmpty().sortedWith(
                compareBy<TimelineEvent> { it.minute }
                    .thenBy { it.eventId }
            )
            TimelineHourSlot(
                hour = hour,
                timeLabel = String.format(Locale.US, "%02d:00", hour),
                events = hourEvents
            )
        }

        DayTimelineUiState(
            selectedDate = date,
            activeFilter = filter,
            hourSlots = slots,
            totalEventCount = vitalEvents.size + medEvents.size + symptomEvents.size,
            vitalsCount = vitalEvents.size,
            medsCount = medEvents.size,
            symptomsCount = symptomEvents.size,
            editingSymptom = dialogState.editingSymptom,
            showAddEditSymptomSheet = dialogState.showAddEditSymptomSheet,
            symptomDefaultHour = dialogState.symptomDefaultHour,
            symptomToDelete = dialogState.symptomToDelete,
            slotChooserHour = dialogState.slotChooserHour,
            showAddMedicationSheet = dialogState.showAddMedicationSheet,
            medicationDefaultHour = dialogState.medicationDefaultHour,
            doseToMove = dialogState.doseToMove,
            pendingDoseMove = dialogState.pendingDoseMove,
            doseToDelete = dialogState.doseToDelete
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DayTimelineUiState(selectedDate = initialDate)
    )

    fun onPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun onNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun onJumpToToday() {
        _selectedDate.value = todayProvider()
    }

    fun onSelectFilter(filter: TimelineFilter) {
        _activeFilter.value = filter
    }

    // --- Time-slot chooser & creation ---

    fun onHourSlotClick(hour: Int) {
        _dialogState.update { it.copy(slotChooserHour = hour.coerceIn(0, 23)) }
    }

    fun onDismissSlotChooser() {
        _dialogState.update { it.copy(slotChooserHour = null) }
    }

    fun onChooseLogSymptomFromSlot() {
        val hour = _dialogState.value.slotChooserHour ?: return
        _dialogState.update {
            it.copy(
                slotChooserHour = null,
                editingSymptom = null,
                symptomDefaultHour = hour,
                showAddEditSymptomSheet = true
            )
        }
    }

    fun onChooseAddMedicationFromSlot() {
        val hour = _dialogState.value.slotChooserHour ?: return
        _dialogState.update {
            it.copy(
                slotChooserHour = null,
                medicationDefaultHour = hour,
                showAddMedicationSheet = true
            )
        }
    }

    fun onDismissAddMedicationSheet() {
        _dialogState.update {
            it.copy(
                showAddMedicationSheet = false,
                medicationDefaultHour = null
            )
        }
    }

    fun onSaveMedication(
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
    ) {
        val ctx = appContext
        scope.launch {
            if (ctx != null) {
                val loggedMedName = MedicationActions.saveMedication(
                    repository = medicationRepository,
                    appContext = ctx,
                    existing = null,
                    name = name,
                    dosage = dosage,
                    unit = unit,
                    frequency = frequency,
                    instructions = instructions,
                    reminderTimes = reminderTimes,
                    saveToPrescriptions = saveToPrescriptions,
                    logDoseNow = logDoseNow,
                    targetDate = targetDate,
                    oneTimeHourMinute = oneTimeHourMinute
                )
                if (loggedMedName != null) {
                    _userMessage.emit(loggedMedName)
                }
            }
            onDismissAddMedicationSheet()
        }
    }

    // --- Dose quick actions, move time, and delete ---

    fun onMarkDoseTaken(doseItem: ScheduledDoseItem) {
        scope.launch {
            MedicationActions.recordScheduledDoseStatus(
                repository = medicationRepository,
                appContext = appContext,
                doseItem = doseItem,
                status = "TAKEN"
            )
        }
    }

    fun onMarkDoseSkipped(doseItem: ScheduledDoseItem) {
        scope.launch {
            MedicationActions.recordScheduledDoseStatus(
                repository = medicationRepository,
                appContext = appContext,
                doseItem = doseItem,
                status = "SKIPPED"
            )
        }
    }

    fun onUndoDose(doseItem: ScheduledDoseItem) {
        scope.launch {
            MedicationActions.undoScheduledDose(
                repository = medicationRepository,
                doseItem = doseItem
            )
        }
    }

    fun onRequestMoveDose(doseItem: ScheduledDoseItem) {
        _dialogState.update {
            it.copy(
                doseToMove = doseItem,
                pendingDoseMove = null
            )
        }
    }

    fun onDismissMoveDose() {
        _dialogState.update {
            it.copy(
                doseToMove = null,
                pendingDoseMove = null
            )
        }
    }

    fun onPickMoveTime(newHour: Int, newMinute: Int) {
        val dose = _dialogState.value.doseToMove ?: return
        if (dose.isRecurring && dose.reminderId != null) {
            _dialogState.update {
                it.copy(
                    doseToMove = null,
                    pendingDoseMove = PendingDoseMove(
                        dose = dose,
                        newHour = newHour,
                        newMinute = newMinute
                    )
                )
            }
        } else {
            _dialogState.update { it.copy(doseToMove = null, pendingDoseMove = null) }
            scope.launch {
                MedicationActions.moveScheduledDose(
                    repository = medicationRepository,
                    appContext = appContext,
                    doseItem = dose,
                    newHour = newHour,
                    newMinute = newMinute,
                    scope = MedicationActions.RecurringDoseScope.THIS_DAY_ONLY,
                    today = todayProvider()
                )
            }
        }
    }

    fun onConfirmMoveScope(moveScope: MedicationActions.RecurringDoseScope) {
        val pending = _dialogState.value.pendingDoseMove ?: return
        _dialogState.update { it.copy(pendingDoseMove = null) }
        scope.launch {
            MedicationActions.moveScheduledDose(
                repository = medicationRepository,
                appContext = appContext,
                doseItem = pending.dose,
                newHour = pending.newHour,
                newMinute = pending.newMinute,
                scope = moveScope,
                today = todayProvider()
            )
        }
    }

    fun onRequestDeleteDose(doseItem: ScheduledDoseItem) {
        _dialogState.update { it.copy(doseToDelete = doseItem) }
    }

    fun onDismissDeleteDose() {
        _dialogState.update { it.copy(doseToDelete = null) }
    }

    fun onConfirmDeleteDose(deleteScope: MedicationActions.RecurringDoseScope) {
        val dose = _dialogState.value.doseToDelete ?: return
        _dialogState.update { it.copy(doseToDelete = null) }
        scope.launch {
            MedicationActions.deleteScheduledDose(
                repository = medicationRepository,
                appContext = appContext,
                doseItem = dose,
                scope = deleteScope,
                today = todayProvider()
            )
        }
    }

    // --- Symptom edit & delete ---

    fun onOpenEditSymptom(symptom: SymptomEntry) {
        _dialogState.update {
            it.copy(
                editingSymptom = symptom,
                symptomDefaultHour = null,
                showAddEditSymptomSheet = true
            )
        }
    }

    fun onDismissAddEditSymptomSheet() {
        _dialogState.update {
            it.copy(
                showAddEditSymptomSheet = false,
                editingSymptom = null,
                symptomDefaultHour = null
            )
        }
    }

    fun onSaveSymptom(
        symptomType: String,
        customName: String?,
        severity: Int,
        timestamp: Long,
        notes: String?
    ) {
        scope.launch {
            val existing = _dialogState.value.editingSymptom
            if (existing != null) {
                symptomRepository.update(
                    existing.copy(
                        symptomType = symptomType,
                        customName = customName,
                        severity = severity,
                        timestamp = timestamp,
                        notes = notes
                    )
                )
            } else {
                symptomRepository.save(
                    SymptomEntry(
                        symptomType = symptomType,
                        customName = customName,
                        severity = severity,
                        timestamp = timestamp,
                        notes = notes
                    )
                )
            }
            onDismissAddEditSymptomSheet()
        }
    }

    fun onRequestDeleteSymptom(symptom: SymptomEntry) {
        _dialogState.update { it.copy(symptomToDelete = symptom) }
    }

    fun onDismissDeleteDialog() {
        _dialogState.update { it.copy(symptomToDelete = null) }
    }

    fun onConfirmDeleteSymptom() {
        val toDelete = _dialogState.value.symptomToDelete ?: return
        scope.launch {
            symptomRepository.delete(toDelete)
            _dialogState.update { it.copy(symptomToDelete = null) }
        }
    }

    companion object {
        fun factory(
            date: LocalDate,
            measurementRepository: MeasurementRepository,
            medicationRepository: MedicationRepository,
            symptomRepository: SymptomRepository,
            appContext: Context? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DayTimelineViewModel(
                    initialDate = date,
                    measurementRepository = measurementRepository,
                    medicationRepository = medicationRepository,
                    symptomRepository = symptomRepository,
                    appContext = appContext?.applicationContext
                ) as T
            }
        }
    }
}
