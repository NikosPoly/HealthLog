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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val activeFilter: StateFlow<TimelineFilter> = _activeFilter.asStateFlow()

    private val _editingSymptom = MutableStateFlow<SymptomEntry?>(null)
    val editingSymptom: StateFlow<SymptomEntry?> = _editingSymptom.asStateFlow()

    private val _showAddEditSymptomSheet = MutableStateFlow(false)
    val showAddEditSymptomSheet: StateFlow<Boolean> = _showAddEditSymptomSheet.asStateFlow()

    private val _symptomToDelete = MutableStateFlow<SymptomEntry?>(null)
    val symptomToDelete: StateFlow<SymptomEntry?> = _symptomToDelete.asStateFlow()

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
        combine(_editingSymptom, _showAddEditSymptomSheet, _symptomToDelete) { editing, showSheet, toDelete ->
            Triple(editing, showSheet, toDelete)
        }
    ) { date, filter, dayData, symptomState ->
        val (measurements, medData, symptoms) = dayData
        val (meds, logs, overrides) = medData
        val (editing, showSheet, toDelete) = symptomState
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
            editingSymptom = editing,
            showAddEditSymptomSheet = showSheet,
            symptomToDelete = toDelete
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

    fun onOpenEditSymptom(symptom: SymptomEntry) {
        _editingSymptom.value = symptom
        _showAddEditSymptomSheet.value = true
    }

    fun onDismissAddEditSymptomSheet() {
        _showAddEditSymptomSheet.value = false
        _editingSymptom.value = null
    }

    fun onSaveSymptom(
        symptomType: String,
        customName: String?,
        severity: Int,
        timestamp: Long,
        notes: String?
    ) {
        scope.launch {
            val existing = _editingSymptom.value
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
        _symptomToDelete.value = symptom
    }

    fun onDismissDeleteDialog() {
        _symptomToDelete.value = null
    }

    fun onConfirmDeleteSymptom() {
        val toDelete = _symptomToDelete.value ?: return
        scope.launch {
            symptomRepository.delete(toDelete)
            _symptomToDelete.value = null
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
