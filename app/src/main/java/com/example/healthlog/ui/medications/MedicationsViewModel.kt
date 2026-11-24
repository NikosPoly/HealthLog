package com.example.healthlog.ui.medications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationWithReminders
import com.example.healthlog.model.MedicationScheduleResolver
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.notifications.MedicationAlarmScheduler
import com.example.healthlog.notifications.MedicationNotificationHelper
import com.example.healthlog.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class MedicationsViewModel(
    private val repository: MedicationRepository,
    private val appContext: Context
) : ViewModel() {

    private val _editingMedication = MutableStateFlow<MedicationWithReminders?>(null)
    val editingMedication: StateFlow<MedicationWithReminders?> = _editingMedication.asStateFlow()

    private val _deletingMedication = MutableStateFlow<MedicationWithReminders?>(null)
    val deletingMedication: StateFlow<MedicationWithReminders?> = _deletingMedication.asStateFlow()

    private val _isAddSheetOpen = MutableStateFlow(false)
    val isAddSheetOpen: StateFlow<Boolean> = _isAddSheetOpen.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun todayDateString(): String = LocalDate.now().toString()

    val allMedications: StateFlow<List<MedicationWithReminders>> =
        repository.visibleMedications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayDoses: StateFlow<List<ScheduledDoseItem>> =
        combine(
            repository.allMedications,
            repository.getDoseLogsForDate(todayDateString()),
            repository.getOverridesForDate(todayDateString())
        ) { meds, logs, overrides ->
            val today = LocalDate.now()
            MedicationScheduleResolver.resolveForDate(
                date = today,
                medicationsWithReminders = meds,
                doseLogs = logs,
                overrides = overrides,
                today = today
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun openAddMedication() {
        _editingMedication.value = null
        _isAddSheetOpen.value = true
    }

    fun openEditMedication(medicationWithReminders: MedicationWithReminders) {
        _editingMedication.value = medicationWithReminders
        _isAddSheetOpen.value = true
    }

    fun closeAddEditSheet() {
        _isAddSheetOpen.value = false
        _editingMedication.value = null
    }

    fun saveMedication(
        name: String,
        dosage: String,
        unit: String,
        frequency: String,
        instructions: String?,
        reminderTimes: List<Pair<Int, Int>>,
        saveToPrescriptions: Boolean = true,
        logDoseNow: Boolean = false,
        targetDate: LocalDate = LocalDate.now(),
        oneTimeHourMinute: Pair<Int, Int>? = null
    ) {
        viewModelScope.launch {
            val loggedMedName = MedicationActions.saveMedication(
                repository = repository,
                appContext = appContext,
                existing = _editingMedication.value,
                name = name,
                dosage = dosage,
                unit = unit,
                frequency = frequency,
                instructions = instructions,
                reminderTimes = reminderTimes,
                saveToPrescriptions = saveToPrescriptions,
                logDoseNow = logDoseNow,
                targetDate = targetDate,
                oneTimeHourMinute = oneTimeHourMinute
            )
            if (loggedMedName != null) {
                _userMessage.emit(loggedMedName)
            }
            closeAddEditSheet()
        }
    }

    fun quickLogAsNeeded(medication: Medication) {
        viewModelScope.launch {
            val nowTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            repository.recordAsNeededDose(
                medicationId = medication.id,
                scheduledDate = todayDateString(),
                scheduledTime = nowTime,
                status = "TAKEN"
            )
            _userMessage.emit(medication.name)
        }
    }

    fun toggleMedicationActive(
        medicationWithReminders: MedicationWithReminders,
        onPermissionRequired: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (medicationWithReminders.medication.frequency == "DAILY" && medicationWithReminders.reminders.isNotEmpty()) {
                if (!MedicationNotificationHelper.areNotificationsEnabled(appContext)) {
                    onPermissionRequired()
                    return@launch
                }
            }
            val newStatus = !medicationWithReminders.medication.isActive
            repository.setMedicationActive(medicationWithReminders.medication, newStatus)
            val actualReminders = repository.getRemindersForMedication(medicationWithReminders.medication.id)
            if (!newStatus) {
                actualReminders.forEach { reminder ->
                    val overrides = repository.getOverridesForReminder(reminder.id)
                    MedicationAlarmScheduler.cancelReminder(appContext, reminder.id, overrides)
                }
            } else {
                val updatedMed = repository.getMedicationById(medicationWithReminders.medication.id)
                if (updatedMed != null) {
                    actualReminders.filter { it.isEnabled }.forEach { reminder ->
                        val overrides = repository.getOverridesForReminder(reminder.id)
                        MedicationAlarmScheduler.scheduleReminder(appContext, updatedMed, reminder, overrides)
                    }
                }
            }
        }
    }

    fun enableMedicationAndSyncAlarms(medicationId: Long) {
        viewModelScope.launch {
            if (!MedicationNotificationHelper.areNotificationsEnabled(appContext)) return@launch

            val tappedMed = repository.getMedicationById(medicationId)
            if (tappedMed != null && !tappedMed.isActive) {
                repository.setMedicationActive(tappedMed, true)
            }

            val activeMeds = repository.activeMedications.first()
            activeMeds.forEach { medItem ->
                if (medItem.medication.frequency == "DAILY") {
                    medItem.reminders.filter { it.isEnabled }.forEach { reminder ->
                        val overrides = repository.getOverridesForReminder(reminder.id)
                        MedicationAlarmScheduler.scheduleReminder(appContext, medItem.medication, reminder, overrides)
                    }
                }
            }
        }
    }

    fun openDeleteConfirmation(medicationWithReminders: MedicationWithReminders) {
        _deletingMedication.value = medicationWithReminders
    }

    fun closeDeleteConfirmation() {
        _deletingMedication.value = null
    }

    fun confirmDelete() {
        val item = _deletingMedication.value ?: return
        deleteMedication(item)
        _deletingMedication.value = null
    }

    fun deleteMedication(medicationWithReminders: MedicationWithReminders) {
        viewModelScope.launch {
            val persistedReminders = repository.getRemindersForMedication(medicationWithReminders.medication.id)
            persistedReminders.forEach { reminder ->
                val overrides = repository.getOverridesForReminder(reminder.id)
                MedicationAlarmScheduler.cancelReminder(appContext, reminder.id, overrides)
            }
            medicationWithReminders.reminders.forEach { reminder ->
                MedicationAlarmScheduler.cancelReminder(appContext, reminder.id)
            }
            repository.deleteMedication(medicationWithReminders.medication)
        }
    }

    companion object {
        fun factory(
            repository: MedicationRepository,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MedicationsViewModel(repository, context.applicationContext) as T
            }
        }
    }
}
