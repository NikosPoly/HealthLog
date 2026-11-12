package com.example.healthlog.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.healthlog.database.Measurement
import com.example.healthlog.repository.MeasurementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// State and actions for the measurement details screen.
class MeasurementDetailsViewModel(
    private val repository: MeasurementRepository,
    private val measurementId: Long
) : ViewModel() {

    // The measurement observed live by its ID (null while loading or if not found).
    val measurement: StateFlow<Measurement?> =
        repository.getMeasurementById(measurementId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    fun deleteMeasurement(measurement: Measurement, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(measurement)
            onDeleted()
        }
    }

    companion object {
        fun factory(
            repository: MeasurementRepository,
            measurementId: Long
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MeasurementDetailsViewModel(repository, measurementId)
            }
        }
    }
}
