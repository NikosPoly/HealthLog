package com.example.healthlog.ui.home

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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// State and actions of the home screen: choosing a measurement type and saving it.
class HomeViewModel(
    private val repository: MeasurementRepository
) : ViewModel() {

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

    companion object {
        // Tells Android how to create this ViewModel with a repository.
        fun factory(repository: MeasurementRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(repository)
            }
        }
    }
}
