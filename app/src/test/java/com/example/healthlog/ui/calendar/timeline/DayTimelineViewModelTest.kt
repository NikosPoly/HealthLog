package com.example.healthlog.ui.calendar.timeline

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds

class DayTimelineViewModelTest {

    private class FakeMeasurementDao : MeasurementDao {
        val measurements = mutableListOf<Measurement>()
        val flow = MutableStateFlow<List<Measurement>>(emptyList())

        private fun notifyChanges() {
            flow.value = measurements.toList()
        }

        override suspend fun insertAll(measurements: List<Measurement>) {
            this.measurements.addAll(measurements)
            notifyChanges()
        }

        override suspend fun delete(measurement: Measurement) {
            this.measurements.remove(measurement)
            notifyChanges()
        }

        override fun getMeasurementsByType(type: MeasurementType): Flow<List<Measurement>> =
            flow.map { list -> list.filter { it.type == type } }

        override fun getMeasurementById(id: Long): Flow<Measurement?> =
            flow.map { list -> list.find { it.id == id } }

        override fun getLatestHeight(): Flow<Double?> = MutableStateFlow(175.0)

        override fun getMeasurementsInRange(startMillis: Long, endMillis: Long): Flow<List<Measurement>> =
            flow.map { list -> list.filter { it.timestamp in startMillis until endMillis } }
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
            medicationsFlow.map { list -> list.filter { it.medication.isActive } }
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

    private val fixedZone = ZoneId.of("UTC")
    private lateinit var measurementDao: FakeMeasurementDao
    private lateinit var medicationDao: FakeMedicationDao
    private lateinit var symptomDao: FakeSymptomDao
    private lateinit var viewModel: DayTimelineViewModel
    private val testDate = LocalDate.of(2026, 11, 20)

    @Before
    fun setUp() {
        measurementDao = FakeMeasurementDao()
        medicationDao = FakeMedicationDao()
        symptomDao = FakeSymptomDao()

        viewModel = DayTimelineViewModel(
            initialDate = testDate,
            measurementRepository = MeasurementRepository(measurementDao),
            medicationRepository = MedicationRepository(medicationDao),
            symptomRepository = SymptomRepository(symptomDao),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            zoneId = fixedZone,
            todayProvider = { testDate }
        )
    }

    @Test
    fun initialSlots_generateAll24Hours() = runBlocking {
        val state = withTimeout(5.seconds) {
            viewModel.uiState.first { it.hourSlots.size == 24 }
        }
        assertEquals(24, state.hourSlots.size)
        assertEquals("00:00", state.hourSlots.first().timeLabel)
        assertEquals("23:00", state.hourSlots.last().timeLabel)
        assertEquals(0, state.totalEventCount)
    }

    @Test
    fun eventsGroupingAndFiltering_distributeIntoHourSlots() = runBlocking {
        val vitalTs = testDate.atTime(8, 15).atZone(fixedZone).toInstant().toEpochMilli()
        measurementDao.insertAll(
            listOf(
                Measurement(
                    id = 1L,
                    type = MeasurementType.BLOOD_PRESSURE,
                    value = 120.0,
                    secondaryValue = 80.0,
                    tertiaryValue = 70.0,
                    timestamp = vitalTs
                )
            )
        )

        val med = Medication(id = 2L, name = "Aspirin", dosage = "100", unit = "mg", frequency = "DAILY", isActive = true)
        val reminder = MedicationReminder(id = 20L, medicationId = 2L, hour = 8, minute = 30, daysOfWeekMask = 127, isEnabled = true, startDate = "2026-11-01")
        medicationDao.medicationsFlow.value = listOf(MedicationWithReminders(med, listOf(reminder)))

        val symptomTs = testDate.atTime(14, 45).atZone(fixedZone).toInstant().toEpochMilli()
        symptomDao.insert(
            SymptomEntry(
                id = 3L,
                symptomType = "HEADACHE",
                severity = 3,
                timestamp = symptomTs
            )
        )

        val state = withTimeout(5.seconds) {
            viewModel.uiState.first { it.totalEventCount == 3 }
        }
        assertEquals(3, state.totalEventCount)
        assertEquals(1, state.vitalsCount)
        assertEquals(1, state.medsCount)
        assertEquals(1, state.symptomsCount)

        // Hour 8 has 2 events (Vital and Medication)
        val slot8 = state.hourSlots[8]
        assertTrue(slot8.isPopulated)
        assertEquals(2, slot8.events.size)
        assertTrue(slot8.events[0] is TimelineEvent.Vital)
        assertTrue(slot8.events[1] is TimelineEvent.Medication)

        // Hour 14 has 1 event (Symptom)
        val slot14 = state.hourSlots[14]
        assertTrue(slot14.isPopulated)
        assertEquals(1, slot14.events.size)
        assertTrue(slot14.events.single() is TimelineEvent.Symptom)

        // Empty hour has 0 events
        assertFalse(state.hourSlots[0].isPopulated)

        // Test filtering: VITALS only
        viewModel.onSelectFilter(TimelineFilter.VITALS)
        val vitalsOnlyState = withTimeout(5.seconds) {
            viewModel.uiState.first { it.activeFilter == TimelineFilter.VITALS }
        }
        assertEquals(1, vitalsOnlyState.hourSlots[8].events.size)
        assertTrue(vitalsOnlyState.hourSlots[8].events.single() is TimelineEvent.Vital)
        assertFalse(vitalsOnlyState.hourSlots[14].isPopulated)

        // Test filtering: SYMPTOMS only
        viewModel.onSelectFilter(TimelineFilter.SYMPTOMS)
        val symptomsOnlyState = withTimeout(5.seconds) {
            viewModel.uiState.first { it.activeFilter == TimelineFilter.SYMPTOMS }
        }
        assertFalse(symptomsOnlyState.hourSlots[8].isPopulated)
        assertEquals(1, symptomsOnlyState.hourSlots[14].events.size)
    }

