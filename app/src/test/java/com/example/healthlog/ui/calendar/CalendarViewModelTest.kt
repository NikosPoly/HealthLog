package com.example.healthlog.ui.calendar

import com.example.healthlog.database.Measurement
import com.example.healthlog.database.MeasurementDao
import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationDao
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import com.example.healthlog.database.MedicationWithReminders
import com.example.healthlog.database.SymptomDao
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.ScheduledDoseStatus
import com.example.healthlog.repository.MeasurementRepository
import com.example.healthlog.repository.MedicationRepository
import com.example.healthlog.repository.SymptomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModelTest {

    private class FakeMeasurementDao : MeasurementDao {
        val measurements = mutableListOf<Measurement>()
        override suspend fun insertAll(measurements: List<Measurement>) { this.measurements.addAll(measurements) }
        override suspend fun delete(measurement: Measurement) { this.measurements.remove(measurement) }
        override fun getMeasurementsByType(type: MeasurementType): Flow<List<Measurement>> =
            MutableStateFlow(measurements.filter { it.type == type })
        override fun getMeasurementById(id: Long): Flow<Measurement?> =
            MutableStateFlow(measurements.find { it.id == id })
        override fun getLatestHeight(): Flow<Double?> = MutableStateFlow(175.0)
        override fun getMeasurementsInRange(startMillis: Long, endMillis: Long): Flow<List<Measurement>> =
            MutableStateFlow(measurements.filter { it.timestamp >= startMillis && it.timestamp < endMillis })
    }

    private class FakeMedicationDao : MedicationDao {
        val medicationsFlow = MutableStateFlow<List<MedicationWithReminders>>(emptyList())
        val doseLogs = mutableListOf<MedicationDoseLog>()
        val doseLogsFlow = MutableStateFlow<List<MedicationDoseLog>>(emptyList())
        val overridesFlow = MutableStateFlow<List<MedicationScheduleOverride>>(emptyList())
        private var nextDoseId = 1L

        private fun notifyDoseChanges() {
            doseLogsFlow.value = doseLogs.toList()
        }

        override fun getAllMedicationsWithReminders(): Flow<List<MedicationWithReminders>> = medicationsFlow
        override fun getActiveMedicationsWithReminders(): Flow<List<MedicationWithReminders>> =
            medicationsFlow.map { list -> list.filter { it.medication.isActive && !it.medication.isDeleted } }
        override fun getMedicationWithReminders(id: Long): Flow<MedicationWithReminders?> =
            medicationsFlow.map { list -> list.find { it.medication.id == id } }
        override suspend fun getMedicationById(id: Long): Medication? =
            medicationsFlow.value.find { it.medication.id == id }?.medication
        override suspend fun getAllActiveReminders(): List<MedicationReminder> =
            medicationsFlow.value.filter { it.medication.isActive }.flatMap { it.reminders }
        override suspend fun getRemindersForMedication(medicationId: Long): List<MedicationReminder> =
            medicationsFlow.value.find { it.medication.id == medicationId }?.reminders.orEmpty()
        override suspend fun getReminderById(id: Long): MedicationReminder? =
            medicationsFlow.value.flatMap { it.reminders }.find { it.id == id }
        override suspend fun insertMedication(medication: Medication): Long = 1L
        override suspend fun updateMedication(medication: Medication) {}
        override suspend fun deleteMedication(medication: Medication) {}
        override suspend fun insertReminder(reminder: MedicationReminder): Long = 1L
        override suspend fun insertReminders(reminders: List<MedicationReminder>): List<Long> = emptyList()
        override suspend fun updateReminder(reminder: MedicationReminder) {}
        override suspend fun deleteReminderById(id: Long) {}
        override suspend fun deleteRemindersForMedication(medicationId: Long) {}
        override suspend fun insertDoseLog(log: MedicationDoseLog): Long {
            val id = if (log.id == 0L) nextDoseId++ else log.id
            val entry = log.copy(id = id)
            doseLogs.removeAll { it.id == id }
            doseLogs.add(entry)
            notifyDoseChanges()
            return id
        }
        override suspend fun deleteDoseLog(id: Long) {
            doseLogs.removeAll { it.id == id }
            notifyDoseChanges()
        }
        override suspend fun deleteDoseLog(medicationId: Long, date: String, time: String) {
            doseLogs.removeAll {
                it.medicationId == medicationId && it.scheduledDate == date && it.scheduledTime == time
            }
            notifyDoseChanges()
        }
        override fun getDoseLogsForDate(date: String): Flow<List<MedicationDoseLog>> =
            doseLogsFlow.map { list -> list.filter { it.scheduledDate == date } }
        override suspend fun getDoseLog(medicationId: Long, date: String, time: String): MedicationDoseLog? =
            doseLogs.find { it.medicationId == medicationId && it.scheduledDate == date && it.scheduledTime == time }
        override suspend fun getDoseLogCountForMedication(medicationId: Long): Int =
            doseLogs.count { it.medicationId == medicationId }
        override fun getDoseLogsInDateRange(startDate: String, endDate: String): Flow<List<MedicationDoseLog>> =
            doseLogsFlow.map { list -> list.filter { it.scheduledDate >= startDate && it.scheduledDate <= endDate } }
        override suspend fun upsertOverride(override: MedicationScheduleOverride): Long = 1L
        override suspend fun getOverrideById(id: Long): MedicationScheduleOverride? = null
        override fun getOverridesForDate(date: String): Flow<List<MedicationScheduleOverride>> =
            overridesFlow.map { list -> list.filter { it.originalDate == date } }
        override fun getOverridesInDateRange(startDate: String, endDate: String): Flow<List<MedicationScheduleOverride>> =
            overridesFlow.map { list -> list.filter { it.originalDate >= startDate && it.originalDate <= endDate } }
        override suspend fun getOverridesForReminder(reminderId: Long): List<MedicationScheduleOverride> = emptyList()
        override suspend fun getOverridesFromDate(fromDate: String): List<MedicationScheduleOverride> = emptyList()
        override suspend fun deleteOverrideById(id: Long) {}
        override suspend fun deleteOverride(reminderId: Long, date: String) {}
        override suspend fun deleteOverridesForReminderFromDate(reminderId: Long, fromDate: String) {}
    }

    private class FakeSymptomDao : SymptomDao {
        val symptoms = mutableListOf<SymptomEntry>()
        val flow = MutableStateFlow<List<SymptomEntry>>(emptyList())
        private var nextId = 1L

        private fun notifyChanges() {
            flow.value = symptoms.toList()
        }

        override suspend fun insert(symptom: SymptomEntry): Long {
            val id = if (symptom.id == 0L) nextId++ else symptom.id
            val entry = symptom.copy(id = id)
            symptoms.removeAll { it.id == id }
            symptoms.add(entry)
            notifyChanges()
            return id
        }

        override suspend fun update(symptom: SymptomEntry) {
            symptoms.removeAll { it.id == symptom.id }
            symptoms.add(symptom)
            notifyChanges()
        }

        override suspend fun delete(symptom: SymptomEntry) {
            symptoms.removeAll { it.id == symptom.id }
            notifyChanges()
        }

        override suspend fun deleteById(id: Long) {
            symptoms.removeAll { it.id == id }
            notifyChanges()
        }

        override fun getSymptomById(id: Long): Flow<SymptomEntry?> =
            flow.map { list -> list.find { it.id == id } }

        override fun getSymptomsInRange(startMillis: Long, endMillis: Long): Flow<List<SymptomEntry>> =
            flow.map { list -> list.filter { it.timestamp >= startMillis && it.timestamp < endMillis } }

        override fun getAllSymptoms(): Flow<List<SymptomEntry>> = flow
    }

    private lateinit var measurementDao: FakeMeasurementDao
    private lateinit var medicationDao: FakeMedicationDao
    private lateinit var symptomDao: FakeSymptomDao
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        measurementDao = FakeMeasurementDao()
        medicationDao = FakeMedicationDao()
        symptomDao = FakeSymptomDao()

        viewModel = CalendarViewModel(
            measurementRepository = MeasurementRepository(measurementDao),
            medicationRepository = MedicationRepository(medicationDao),
            symptomRepository = SymptomRepository(symptomDao),
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )
    }

    @Test
    fun monthNavigation_updatesDisplayedMonthAndSelection() {
        val initialMonth = viewModel.displayedMonth.value

        viewModel.onNextMonth()
        assertEquals(initialMonth.plusMonths(1), viewModel.displayedMonth.value)

        viewModel.onPreviousMonth()
        assertEquals(initialMonth, viewModel.displayedMonth.value)

        val targetDate = LocalDate.of(2026, 5, 20)
        viewModel.onSelectDate(targetDate)
        assertEquals(targetDate, viewModel.selectedDate.value)
        assertEquals(YearMonth.of(2026, 5), viewModel.displayedMonth.value)

        // Select month/year directly via dialog
        viewModel.onSelectMonthYear(YearMonth.of(2027, 8))
        assertEquals(YearMonth.of(2027, 8), viewModel.displayedMonth.value)
        assertEquals(LocalDate.of(2027, 8, 20), viewModel.selectedDate.value)
    }

    @Test
    fun jumpToToday_resetsMonthAndDateToNow() {
        viewModel.onSelectDate(LocalDate.of(2025, 1, 15))

        viewModel.onJumpToToday()

        val today = LocalDate.now()
        assertEquals(today, viewModel.selectedDate.value)
        assertEquals(YearMonth.from(today), viewModel.displayedMonth.value)
    }

    @Test
    fun symptomCrud_managesSheetStateAndPersistence() = runBlocking {
        // Open add symptom sheet
        viewModel.onOpenAddSymptom()
        assertTrue(viewModel.showAddEditSheet.value)
        assertNull(viewModel.editingSymptom.value)

        // Save a symptom
        viewModel.onSaveSymptom(
            symptomType = "HEADACHE",
            customName = null,
            severity = 3,
            timestamp = 1763737200000L,
            notes = "Tension headache"
        )

        // Let async coroutines run if needed
        val saved = symptomDao.symptoms.firstOrNull()
        assertNotNull(saved)
        assertEquals("HEADACHE", saved?.symptomType)
        assertEquals(3, saved?.severity)
        assertEquals("Tension headache", saved?.notes)

        // Request delete dialog
        viewModel.onRequestDeleteSymptom(saved!!)
        assertEquals(saved, viewModel.symptomToDelete.value)

        viewModel.onConfirmDeleteSymptom()
        assertTrue(symptomDao.symptoms.isEmpty())
        assertNull(viewModel.symptomToDelete.value)
    }

    @Test
    fun editSymptom_updatesExistingRecordWithoutDuplicating() = runBlocking {
        viewModel.onOpenAddSymptom()
        viewModel.onSaveSymptom(
            symptomType = "HEADACHE",
            customName = null,
            severity = 2,
            timestamp = 1763737200000L,
            notes = "Initial note"
        )

        val original = symptomDao.symptoms.single()

        // Open edit sheet and update severity & custom type
        viewModel.onOpenEditSymptom(original)
        assertTrue(viewModel.showAddEditSheet.value)
        assertEquals(original, viewModel.editingSymptom.value)

        viewModel.onSaveSymptom(
            symptomType = "CUSTOM",
            customName = "Neck tension",
            severity = 4,
            timestamp = original.timestamp,
            notes = "Updated note"
        )

        assertEquals(1, symptomDao.symptoms.size)
        val updated = symptomDao.symptoms.single()
        assertEquals(original.id, updated.id)
        assertEquals("CUSTOM", updated.symptomType)
        assertEquals("Neck tension", updated.customName)
        assertEquals(4, updated.severity)
        assertEquals("Updated note", updated.notes)
        assertNull(viewModel.editingSymptom.value)
    }

    @Test
    fun selectMonthYear_clampsDayToShorterMonthLength() {
        viewModel.onSelectDate(LocalDate.of(2026, 1, 31))
        viewModel.onSelectMonthYear(YearMonth.of(2026, 2))

        assertEquals(YearMonth.of(2026, 2), viewModel.displayedMonth.value)
        assertEquals(LocalDate.of(2026, 2, 28), viewModel.selectedDate.value)
    }

    @Test
    fun scheduledDoseQuickActions_markTakenSkippedAndUndo() = runBlocking {
        val today = LocalDate.now()
        val med = Medication(
            id = 10L,
            name = "Metformin",
            dosage = "500",
            unit = "mg",
            frequency = "DAILY",
            isActive = true
        )
        val reminder = MedicationReminder(
            id = 100L,
            medicationId = 10L,
            hour = 8,
            minute = 0,
            daysOfWeekMask = 127,
            isEnabled = true,
            startDate = today.toString()
        )
        medicationDao.medicationsFlow.value = listOf(MedicationWithReminders(med, listOf(reminder)))

        val initialSummary = viewModel.uiState.first { it.daySummaries[today]?.scheduledDoses?.isNotEmpty() == true }
            .daySummaries[today]!!
        val pendingDose = initialSummary.scheduledDoses.single()
        assertEquals(ScheduledDoseStatus.PENDING, pendingDose.status)
        assertEquals("Metformin", pendingDose.medicationName)

        // Mark Taken
        viewModel.onMarkDoseTaken(pendingDose)
        val takenDose = viewModel.uiState.first {
            it.daySummaries[today]?.scheduledDoses?.singleOrNull()?.status == ScheduledDoseStatus.TAKEN
        }.daySummaries[today]!!.scheduledDoses.single()
        assertEquals(ScheduledDoseStatus.TAKEN, takenDose.status)
        assertNotNull(takenDose.doseLogId)

        // Undo reverts back to PENDING
        viewModel.onUndoDoseAction(takenDose)
        val revertedDose = viewModel.uiState.first {
            it.daySummaries[today]?.scheduledDoses?.singleOrNull()?.status == ScheduledDoseStatus.PENDING
        }.daySummaries[today]!!.scheduledDoses.single()
        assertEquals(ScheduledDoseStatus.PENDING, revertedDose.status)
        assertNull(revertedDose.doseLogId)

        // Mark Skipped
        viewModel.onMarkDoseSkipped(revertedDose)
        val skippedDose = viewModel.uiState.first {
            it.daySummaries[today]?.scheduledDoses?.singleOrNull()?.status == ScheduledDoseStatus.SKIPPED
        }.daySummaries[today]!!.scheduledDoses.single()
        assertEquals(ScheduledDoseStatus.SKIPPED, skippedDose.status)
    }
}
