package com.example.healthlog.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.repository.MeasurementRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// State and actions of the history screen: the saved measurements and deleting them.
class HistoryViewModel(
    private val repository: MeasurementRepository
) : ViewModel() {

    // The live list from the database, newest first.
    val measurements: StateFlow<List<Measurement>> = repository.getAllMeasurements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

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
            _messages.send(R.string.message_measurement_deleted)
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