    @Test
    fun navigationBetweenDays_updatesSelectedDate() {
        assertEquals(testDate, viewModel.selectedDate.value)

        viewModel.onNextDay()
        assertEquals(testDate.plusDays(1), viewModel.selectedDate.value)

        viewModel.onPreviousDay()
        assertEquals(testDate, viewModel.selectedDate.value)

        viewModel.onNextDay()
        viewModel.onJumpToToday()
        assertEquals(testDate, viewModel.selectedDate.value)
    }

    @Test
    fun doseQuickActions_markTakenSkippedAndUndo() = runBlocking {
        val med = Medication(id = 10L, name = "Metformin", dosage = "500", unit = "mg", frequency = "DAILY", isActive = true)
        val reminder = MedicationReminder(id = 100L, medicationId = 10L, hour = 9, minute = 0, daysOfWeekMask = 127, isEnabled = true, startDate = testDate.toString())
        medicationDao.medicationsFlow.value = listOf(MedicationWithReminders(med, listOf(reminder)))

        val state = withTimeout(5.seconds) {
            viewModel.uiState.first { it.medsCount == 1 }
        }
        val medEvent = state.hourSlots[9].events.single() as TimelineEvent.Medication
        assertEquals(ScheduledDoseStatus.PENDING, medEvent.doseItem.status)

        viewModel.onMarkDoseTaken(medEvent.doseItem)
        val takenState = withTimeout(5.seconds) {
            viewModel.uiState.first {
                val event = it.hourSlots[9].events.singleOrNull() as? TimelineEvent.Medication
                event?.doseItem?.status == ScheduledDoseStatus.TAKEN
            }
        }
        val takenDose = (takenState.hourSlots[9].events.single() as TimelineEvent.Medication).doseItem
        assertEquals(ScheduledDoseStatus.TAKEN, takenDose.status)

        viewModel.onUndoDose(takenDose)
        val undoneState = withTimeout(5.seconds) {
            viewModel.uiState.first {
                val event = it.hourSlots[9].events.singleOrNull() as? TimelineEvent.Medication
                event?.doseItem?.status == ScheduledDoseStatus.PENDING
            }
        }
        val undoneDose = (undoneState.hourSlots[9].events.single() as TimelineEvent.Medication).doseItem
        assertEquals(ScheduledDoseStatus.PENDING, undoneDose.status)

        viewModel.onMarkDoseSkipped(undoneDose)
        val skippedState = withTimeout(5.seconds) {
            viewModel.uiState.first {
                val event = it.hourSlots[9].events.singleOrNull() as? TimelineEvent.Medication
                event?.doseItem?.status == ScheduledDoseStatus.SKIPPED
            }
        }
        val skippedDose = (skippedState.hourSlots[9].events.single() as TimelineEvent.Medication).doseItem
        assertEquals(ScheduledDoseStatus.SKIPPED, skippedDose.status)
    }
}
