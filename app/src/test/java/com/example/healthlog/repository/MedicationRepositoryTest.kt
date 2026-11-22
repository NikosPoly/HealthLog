package com.example.healthlog.repository

import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationDao
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationWithReminders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationRepositoryTest {

    private class FakeMedicationDao : MedicationDao {
        val medications = mutableListOf<Medication>()
        val reminders = mutableListOf<MedicationReminder>()
        val doseLogs = mutableListOf<MedicationDoseLog>()

        private val medicationsFlow = MutableStateFlow<List<Medication>>(emptyList())
        private val doseLogsFlow = MutableStateFlow<List<MedicationDoseLog>>(emptyList())

        private fun notifyChanges() {
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

        override suspend fun getMedicationById(id: Long): Medication? {
            return medications.find { it.id == id }
        }

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

    private val fakeDao = FakeMedicationDao()
    private val repository = MedicationRepository(fakeDao)

    @Test
    fun saveMedication_insertsMedicationAndReminders() = runBlocking {
        val med = Medication(
            id = 0,
            name = "Amlodipine",
            dosage = "5",
            unit = "mg",
            frequency = "DAILY"
        )
        val reminders = listOf(
            MedicationReminder(id = 0, medicationId = 0, hour = 8, minute = 30)
        )

        val id = repository.saveMedication(med, reminders)
        assertEquals(1L, id)

        val savedMed = repository.getMedicationById(id)
        assertEquals("Amlodipine", savedMed?.name)

        val activeList = repository.activeMedications.first()
        assertEquals(1, activeList.size)
        assertEquals(1, activeList[0].reminders.size)
        assertEquals(8, activeList[0].reminders[0].hour)
        assertEquals(30, activeList[0].reminders[0].minute)
    }

    @Test
    fun setMedicationActive_togglesStatus() = runBlocking {
        val id = repository.saveMedication(
            Medication(id = 0, name = "Metformin", dosage = "500", unit = "mg", frequency = "DAILY", isActive = true)
        )

        val med = repository.getMedicationById(id)!!
        assertTrue(med.isActive)

        repository.setMedicationActive(med, false)

        val updated = repository.getMedicationById(id)!!
        assertFalse(updated.isActive)
        assertTrue(repository.activeMedications.first().isEmpty())
    }

    @Test
    fun recordDose_andDeleteDose_updatesLogs() = runBlocking {
        val medId = repository.saveMedication(
            Medication(id = 0, name = "Aspirin", dosage = "100", unit = "mg", frequency = "DAILY")
        )

        repository.recordDose(
            medicationId = medId,
            scheduledDate = "2026-11-18",
            scheduledTime = "08:00",
            status = "TAKEN"
        )

        val logs = repository.getDoseLogsForDate("2026-11-18").first()
        assertEquals(1, logs.size)
        assertEquals("TAKEN", logs[0].status)
        assertEquals("08:00", logs[0].scheduledTime)

        // Recording again replaces the dose log for that time
        repository.recordDose(
            medicationId = medId,
            scheduledDate = "2026-11-18",
            scheduledTime = "08:00",
            status = "SKIPPED"
        )
        val updatedLogs = repository.getDoseLogsForDate("2026-11-18").first()
        assertEquals(1, updatedLogs.size)
        assertEquals("SKIPPED", updatedLogs[0].status)

        // Delete dose
        repository.deleteDose(medId, "2026-11-18", "08:00")
        assertTrue(repository.getDoseLogsForDate("2026-11-18").first().isEmpty())
    }
}
