package com.example.healthlog.ui.medications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationWithReminders
import com.example.healthlog.notifications.MedicationAlarmScheduler
import com.example.healthlog.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicationsViewModel(
    private val repository: MedicationRepository,
    private val appContext: Context
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0) // 0: Today's Schedule, 1: Prescriptions
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _editingMedication = MutableStateFlow<MedicationWithReminders?>(null)
    val editingMedication: StateFlow<MedicationWithReminders?> = _editingMedication.asStateFlow()

    private val _deletingMedication = MutableStateFlow<MedicationWithReminders?>(null)
    val deletingMedication: StateFlow<MedicationWithReminders?> = _deletingMedication.asStateFlow()

    private val _isAddSheetOpen = MutableStateFlow(false)
    val isAddSheetOpen: StateFlow<Boolean> = _isAddSheetOpen.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun todayDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val allMedications: StateFlow<List<MedicationWithReminders>> =
        repository.allMedications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayDoses: StateFlow<List<TodayDoseItem>> =
        combine(
            repository.activeMedications,
            repository.getDoseLogsForDate(todayDateString())
        ) { meds, logs ->
            val todayCalendar = Calendar.getInstance()
            val dayOfWeek = todayCalendar.get(Calendar.DAY_OF_WEEK)
            val currentBit = when (dayOfWeek) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 4
                Calendar.THURSDAY -> 8
                Calendar.FRIDAY -> 16
                Calendar.SATURDAY -> 32
                Calendar.SUNDAY -> 64
                else -> 1
            }

            val todayDate = todayDateString()
            val result = mutableListOf<TodayDoseItem>()

            meds.forEach { medWithReminders ->
                val med = medWithReminders.medication
                val activeReminders = medWithReminders.reminders.filter {
                    it.isEnabled && (it.daysOfWeekMask and currentBit) != 0
                }

                if (activeReminders.isNotEmpty()) {
                    val createdCal = Calendar.getInstance().apply { timeInMillis = med.createdAt }
                    val isCreatedToday = (createdCal.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR)
                            && createdCal.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR))
                    val createdHour = createdCal.get(Calendar.HOUR_OF_DAY)
                    val createdMinute = createdCal.get(Calendar.MINUTE)

                    activeReminders.forEach { reminder ->
                        val scheduledTime = String.format(Locale.US, "%02d:%02d", reminder.hour, reminder.minute)
                        val log = logs.find { it.medicationId == med.id && it.scheduledTime == scheduledTime }

                        val isBeforeCreation = isCreatedToday &&
                            (reminder.hour < createdHour || (reminder.hour == createdHour && reminder.minute < createdMinute))

                        // If the reminder occurred before the medicine was created today and wasn't logged,
                        // don't show it as an unfulfilled/pending dose for today.
                        if (!isBeforeCreation || log != null) {
                            val status = when (log?.status) {
                                "TAKEN" -> DoseStatus.TAKEN
                                "SKIPPED" -> DoseStatus.SKIPPED
                                else -> DoseStatus.PENDING
                            }

                            result.add(
                                TodayDoseItem(
                                    logId = log?.id,
                                    medication = med,
                                    reminder = reminder,
                                    scheduledTime = scheduledTime,
                                    scheduledDate = todayDate,
                                    status = status,
                                    actionTimestamp = log?.actionTimestamp,
                                    isAsNeeded = false
                                )
                            )
                        }
                    }
                } else if (med.frequency == "AS_NEEDED" || med.frequency == "ONE_TIME") {
                    // Check if any as-needed or one-time doses were logged today for this medicine
                    val asNeededLogs = logs.filter { it.medicationId == med.id }
                    asNeededLogs.forEach { log ->
                        result.add(
                            TodayDoseItem(
                                logId = log.id,
                                medication = med,
                                reminder = null,
                                scheduledTime = log.scheduledTime,
                                scheduledDate = todayDate,
                                status = DoseStatus.TAKEN,
                                actionTimestamp = log.actionTimestamp,
                                isAsNeeded = true
                            )
                        )
                    }
                }
            }

            result.sortedBy { it.scheduledTime }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

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
        logDoseNow: Boolean = false
    ) {
        viewModelScope.launch {
            val existing = _editingMedication.value
            val effectiveFrequency = if (frequency == "DAILY") {
                "DAILY"
            } else if (saveToPrescriptions) {
                "AS_NEEDED"
            } else {
                "ONE_TIME"
            }

            val medToSave = existing?.medication?.copy(
                name = name.trim(),
                dosage = dosage.trim(),
                unit = unit,
                frequency = effectiveFrequency,
                instructions = instructions?.trim()?.ifBlank { null }
            ) ?: Medication(
                name = name.trim(),
                dosage = dosage.trim(),
                unit = unit,
                frequency = effectiveFrequency,
                instructions = instructions?.trim()?.ifBlank { null },
                isActive = true
            )

            val remindersToSave = if (effectiveFrequency == "DAILY") {
                reminderTimes.map { (hour, minute) ->
                    MedicationReminder(
                        medicationId = medToSave.id,
                        hour = hour,
                        minute = minute,
                        daysOfWeekMask = 127,
                        isEnabled = true
                    )
                }
            } else {
                emptyList()
            }

            val savedId = if (existing == null) {
                repository.saveMedication(medToSave, remindersToSave)
            } else {
                // Cancel existing scheduled alarms for this medication before overwriting reminders,
                // preventing orphaned alarms from previous edits from ringing.
                val oldReminders = repository.getRemindersForMedication(medToSave.id)
                oldReminders.forEach { reminder ->
                    MedicationAlarmScheduler.cancelReminder(appContext, reminder.id)
                }
                existing.reminders.forEach { reminder ->
                    MedicationAlarmScheduler.cancelReminder(appContext, reminder.id)
                }

                repository.updateMedication(medToSave, remindersToSave)
                medToSave.id
            }

            // Schedule alarms for active recurring reminders using persisted reminders with real database IDs
            val updatedMed = repository.getMedicationById(savedId)
            if (updatedMed != null && updatedMed.isActive && effectiveFrequency == "DAILY") {
                val actualReminders = repository.getRemindersForMedication(savedId)
                actualReminders.forEach { reminder ->
                    MedicationAlarmScheduler.scheduleReminder(appContext, updatedMed, reminder)
                }
            }

            // If it's a one-time temporary intake or user requested logging a dose now
            if (effectiveFrequency == "ONE_TIME" || (effectiveFrequency == "AS_NEEDED" && logDoseNow)) {
                val nowTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                repository.recordAsNeededDose(
                    medicationId = savedId,
                    scheduledDate = todayDateString(),
                    scheduledTime = nowTime,
                    status = "TAKEN"
                )
                _userMessage.emit(medToSave.name)
            }

            closeAddEditSheet()
        }
    }

    fun markDoseTaken(doseItem: TodayDoseItem) {
        viewModelScope.launch {
            repository.recordDose(
                medicationId = doseItem.medication.id,
                scheduledDate = doseItem.scheduledDate,
                scheduledTime = doseItem.scheduledTime,
                status = "TAKEN"
            )
        }
    }

    fun markDoseSkipped(doseItem: TodayDoseItem) {
        viewModelScope.launch {
            repository.recordDose(
                medicationId = doseItem.medication.id,
                scheduledDate = doseItem.scheduledDate,
                scheduledTime = doseItem.scheduledTime,
                status = "SKIPPED"
            )
        }
    }

    fun undoDoseAction(doseItem: TodayDoseItem) {
        viewModelScope.launch {
            if (doseItem.logId != null) {
                repository.deleteDoseById(doseItem.logId)
            } else {
                repository.deleteDose(
                    medicationId = doseItem.medication.id,
                    scheduledDate = doseItem.scheduledDate,
                    scheduledTime = doseItem.scheduledTime
                )
            }
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
            val newStatus = !medicationWithReminders.medication.isActive
            if (newStatus && medicationWithReminders.medication.frequency == "DAILY" && medicationWithReminders.reminders.isNotEmpty()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            appContext,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        onPermissionRequired()
                        return@launch
                    }
                }
            }
            repository.setMedicationActive(medicationWithReminders.medication, newStatus)
            val actualReminders = repository.getRemindersForMedication(medicationWithReminders.medication.id)
            if (!newStatus) {
                actualReminders.forEach {
                    MedicationAlarmScheduler.cancelReminder(appContext, it.id)
                }
            } else {
                val updatedMed = repository.getMedicationById(medicationWithReminders.medication.id)
                if (updatedMed != null) {
                    actualReminders.filter { it.isEnabled }.forEach {
                        MedicationAlarmScheduler.scheduleReminder(appContext, updatedMed, it)
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
            // Cancel alarms for both the in-memory reminders and any reminders currently in the DB
            val persistedReminders = repository.getRemindersForMedication(medicationWithReminders.medication.id)
            persistedReminders.forEach {
                MedicationAlarmScheduler.cancelReminder(appContext, it.id)
            }
            medicationWithReminders.reminders.forEach {
                MedicationAlarmScheduler.cancelReminder(appContext, it.id)
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
