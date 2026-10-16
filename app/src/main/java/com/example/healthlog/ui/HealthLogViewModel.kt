package com.example.healthlog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.healthlog.database.Measurement
import com.example.healthlog.database.MeasurementDao
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HealthLogViewModel(
    private val dao: MeasurementDao
) : ViewModel() {

    // The live list from the database, newest first.
    val measurements: StateFlow<List<Measurement>> = dao.getAllMeasurements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // One-time messages for the snackbar, sent after an operation has finished.
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun save(measurement: Measurement) {
        viewModelScope.launch {
            dao.insert(measurement)
            _messages.send("Η μέτρηση αποθηκεύτηκε")
        }
    }

    fun delete(measurement: Measurement) {
        viewModelScope.launch {
            dao.delete(measurement)
            _messages.send("Η μέτρηση διαγράφηκε")
        }
    }

    companion object {
        // Tells Android how to create this ViewModel with a DAO.
        fun factory(dao: MeasurementDao): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HealthLogViewModel(dao)
            }
        }
    }
}
