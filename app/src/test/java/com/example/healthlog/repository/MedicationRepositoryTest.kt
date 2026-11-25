package com.example.healthlog.repository

import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationDao
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import com.example.healthlog.database.MedicationWithReminders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MedicationRepositoryTest {

    private class FakeMedicationDao : MedicationDao {
        val medications = mutableListOf<Medication>()
        val reminders = mutableListOf<MedicationReminder>()
        val doseLogs = mutableListOf<MedicationDoseLog>()
        val overrides = mutableListOf<MedicationScheduleOverride>()

        private var nextMedId = 1L
        private var nextReminderId = 1L
        private var nextDoseLogId = 1L
        private var nextOverrideId = 1L

        private val medicationsFlow = MutableStateFlow<List<Medication>>(emptyList())
        private val doseLogsFlow = MutableStateFlow<List<MedicationDoseLog>>(emptyList())
        private val overridesFlow = MutableStateFlow<List<MedicationScheduleOverride>>(emptyList())

        private fun notifyChanges() {
            medicationsFlow.value = medications.toList()
            doseLogsFlow.value = doseLogs.toList()
            overridesFlow.value = overrides.toList()
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
                list.filter { it.isActive && !it.isDeleted }.map { med ->
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

        override suspend fun getReminderById(id: Long): MedicationReminder? {
            return reminders.find { it.id == id }
        }

        override suspend fun insertMedication(medication: Medication): Long {
            val id = if (medication.id == 0L) nextMedId++ else medication.id
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
            overrides.removeAll { it.medicationId == medication.id }
            notifyChanges()
        }

        override suspend fun insertReminder(reminder: MedicationReminder): Long {
            val id = if (reminder.id == 0L) nextReminderId++ else reminder.id
            reminders.removeAll { it.id == id }
            reminders.add(reminder.copy(id = id))
            notifyChanges()
            return id
        }

        override suspend fun insertReminders(reminders: List<MedicationReminder>): List<Long> {
            val ids = mutableListOf<Long>()
            reminders.forEach { reminder ->
                ids.add(insertReminder(reminder))
            }
            return ids
        }

        override suspend fun updateReminder(reminder: MedicationReminder) {
            reminders.removeAll { it.id == reminder.id }
            reminders.add(reminder)
            notifyChanges()
        }

        override suspend fun deleteReminderById(id: Long) {
            reminders.removeAll { it.id == id }
            overrides.removeAll { it.reminderId == id }
            notifyChanges()
        }

        override suspend fun deleteRemindersForMedication(medicationId: Long) {
            val removedIds = reminders.filter { it.medicationId == medicationId }.map { it.id }.toSet()
            reminders.removeAll { it.medicationId == medicationId }
            overrides.removeAll { it.reminderId in removedIds }
            notifyChanges()
        }

        override suspend fun insertDoseLog(log: MedicationDoseLog): Long {
            val id = if (log.id == 0L) nextDoseLogId++ else log.id
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

        override suspend fun deleteUnloggedDoseLogsForMedication(medicationId: Long) {
            doseLogs.removeAll { it.medicationId == medicationId && it.status == MedicationDoseLog.STATUS_UNLOGGED }
            notifyChanges()
        }

        override fun getDoseLogsForDate(date: String): Flow<List<MedicationDoseLog>> {
            return doseLogsFlow.map { list -> list.filter { it.scheduledDate == date } }
        }

        override suspend fun getDoseLog(medicationId: Long, date: String, time: String): MedicationDoseLog? {
            return doseLogs.find { it.medicationId == medicationId && it.scheduledDate == date && it.scheduledTime == time }
        }

        override suspend fun getDoseLogCountForMedication(medicationId: Long): Int {
            return doseLogs.count { it.medicationId == medicationId }
        }

        override suspend fun getUnloggedDoseLogCountForMedication(medicationId: Long): Int {
            return doseLogs.count { it.medicationId == medicationId && it.status == MedicationDoseLog.STATUS_UNLOGGED }
        }

        override fun getDoseLogsInDateRange(startDate: String, endDate: String): Flow<List<MedicationDoseLog>> {
            return doseLogsFlow.map { list ->
                list.filter { it.scheduledDate >= startDate && it.scheduledDate <= endDate }
            }
        }

        override suspend fun upsertOverride(override: MedicationScheduleOverride): Long {
            overrides.removeAll { it.reminderId == override.reminderId && it.originalDate == override.originalDate }
            val id = if (override.id == 0L) nextOverrideId++ else override.id
            overrides.add(override.copy(id = id))
            notifyChanges()
            return id
        }

        override suspend fun getOverrideById(id: Long): MedicationScheduleOverride? {
            return overrides.find { it.id == id }
        }

        override fun getOverridesForDate(date: String): Flow<List<MedicationScheduleOverride>> {
            return overridesFlow.map { list -> list.filter { it.originalDate == date } }
        }

        override fun getOverridesInDateRange(
            startDate: String,
            endDate: String
        ): Flow<List<MedicationScheduleOverride>> {
            return overridesFlow.map { list ->
                list.filter { it.originalDate >= startDate && it.originalDate <= endDate }
            }
        }

        override suspend fun getOverridesForReminder(reminderId: Long): List<MedicationScheduleOverride> {
            return overrides.filter { it.reminderId == reminderId }
        }

        override suspend fun getOverridesFromDate(fromDate: String): List<MedicationScheduleOverride> {
            return overrides.filter { it.originalDate >= fromDate }
        }

        override suspend fun deleteOverrideById(id: Long) {
            overrides.removeAll { it.id == id }
            notifyChanges()
        }

        override suspend fun deleteOverride(reminderId: Long, date: String) {
            overrides.removeAll { it.reminderId == reminderId && it.originalDate == date }
            notifyChanges()
        }

        override suspend fun deleteOverridesForReminderFromDate(reminderId: Long, fromDate: String) {
            overrides.removeAll { it.reminderId == reminderId && it.originalDate >= fromDate }
            notifyChanges()
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

    @Test
    fun rescheduleDoseForDate_createsReplacesAndClearsOverride() = runBlocking {
        val medId = repository.saveMedication(
            Medication(name = "Amlodipine", dosage = "5", unit = "mg", frequency = "DAILY"),
            listOf(MedicationReminder(medicationId = 0, hour = 8, minute = 0))
        )
        val reminderId = repository.getRemindersForMedication(medId).first().id

        // Move 08:00 → 10:30 on 2026-11-23
        val created = repository.rescheduleDoseForDate(reminderId, "2026-11-23", 10, 30)
        assertNotNull(created)
        assertEquals(MedicationScheduleOverride.TYPE_RESCHEDULED, created!!.overrideType)
        assertEquals(10, created.newHour)
        assertEquals(30, created.newMinute)
        assertEquals(1, repository.getOverridesForDate("2026-11-23").first().size)

        // Move again 10:30 → 11:15 replaces existing override (still 1 row)
        val replaced = repository.rescheduleDoseForDate(reminderId, "2026-11-23", 11, 15)
        assertNotNull(replaced)
        assertEquals(11, replaced!!.newHour)
        assertEquals(15, replaced.newMinute)
        assertEquals(1, repository.getOverridesForDate("2026-11-23").first().size)

        // Moving back to original 08:00 clears the override cleanly
        val cleared = repository.rescheduleDoseForDate(reminderId, "2026-11-23", 8, 0)
        assertNull(cleared)
        assertTrue(repository.getOverridesForDate("2026-11-23").first().isEmpty())
    }

    @Test
    fun cancelDoseForDate_persistsCancelledOverride() = runBlocking {
        val medId = repository.saveMedication(
            Medication(name = "Metformin", dosage = "500", unit = "mg", frequency = "DAILY"),
            listOf(MedicationReminder(medicationId = 0, hour = 9, minute = 0))
        )
        val reminderId = repository.getRemindersForMedication(medId).first().id

        val cancelled = repository.cancelDoseForDate(reminderId, "2026-11-24")
        assertNotNull(cancelled)
        assertEquals(MedicationScheduleOverride.TYPE_CANCELLED, cancelled!!.overrideType)
        assertNull(cancelled.newHour)
        assertNull(cancelled.newMinute)

        val rangeOverrides = repository.getOverridesInDateRange("2026-11-01", "2026-11-30").first()
        assertEquals(1, rangeOverrides.size)
        assertEquals("2026-11-24", rangeOverrides.first().originalDate)
    }

    @Test
    fun rescheduleReminderFromDate_todayUpdatesInPlaceAndClearsFutureOverrides() = runBlocking {
        val today = LocalDate.of(2026, 11, 23)
        val medId = repository.saveMedication(
            Medication(name = "Amlodipine", dosage = "5", unit = "mg", frequency = "DAILY"),
            listOf(MedicationReminder(medicationId = 0, hour = 8, minute = 0))
        )
        val reminderId = repository.getRemindersForMedication(medId).first().id
        repository.rescheduleDoseForDate(reminderId, "2026-11-22", 9, 0) // past override (kept)
        repository.rescheduleDoseForDate(reminderId, "2026-11-25", 10, 0) // future override (cleared)

        val (updated, split) = repository.rescheduleReminderFromDate(
            reminderId = reminderId,
            fromDate = today,
            newHour = 9,
            newMinute = 30,
            today = today
        )

        assertNotNull(updated)
        assertNull(split)
        assertEquals(9, updated!!.hour)
        assertEquals(30, updated.minute)
        assertNull(updated.endDate)

        // Only the pre-fromDate override survives
        val remainingOverrides = repository.getOverridesForReminder(reminderId)
        assertEquals(1, remainingOverrides.size)
        assertEquals("2026-11-22", remainingOverrides.first().originalDate)
    }

    @Test
    fun rescheduleReminderFromDate_futureDateSplitsReminderCleanly() = runBlocking {
        val today = LocalDate.of(2026, 11, 23)
        val futureDate = LocalDate.of(2026, 11, 27)
        val medId = repository.saveMedication(
            Medication(name = "Amlodipine", dosage = "5", unit = "mg", frequency = "DAILY"),
            listOf(MedicationReminder(medicationId = 0, hour = 8, minute = 0, startDate = "2026-11-20"))
        )
        val reminderId = repository.getRemindersForMedication(medId).first().id
        repository.rescheduleDoseForDate(reminderId, "2026-11-25", 9, 0) // before split (kept)
        repository.rescheduleDoseForDate(reminderId, "2026-11-28", 10, 0) // after split (cleared)

        val (cappedOriginal, splitNew) = repository.rescheduleReminderFromDate(
            reminderId = reminderId,
            fromDate = futureDate,
            newHour = 10,
            newMinute = 15,
            today = today
        )

        assertNotNull(cappedOriginal)
        assertNotNull(splitNew)
        assertEquals(8, cappedOriginal!!.hour)
        assertEquals("2026-11-26", cappedOriginal.endDate)

        assertEquals(10, splitNew!!.hour)
        assertEquals(15, splitNew.minute)
        assertEquals("2026-11-27", splitNew.startDate)
        assertNull(splitNew.endDate)

        val allReminders = repository.getRemindersForMedication(medId)
        assertEquals(2, allReminders.size)

        val remainingOverrides = repository.getOverridesForReminder(reminderId)
        assertEquals(1, remainingOverrides.size)
        assertEquals("2026-11-25", remainingOverrides.first().originalDate)
    }

    @Test
    fun deleteReminderFromDate_todayDeletesReminder_futureDateCapsEndDate() = runBlocking {
        val today = LocalDate.of(2026, 11, 23)
        val medId = repository.saveMedication(
            Medication(name = "Aspirin", dosage = "100", unit = "mg", frequency = "DAILY"),
            listOf(
                MedicationReminder(medicationId = 0, hour = 8, minute = 0),
                MedicationReminder(medicationId = 0, hour = 20, minute = 0)
            )
        )
        val reminders = repository.getRemindersForMedication(medId)
        val morningId = reminders.first { it.hour == 8 }.id
        val eveningId = reminders.first { it.hour == 20 }.id

        // Future delete caps evening reminder at 2026-11-25
        val capped = repository.deleteReminderFromDate(
            reminderId = eveningId,
            fromDate = LocalDate.of(2026, 11, 26),
            today = today
        )
        assertNotNull(capped)
        assertEquals("2026-11-25", capped!!.endDate)

        // Today delete removes morning reminder completely
        val deleted = repository.deleteReminderFromDate(
            reminderId = morningId,
            fromDate = today,
            today = today
        )
        assertNull(deleted)

        val remaining = repository.getRemindersForMedication(medId)
        assertEquals(1, remaining.size)
        assertEquals(eveningId, remaining.first().id)
        // Parent medication is preserved so past dose logs are never lost
        assertNotNull(repository.getMedicationById(medId))
    }

    @Test
    fun deleteMedication_withoutDoseLogs_hardDeletesMedicationAndReminders() = runBlocking {
        val medId = repository.saveMedication(
            Medication(name = "TypoMed", dosage = "10", unit = "mg", frequency = "DAILY"),
            listOf(MedicationReminder(medicationId = 0, hour = 8, minute = 0))
        )
        val med = repository.getMedicationById(medId)!!

        repository.deleteMedication(med)

        assertNull(repository.getMedicationById(medId))
        assertTrue(repository.getRemindersForMedication(medId).isEmpty())
        assertTrue(repository.allMedications.first().isEmpty())
        assertTrue(repository.visibleMedications.first().isEmpty())
    }

    @Test
    fun deleteMedication_recurringWithTakenAndSkippedLogs_softDeletesAndPreservesHistory() = runBlocking {
        val medId = repository.saveMedication(
            Medication(name = "Amlodipine", dosage = "5", unit = "mg", frequency = "DAILY"),
            listOf(MedicationReminder(medicationId = 0, hour = 8, minute = 0))
        )
        val reminderId = repository.getRemindersForMedication(medId).single().id
        repository.rescheduleDoseForDate(reminderId, "2026-11-25", 9, 30)
        repository.recordDose(medId, "2026-11-22", "08:00", status = "TAKEN")
        repository.recordDose(medId, "2026-11-23", "08:00", status = "SKIPPED")

        val med = repository.getMedicationById(medId)!!
        repository.deleteMedication(med)

        // Removed from Medications cabinet and active alarms
        assertTrue(repository.visibleMedications.first().isEmpty())
        assertTrue(repository.activeMedications.first().isEmpty())
        assertTrue(repository.getRemindersForMedication(medId).isEmpty())
        assertTrue(repository.getOverridesForReminder(reminderId).isEmpty())

        // Soft-deleted row and both TAKEN & SKIPPED dose logs remain for Calendar history
        val persisted = repository.getMedicationById(medId)
        assertNotNull(persisted)
        assertTrue(persisted!!.isDeleted)
        assertFalse(persisted.isActive)
        assertEquals(1, repository.allMedications.first().size)
        val historyLogs = repository.getDoseLogsInDateRange("2026-11-01", "2026-11-30").first()
        assertEquals(2, historyLogs.size)
        assertTrue(historyLogs.any { it.status == "TAKEN" && it.scheduledDate == "2026-11-22" })
        assertTrue(historyLogs.any { it.status == "SKIPPED" && it.scheduledDate == "2026-11-23" })
    }

    @Test
    fun deleteMedication_asNeededWithLoggedDose_softDeletesAndPreservesHistory() = runBlocking {
        val medId = repository.saveMedication(
            Medication(name = "Ibuprofen", dosage = "400", unit = "mg", frequency = "AS_NEEDED")
        )
        repository.recordAsNeededDose(medId, "2026-11-23", "14:30", status = "TAKEN")

        val med = repository.getMedicationById(medId)!!
        repository.deleteMedication(med)

        assertTrue(repository.visibleMedications.first().isEmpty())
        val persisted = repository.getMedicationById(medId)
        assertNotNull(persisted)
        assertTrue(persisted!!.isDeleted)
        assertFalse(persisted.isActive)
        val logs = repository.getDoseLogsForDate("2026-11-23").first()
        assertEquals(1, logs.size)
        assertEquals("TAKEN", logs.single().status)
    }
}
