package com.example.healthlog.repository

import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationDao
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationWithReminders
import kotlinx.coroutines.flow.Flow

class MedicationRepository(
    private val dao: MedicationDao
) {
    val allMedications: Flow<List<MedicationWithReminders>> =
        dao.getAllMedicationsWithReminders()

    val activeMedications: Flow<List<MedicationWithReminders>> =
        dao.getActiveMedicationsWithReminders()

    fun getMedicationWithReminders(id: Long): Flow<MedicationWithReminders?> =
        dao.getMedicationWithReminders(id)

    suspend fun getMedicationById(id: Long): Medication? =
        dao.getMedicationById(id)

    suspend fun getAllActiveReminders(): List<MedicationReminder> =
        dao.getAllActiveReminders()

    suspend fun saveMedication(
        medication: Medication,
        reminders: List<MedicationReminder> = emptyList()
    ): Long {
        val medicationId = dao.insertMedication(medication)
        dao.deleteRemindersForMedication(medicationId)
        if (reminders.isNotEmpty()) {
            val remindersToInsert = reminders.map { it.copy(medicationId = medicationId) }
            dao.insertReminders(remindersToInsert)
        }
        return medicationId
    }

    suspend fun updateMedication(
        medication: Medication,
        reminders: List<MedicationReminder> = emptyList()
    ) {
        dao.updateMedication(medication)
        dao.deleteRemindersForMedication(medication.id)
        if (reminders.isNotEmpty()) {
            val remindersToInsert = reminders.map { it.copy(medicationId = medication.id) }
            dao.insertReminders(remindersToInsert)
        }
    }

    suspend fun setMedicationActive(medication: Medication, isActive: Boolean) {
        dao.updateMedication(medication.copy(isActive = isActive))
    }

    suspend fun deleteMedication(medication: Medication) {
        dao.deleteMedication(medication)
    }

    fun getDoseLogsForDate(date: String): Flow<List<MedicationDoseLog>> =
        dao.getDoseLogsForDate(date)

    suspend fun recordDose(
        medicationId: Long,
        scheduledDate: String,
        scheduledTime: String,
        status: String = "TAKEN",
        notes: String? = null
    ): Long {
        dao.deleteDoseLog(medicationId, scheduledDate, scheduledTime)
        return dao.insertDoseLog(
            MedicationDoseLog(
                medicationId = medicationId,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                actionTimestamp = System.currentTimeMillis(),
                status = status,
                notes = notes
            )
        )
    }

    suspend fun deleteDose(medicationId: Long, scheduledDate: String, scheduledTime: String) {
        dao.deleteDoseLog(medicationId, scheduledDate, scheduledTime)
    }
}
