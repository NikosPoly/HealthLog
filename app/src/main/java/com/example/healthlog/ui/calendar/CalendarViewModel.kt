package com.example.healthlog.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.repository.MeasurementRepository
import com.example.healthlog.repository.MedicationRepository
import com.example.healthlog.repository.SymptomRepository
import com.example.healthlog.ui.medications.MedicationActions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val displayedMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val daySummaries: Map<LocalDate, CalendarDaySummary> = emptyMap(),
    val latestHeightCm: Double? = null,
    val isLoading: Boolean = false,
    val editingSymptom: SymptomEntry? = null,
    val showAddEditSheet: Boolean = false,
    val symptomToDelete: SymptomEntry? = null,
    val doseToMove: ScheduledDoseItem? = null,
    val pendingDoseMove: MedicationActions.PendingDoseMove? = null,
    val doseToDelete: ScheduledDoseItem? = null
)

class CalendarViewModel(
    private val measurementRepository: MeasurementRepository,
    private val medicationRepository: MedicationRepository,
    private val symptomRepository: SymptomRepository,
    private val appContext: Context? = null,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val scope: kotlinx.coroutines.CoroutineScope get() = coroutineScope ?: viewModelScope

    private val _displayedMonth = MutableStateFlow(YearMonth.now())
    val displayedMonth: StateFlow<YearMonth> = _displayedMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _editingSymptom = MutableStateFlow<SymptomEntry?>(null)
    val editingSymptom: StateFlow<SymptomEntry?> = _editingSymptom.asStateFlow()

    private val _showAddEditSheet = MutableStateFlow(false)
    val showAddEditSheet: StateFlow<Boolean> = _showAddEditSheet.asStateFlow()

    private val _symptomToDelete = MutableStateFlow<SymptomEntry?>(null)
    val symptomToDelete: StateFlow<SymptomEntry?> = _symptomToDelete.asStateFlow()

    private val _doseToMove = MutableStateFlow<ScheduledDoseItem?>(null)
    val doseToMove: StateFlow<ScheduledDoseItem?> = _doseToMove.asStateFlow()

    private val _pendingDoseMove = MutableStateFlow<MedicationActions.PendingDoseMove?>(null)
    val pendingDoseMove: StateFlow<MedicationActions.PendingDoseMove?> = _pendingDoseMove.asStateFlow()

    private val _doseToDelete = MutableStateFlow<ScheduledDoseItem?>(null)
    val doseToDelete: StateFlow<ScheduledDoseItem?> = _doseToDelete.asStateFlow()

    private data class CalendarDialogState(
        val editingSymptom: SymptomEntry? = null,
        val showAddEditSheet: Boolean = false,
        val symptomToDelete: SymptomEntry? = null,
        val doseToMove: ScheduledDoseItem? = null,
        val pendingDoseMove: MedicationActions.PendingDoseMove? = null,
        val doseToDelete: ScheduledDoseItem? = null
    )

    private val dialogStateFlow = combine(
        combine(_editingSymptom, _showAddEditSheet, _symptomToDelete) { editing, showSheet, toDelete ->
            Triple(editing, showSheet, toDelete)
        },
        combine(_doseToMove, _pendingDoseMove, _doseToDelete) { toMove, pendingMove, toDeleteDose ->
            Triple(toMove, pendingMove, toDeleteDose)
        }
    ) { (editing, showSheet, toDelete), (toMove, pendingMove, toDeleteDose) ->
        CalendarDialogState(
            editingSymptom = editing,
            showAddEditSheet = showSheet,
            symptomToDelete = toDelete,
            doseToMove = toMove,
            pendingDoseMove = pendingMove,
            doseToDelete = toDeleteDose
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthSummariesFlow = _displayedMonth.flatMapLatest { month ->
        val (startMillis, endMillis) = CalendarAggregator.monthTimeRange(month)
        val (startDate, endDate) = CalendarAggregator.monthIsoDateRange(month)

        val medicationDataFlow = combine(
            medicationRepository.allMedications,
            medicationRepository.getDoseLogsInDateRange(startDate, endDate),
            medicationRepository.getOverridesInDateRange(startDate, endDate)
        ) { meds, doses, overrides ->
            Triple(meds, doses, overrides)
        }

        combine(
            measurementRepository.getMeasurementsInRange(startMillis, endMillis),
            medicationDataFlow,
            symptomRepository.getSymptomsInRange(startMillis, endMillis),
            measurementRepository.getLatestHeight()
        ) { measurements, (meds, doses, overrides), symptoms, height ->
            CalendarAggregator.aggregateMonth(
                yearMonth = month,
                measurements = measurements,
                doseLogs = doses,
                symptoms = symptoms,
                medicationsWithReminders = meds,
                overrides = overrides,
                latestHeightCm = height
            ) to height
        }
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        combine(_displayedMonth, _selectedDate, monthSummariesFlow) { month, selected, (summaries, height) ->
            Triple(month, selected, summaries to height)
        },
        dialogStateFlow
    ) { (month, selected, summariesAndHeight), dialogs ->
        val (summaries, height) = summariesAndHeight
        CalendarUiState(
            displayedMonth = month,
            selectedDate = selected,
            daySummaries = summaries,
            latestHeightCm = height,
            isLoading = false,
            editingSymptom = dialogs.editingSymptom,
            showAddEditSheet = dialogs.showAddEditSheet,
            symptomToDelete = dialogs.symptomToDelete,
            doseToMove = dialogs.doseToMove,
            pendingDoseMove = dialogs.pendingDoseMove,
            doseToDelete = dialogs.doseToDelete
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun onPreviousMonth() {
        val prev = _displayedMonth.value.minusMonths(1)
        _displayedMonth.value = prev
        if (_selectedDate.value.year != prev.year || _selectedDate.value.month != prev.month) {
            _selectedDate.value = prev.atDay(1)
        }
    }

    fun onNextMonth() {
        val next = _displayedMonth.value.plusMonths(1)
        _displayedMonth.value = next
        if (_selectedDate.value.year != next.year || _selectedDate.value.month != next.month) {
            _selectedDate.value = next.atDay(1)
        }
    }

    fun onJumpToToday() {
        val today = LocalDate.now()
        _displayedMonth.value = YearMonth.from(today)
        _selectedDate.value = today
    }

    fun onSelectMonthYear(yearMonth: YearMonth) {
        _displayedMonth.value = yearMonth
        val currentDay = _selectedDate.value.dayOfMonth
        val maxDay = yearMonth.lengthOfMonth()
        val day = currentDay.coerceAtMost(maxDay)
        _selectedDate.value = yearMonth.atDay(day)
    }

    fun onSelectDate(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _displayedMonth.value) {
            _displayedMonth.value = YearMonth.from(date)
        }
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

    fun onUndoDoseAction(doseItem: ScheduledDoseItem) {
        scope.launch {
            MedicationActions.undoScheduledDose(
                repository = medicationRepository,
                doseItem = doseItem
            )
        }
    }

    fun onOpenAddSymptom() {
        _editingSymptom.value = null
        _showAddEditSheet.value = true
    }

    fun onOpenEditSymptom(symptom: SymptomEntry) {
        _editingSymptom.value = symptom
        _showAddEditSheet.value = true
    }

    fun onDismissAddEditSheet() {
        _showAddEditSheet.value = false
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
            onDismissAddEditSheet()
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

    fun onRequestMoveDose(dose: ScheduledDoseItem) {
        _doseToMove.value = dose
        _pendingDoseMove.value = null
    }

    fun onDismissMoveDose() {
        _doseToMove.value = null
        _pendingDoseMove.value = null
    }

    fun onPickMoveTime(hour: Int, minute: Int) {
        val dose = _doseToMove.value ?: return
        if (dose.isRecurring && dose.reminderId != null) {
            _doseToMove.value = null
            _pendingDoseMove.value = MedicationActions.PendingDoseMove(dose, hour, minute)
        } else {
            _doseToMove.value = null
            _pendingDoseMove.value = null
            scope.launch {
                MedicationActions.moveScheduledDose(
                    repository = medicationRepository,
                    appContext = appContext,
                    doseItem = dose,
                    newHour = hour,
                    newMinute = minute,
                    scope = MedicationActions.RecurringDoseScope.THIS_DAY_ONLY
                )
            }
        }
    }

    fun onConfirmMoveScope(moveScope: MedicationActions.RecurringDoseScope) {
        val pending = _pendingDoseMove.value ?: return
        _pendingDoseMove.value = null
        scope.launch {
            MedicationActions.moveScheduledDose(
                repository = medicationRepository,
                appContext = appContext,
                doseItem = pending.dose,
                newHour = pending.newHour,
                newMinute = pending.newMinute,
                scope = moveScope
            )
        }
    }

    fun onRequestDeleteDose(dose: ScheduledDoseItem) {
        _doseToDelete.value = dose
    }

    fun onDismissDeleteDose() {
        _doseToDelete.value = null
    }

    fun onConfirmDeleteDose(deleteScope: MedicationActions.RecurringDoseScope) {
        val dose = _doseToDelete.value ?: return
        _doseToDelete.value = null
        scope.launch {
            MedicationActions.deleteScheduledDose(
                repository = medicationRepository,
                appContext = appContext,
                doseItem = dose,
                scope = deleteScope
            )
        }
    }

    companion object {
        fun factory(
            measurementRepository: MeasurementRepository,
            medicationRepository: MedicationRepository,
            symptomRepository: SymptomRepository,
            appContext: Context? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CalendarViewModel(
                    measurementRepository = measurementRepository,
                    medicationRepository = medicationRepository,
                    symptomRepository = symptomRepository,
                    appContext = appContext?.applicationContext
                ) as T
            }
        }
    }
}
