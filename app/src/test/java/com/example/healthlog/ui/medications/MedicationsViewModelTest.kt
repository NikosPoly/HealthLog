package com.example.healthlog.ui.medications

import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationDao
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationWithReminders
import com.example.healthlog.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationsViewModelTest {

    private class FakeMedicationDao : MedicationDao {
        val medications = mutableListOf<Medication>()
        val reminders = mutableListOf<MedicationReminder>()
        val doseLogs = mutableListOf<MedicationDoseLog>()

        private val medicationsFlow = MutableStateFlow<List<Medication>>(emptyList())
        private val doseLogsFlow = MutableStateFlow<List<MedicationDoseLog>>(emptyList())

        fun notifyChanges() {
            medicationsFlow.value = medications.toList()
            doseLogsFlow.value = doseLogs.toList()
        }

        override fun getAllMedicationsWithReminders(): Flow<List<MedicationWithReminders>> {
            return medicationsFlow.map { list ->
                list.map { med ->
                    MedicationWithReminders(
                        medication = med,
                        reminders = reminders.filter { it.medicationId == med.id }
                    )
                }
            }
        }

        override fun getActiveMedicationsWithReminders(): Flow<List<MedicationWithReminders>> {
            return medicationsFlow.map { list ->
                list.filter { it.isActive }.map { med ->
                    MedicationWithReminders(
                        medication = med,
                        reminders = reminders.filter { it.medicationId == med.id }
                    )
                }
            }
        }

        override fun getMedicationWithReminders(id: Long): Flow<MedicationWithReminders?> {
            return medicationsFlow.map { list ->
                list.find { it.id == id }?.let { med ->
                    MedicationWithReminders(
                        medication = med,
                        reminders = reminders.filter { it.medicationId == med.id }
                    )
                }
            }
        }

        override suspend fun getMedicationById(id: Long): Medication? = medications.find { it.id == id }

        override suspend fun getAllActiveReminders(): List<MedicationReminder> {
            val activeMedIds = medications.filter { it.isActive }.map { it.id }.toSet()
            return reminders.filter { it.isEnabled && it.medicationId in activeMedIds }
        }

        override suspend fun getRemindersForMedication(medicationId: Long): List<MedicationReminder> {
            return reminders.filter { it.medicationId == medicationId }
        }

        override suspend fun insertMedication(medication: Medication): Long {
            val id = if (medication.id == 0L) (medications.size + 1).toLong() else medication.id
            val toSave = medication.copy(id = id)
            medications.removeAll { it.id == id }
            medications.add(toSave)
            notifyChanges()
            return id
        }

        override suspend fun updateMedication(medication: Medication) {
            medications.removeAll { it.id == medication.id }
            medications.add(medication)
            notifyChanges()
        }

        override suspend fun deleteMedication(medication: Medication) {
            medications.removeAll { it.id == medication.id }
            reminders.removeAll { it.medicationId == medication.id }
            doseLogs.removeAll { it.medicationId == medication.id }
            notifyChanges()
        }

        override suspend fun insertReminders(reminders: List<MedicationReminder>): List<Long> {
            val ids = mutableListOf<Long>()
            reminders.forEachIndexed { index, reminder ->
                val id = (this.reminders.size + 1 + index).toLong()
                this.reminders.add(reminder.copy(id = id))
                ids.add(id)
            }
            notifyChanges()
            return ids
        }

        override suspend fun deleteRemindersForMedication(medicationId: Long) {
            reminders.removeAll { it.medicationId == medicationId }
            notifyChanges()
        }

        override suspend fun insertDoseLog(log: MedicationDoseLog): Long {
            val id = (doseLogs.size + 1).toLong()
            doseLogs.add(log.copy(id = id))
            notifyChanges()
            return id
        }

        override suspend fun deleteDoseLog(id: Long) {
            doseLogs.removeAll { it.id == id }
            notifyChanges()
        }

        override suspend fun deleteDoseLog(medicationId: Long, date: String, time: String) {
            doseLogs.removeAll { it.medicationId == medicationId && it.scheduledDate == date && it.scheduledTime == time }
            notifyChanges()
        }

        override fun getDoseLogsForDate(date: String): Flow<List<MedicationDoseLog>> {
            return doseLogsFlow.map { list -> list.filter { it.scheduledDate == date } }
        }

        override suspend fun getDoseLog(medicationId: Long, date: String, time: String): MedicationDoseLog? {
            return doseLogs.find { it.medicationId == medicationId && it.scheduledDate == date && it.scheduledTime == time }
        }

        override fun getDoseLogsInDateRange(startDate: String, endDate: String): Flow<List<MedicationDoseLog>> {
            return doseLogsFlow.map { list ->
                list.filter { it.scheduledDate >= startDate && it.scheduledDate <= endDate }
            }
        }
    }

    @Test
    fun tabSelection_updatesSelectedTab() {
        val fakeDao = FakeMedicationDao()
        val repository = MedicationRepository(fakeDao)
        val context = android.app.Application()
        val viewModel = MedicationsViewModel(repository, context)

        assertEquals(0, viewModel.selectedTab.value)
        viewModel.selectTab(1)
        assertEquals(1, viewModel.selectedTab.value)
    }

    @Test
    fun sheetState_openAndClose() {
        val fakeDao = FakeMedicationDao()
        val repository = MedicationRepository(fakeDao)
        val context = android.app.Application()
        val viewModel = MedicationsViewModel(repository, context)

        assertEquals(false, viewModel.isAddSheetOpen.value)
        viewModel.openAddMedication()
        assertEquals(true, viewModel.isAddSheetOpen.value)
        assertEquals(null, viewModel.editingMedication.value)

        viewModel.closeAddEditSheet()
        assertEquals(false, viewModel.isAddSheetOpen.value)
    }

    @Test
    fun deleteConfirmation_openAndClose() {
        val fakeDao = FakeMedicationDao()
        val repository = MedicationRepository(fakeDao)
        val context = android.app.Application()
        val viewModel = MedicationsViewModel(repository, context)

        val med = MedicationWithReminders(
            medication = Medication(id = 1, name = "Aspirin", dosage = "100", unit = "mg", frequency = "DAILY"),
            reminders = emptyList()
        )

        assertEquals(null, viewModel.deletingMedication.value)
        viewModel.openDeleteConfirmation(med)
        assertEquals(med, viewModel.deletingMedication.value)

        viewModel.closeDeleteConfirmation()
        assertEquals(null, viewModel.deletingMedication.value)
    }
}
