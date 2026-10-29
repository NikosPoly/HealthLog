package com.example.healthlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.repository.MeasurementRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// State and actions of the home screen: choosing a measurement type and saving it.
class HomeViewModel(
    private val repository: MeasurementRepository
) : ViewModel() {

    // Which entry dialog is open: null = none, otherwise the selected type.
    private val _selectedType = MutableStateFlow<MeasurementType?>(null)
    val selectedType: StateFlow<MeasurementType?> = _selectedType.asStateFlow()

    // One-time messages for the snackbar, sent after an operation has finished.
    // They are string resource IDs: the screen turns them into text, so the
    // ViewModel doesn't need access to resources.
    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages: Flow<Int> = _messages.receiveAsFlow()

    fun openEntry(type: MeasurementType) {
        _selectedType.value = type
    }

    fun closeEntry() {
        _selectedType.value = null
    }

    fun save(measurement: Measurement) {
        closeEntry()

        viewModelScope.launch {
            repository.save(measurement)
            _messages.send(R.string.message_measurement_saved)
        }
    }

    companion object {
        // Tells Android how to create this ViewModel with a repository.
        fun factory(repository: MeasurementRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(repository)
            }
        }
    }
}
