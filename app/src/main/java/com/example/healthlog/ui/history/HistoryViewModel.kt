package com.example.healthlog.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.repository.MeasurementRepository
import com.example.healthlog.ui.history.filter.ClinicalStageFilter
import com.example.healthlog.ui.history.filter.HistoryFilterState
import com.example.healthlog.ui.history.filter.MeasurementConditionFilter
import com.example.healthlog.ui.history.filter.filterMeasurements
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// State and actions of the history screen: the saved measurements per type, filtering, and deleting them.
class HistoryViewModel(
    private val repository: MeasurementRepository
) : ViewModel() {

    // One live list per measurement type, newest first. Each history page shows one of them.
    private val measurementsByType: Map<MeasurementType, StateFlow<List<Measurement>>> =
        MeasurementType.entries.associateWith { type ->
            repository.getMeasurementsByType(type)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList()
                )
        }

    // Latest recorded height for calculating BMI on weight measurements.
    val latestHeight: StateFlow<Double?> =
        repository.getLatestHeight()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    // Filter criteria per measurement type.
    private val filterStateByType: Map<MeasurementType, MutableStateFlow<HistoryFilterState>> =
        MeasurementType.entries.associateWith {
            MutableStateFlow(HistoryFilterState())
        }

    // Filtered measurements per measurement type, combining the raw measurements, the active filter, and latest height.
    private val filteredMeasurementsByType: Map<MeasurementType, StateFlow<List<Measurement>>> =
        MeasurementType.entries.associateWith { type ->
            combine(
                measurementsByType.getValue(type),
                filterStateByType.getValue(type),
                latestHeight
            ) { measurements, filter, height ->
                filterMeasurements(measurements, filter, height)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
        }

    // The raw (unfiltered) list of one measurement type.
    fun measurementsOf(type: MeasurementType): StateFlow<List<Measurement>> =
        measurementsByType.getValue(type)

    // The filtered list of one measurement type.
    fun filteredMeasurementsOf(type: MeasurementType): StateFlow<List<Measurement>> =
        filteredMeasurementsByType.getValue(type)

    // The active filter criteria of one measurement type.
    fun filterStateOf(type: MeasurementType): StateFlow<HistoryFilterState> =
        filterStateByType.getValue(type).asStateFlow()

    // Sets the clinical stage filters for a specific measurement type.
    fun setStageFilters(type: MeasurementType, stages: Set<ClinicalStageFilter>) {
        filterStateByType.getValue(type).update { it.copy(stages = stages) }
    }

    // Removes an individual clinical stage filter for a specific measurement type.
    fun removeStageFilter(type: MeasurementType, stage: ClinicalStageFilter) {
        filterStateByType.getValue(type).update { it.copy(stages = it.stages - stage) }
    }

    // Sets the condition filters for a specific measurement type.
    fun setConditionFilters(type: MeasurementType, conditions: Set<MeasurementConditionFilter>) {
        filterStateByType.getValue(type).update { it.copy(conditions = conditions) }
    }

    // Removes an individual condition filter for a specific measurement type.
    fun removeConditionFilter(type: MeasurementType, condition: MeasurementConditionFilter) {
        filterStateByType.getValue(type).update { it.copy(conditions = it.conditions - condition) }
    }

    // Sets both stage and condition filters simultaneously.
    fun setFilters(
        type: MeasurementType,
        stages: Set<ClinicalStageFilter> = filterStateByType.getValue(type).value.stages,
        conditions: Set<MeasurementConditionFilter> = filterStateByType.getValue(type).value.conditions,
    ) {
        filterStateByType.getValue(type).update { it.copy(stages = stages, conditions = conditions) }
    }

    // Resets all filters for a specific measurement type back to default (no filtering).
    fun clearFilters(type: MeasurementType) {
        filterStateByType.getValue(type).value = HistoryFilterState()
    }

    // The measurement waiting for delete confirmation: null = no confirmation open.
    private val _measurementToDelete = MutableStateFlow<Measurement?>(null)
    val measurementToDelete: StateFlow<Measurement?> = _measurementToDelete.asStateFlow()

    // One-time messages for the snackbar, sent after an operation has finished.
    // They are string resource IDs: the screen turns them into text, so the
    // ViewModel doesn't need access to resources.
    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages: Flow<Int> = _messages.receiveAsFlow()

    fun requestDelete(measurement: Measurement) {
        _measurementToDelete.value = measurement
    }

    fun cancelDelete() {
        _measurementToDelete.value = null
    }

    fun confirmDelete() {
        val measurement = _measurementToDelete.value ?: return
        _measurementToDelete.value = null
        delete(measurement)
    }

    private fun delete(measurement: Measurement) {
        viewModelScope.launch {
            repository.delete(measurement)
            _messages.send(R.string.message_entry_deleted)
        }
    }

    companion object {
        // Tells Android how to create this ViewModel with a repository.
        fun factory(repository: MeasurementRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistoryViewModel(repository)
            }
        }
    }
}
