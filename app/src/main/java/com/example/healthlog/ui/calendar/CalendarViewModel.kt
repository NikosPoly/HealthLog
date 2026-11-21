package com.example.healthlog.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthlog.database.Measurement
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.repository.MeasurementRepository
import com.example.healthlog.repository.MedicationRepository
import com.example.healthlog.repository.SymptomRepository
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
    val symptomToDelete: SymptomEntry? = null
)

class CalendarViewModel(
    private val measurementRepository: MeasurementRepository,
    private val medicationRepository: MedicationRepository,
    private val symptomRepository: SymptomRepository,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthSummariesFlow = _displayedMonth.flatMapLatest { month ->
        val (startMillis, endMillis) = CalendarAggregator.monthTimeRange(month)
        val (startDate, endDate) = CalendarAggregator.monthIsoDateRange(month)

        combine(
            measurementRepository.getMeasurementsInRange(startMillis, endMillis),
            medicationRepository.getDoseLogsInDateRange(startDate, endDate),
            symptomRepository.getSymptomsInRange(startMillis, endMillis),
            measurementRepository.getLatestHeight()
        ) { measurements, doses, symptoms, height ->
            CalendarAggregator.aggregateMonth(
                yearMonth = month,
                measurements = measurements,
                doseLogs = doses,
                symptoms = symptoms,
                latestHeightCm = height
            ) to height
        }
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        combine(_displayedMonth, _selectedDate, monthSummariesFlow) { month, selected, (summaries, height) ->
            Triple(month, selected, summaries to height)
        },
        combine(_editingSymptom, _showAddEditSheet, _symptomToDelete) { editing, showSheet, toDelete ->
            Triple(editing, showSheet, toDelete)
        }
    ) { (month, selected, summariesAndHeight), (editing, showSheet, toDelete) ->
        val (summaries, height) = summariesAndHeight
        CalendarUiState(
            displayedMonth = month,
            selectedDate = selected,
            daySummaries = summaries,
            latestHeightCm = height,
            isLoading = false,
            editingSymptom = editing,
            showAddEditSheet = showSheet,
            symptomToDelete = toDelete
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

    companion object {
        fun factory(
            measurementRepository: MeasurementRepository,
            medicationRepository: MedicationRepository,
            symptomRepository: SymptomRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CalendarViewModel(
                    measurementRepository = measurementRepository,
                    medicationRepository = medicationRepository,
                    symptomRepository = symptomRepository
                ) as T
            }
        }
    }
}
