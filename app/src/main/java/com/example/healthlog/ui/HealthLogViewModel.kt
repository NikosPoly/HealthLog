package com.example.healthlog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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

class HealthLogViewModel(
    private val repository: MeasurementRepository
) : ViewModel() {

    // The live list from the database, newest first.
    val measurements: StateFlow<List<Measurement>> = repository.getAllMeasurements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Which entry dialog is open: null = none, otherwise the selected category.
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // One-time messages for the snackbar, sent after an operation has finished.
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun openEntry(category: String) {
        _selectedCategory.value = category
    }

    fun closeEntry() {
        _selectedCategory.value = null
    }

    fun save(measurement: Measurement) {
        closeEntry()

        viewModelScope.launch {
            repository.insert(measurement)
            _messages.send("Η μέτρηση αποθηκεύτηκε")
        }
    }

    fun delete(measurement: Measurement) {
        viewModelScope.launch {
            repository.delete(measurement)
            _messages.send("Η μέτρηση διαγράφηκε")
        }
    }

    companion object {
        // Tells Android how to create this ViewModel with a repository.
        fun factory(repository: MeasurementRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HealthLogViewModel(repository)
            }
        }
    }
}
