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
import com.example.healthlog.ui.medications.MedicationActions
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
import org.junit.Assert.assertNull
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
        val overrides = mutableListOf<MedicationScheduleOverride>()
        val overridesFlow = MutableStateFlow<List<MedicationScheduleOverride>>(emptyList())
        private var nextDoseId = 1L
        private var nextReminderId = 500L
        private var nextOverrideId = 1L

        private fun notifyDoseChanges() {
            doseLogsFlow.value = doseLogs.toList()
        }

        private fun notifyOverrideChanges() {
            overridesFlow.value = overrides.toList()
        }

        override fun getAllMedicationsWithReminders(): Flow<List<MedicationWithReminders>> = medicationsFlow
        override fun getActiveMedicationsWithReminders(): Flow<List<MedicationWithReminders>> =
            medicationsFlow.map { list -> list.filter { it.medication.isActive && !it.medication.isDeleted } }
        override fun getMedicationWithReminders(id: Long): Flow<MedicationWithReminders?> =
            medicationsFlow.map { list -> list.find { it.medication.id == id } }
        override suspend fun getMedicationById(id: Long): Medication? =
            medicationsFlow.value.find { it.medication.id == id }?.medication
        override suspend fun getAllActiveReminders(): List<MedicationReminder> =
            medicationsFlow.value.filter { it.medication.isActive && !it.medication.isDeleted }.flatMap { it.reminders }
        override suspend fun getRemindersForMedication(medicationId: Long): List<MedicationReminder> =
            medicationsFlow.value.find { it.medication.id == medicationId }?.reminders.orEmpty()
        override suspend fun getReminderById(id: Long): MedicationReminder? =
            medicationsFlow.value.flatMap { it.reminders }.find { it.id == id }
        override suspend fun insertMedication(medication: Medication): Long = 1L
        override suspend fun updateMedication(medication: Medication) {
            medicationsFlow.value = medicationsFlow.value.map { item ->
                if (item.medication.id == medication.id) item.copy(medication = medication) else item
            }
        }
        override suspend fun deleteMedication(medication: Medication) {
            medicationsFlow.value = medicationsFlow.value.filterNot { it.medication.id == medication.id }
        }
        override suspend fun insertReminder(reminder: MedicationReminder): Long {
            val id = if (reminder.id == 0L) nextReminderId++ else reminder.id
            val inserted = reminder.copy(id = id)
            medicationsFlow.value = medicationsFlow.value.map { item ->
                if (item.medication.id == reminder.medicationId) {
                    item.copy(reminders = item.reminders.filterNot { it.id == id } + inserted)
                } else {
                    item
                }
            }
            return id
        }
        override suspend fun insertReminders(reminders: List<MedicationReminder>): List<Long> =
            reminders.map { insertReminder(it) }
        override suspend fun updateReminder(reminder: MedicationReminder) {
            medicationsFlow.value = medicationsFlow.value.map { item ->
                if (item.medication.id == reminder.medicationId) {
                    item.copy(reminders = item.reminders.map { if (it.id == reminder.id) reminder else it })
                } else {
                    item
                }
            }
        }
        override suspend fun deleteReminderById(id: Long) {
            medicationsFlow.value = medicationsFlow.value.map { item ->
                item.copy(reminders = item.reminders.filterNot { it.id == id })
            }
            overrides.removeAll { it.reminderId == id }
            notifyOverrideChanges()
        }
        override suspend fun deleteRemindersForMedication(medicationId: Long) {
            val removedIds = getRemindersForMedication(medicationId).map { it.id }.toSet()
            medicationsFlow.value = medicationsFlow.value.map { item ->
                if (item.medication.id == medicationId) item.copy(reminders = emptyList()) else item
            }
            overrides.removeAll { it.reminderId in removedIds }
            notifyOverrideChanges()
        }
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
        override suspend fun deleteUnloggedDoseLogsForMedication(medicationId: Long) {
            doseLogs.removeAll { it.medicationId == medicationId && it.status == MedicationDoseLog.STATUS_UNLOGGED }
            notifyDoseChanges()
        }
        override fun getDoseLogsForDate(date: String): Flow<List<MedicationDoseLog>> =
            doseLogsFlow.map { list -> list.filter { it.scheduledDate == date } }
        override suspend fun getDoseLog(medicationId: Long, date: String, time: String): MedicationDoseLog? =
            doseLogs.find { it.medicationId == medicationId && it.scheduledDate == date && it.scheduledTime == time }
        override suspend fun getDoseLogCountForMedication(medicationId: Long): Int =
            doseLogs.count { it.medicationId == medicationId }
        override suspend fun getUnloggedDoseLogCountForMedication(medicationId: Long): Int =
            doseLogs.count { it.medicationId == medicationId && it.status == MedicationDoseLog.STATUS_UNLOGGED }
        override fun getDoseLogsInDateRange(startDate: String, endDate: String): Flow<List<MedicationDoseLog>> =
            doseLogsFlow.map { list -> list.filter { it.scheduledDate >= startDate && it.scheduledDate <= endDate } }
        override suspend fun upsertOverride(override: MedicationScheduleOverride): Long {
            overrides.removeAll { it.reminderId == override.reminderId && it.originalDate == override.originalDate }
            val id = if (override.id == 0L) nextOverrideId++ else override.id
            overrides.add(override.copy(id = id))
            notifyOverrideChanges()
            return id
        }
        override suspend fun getOverrideById(id: Long): MedicationScheduleOverride? =
            overrides.find { it.id == id }
        override fun getOverridesForDate(date: String): Flow<List<MedicationScheduleOverride>> =
            overridesFlow.map { list -> list.filter { it.originalDate == date } }
        override fun getOverridesInDateRange(startDate: String, endDate: String): Flow<List<MedicationScheduleOverride>> =
            overridesFlow.map { list -> list.filter { it.originalDate >= startDate && it.originalDate <= endDate } }
        override suspend fun getOverridesForReminder(reminderId: Long): List<MedicationScheduleOverride> =
            overrides.filter { it.reminderId == reminderId }
        override suspend fun getOverridesFromDate(fromDate: String): List<MedicationScheduleOverride> =
            overrides.filter { it.originalDate >= fromDate }
        override suspend fun deleteOverrideById(id: Long) {
            overrides.removeAll { it.id == id }
            notifyOverrideChanges()
        }
        override suspend fun deleteOverride(reminderId: Long, date: String) {
            overrides.removeAll { it.reminderId == reminderId && it.originalDate == date }
            notifyOverrideChanges()
        }
        override suspend fun deleteOverridesForReminderFromDate(reminderId: Long, fromDate: String) {
            overrides.removeAll { it.reminderId == reminderId && it.originalDate >= fromDate }
            notifyOverrideChanges()
        }
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

    @Test
    fun hourSlotChooser_opensPreFilledSymptomOrMedicationSheet() = runBlocking {
        viewModel.onHourSlotClick(15)
        val chooserState = withTimeout(5.seconds) {
            viewModel.uiState.first { it.slotChooserHour == 15 }
        }
        assertEquals(15, chooserState.slotChooserHour)

        viewModel.onChooseLogSymptomFromSlot()
        val symptomSheetState = withTimeout(5.seconds) {
            viewModel.uiState.first { it.showAddEditSymptomSheet }
        }
        assertNull(symptomSheetState.slotChooserHour)
        assertEquals(15, symptomSheetState.symptomDefaultHour)
        viewModel.onDismissAddEditSymptomSheet()

        viewModel.onHourSlotClick(19)
        withTimeout(5.seconds) { viewModel.uiState.first { it.slotChooserHour == 19 } }
        viewModel.onChooseAddMedicationFromSlot()
        val medSheetState = withTimeout(5.seconds) {
            viewModel.uiState.first { it.showAddMedicationSheet }
        }
        assertNull(medSheetState.slotChooserHour)
        assertEquals(19, medSheetState.medicationDefaultHour)
    }

    @Test
    fun moveRecurringDose_thisDayOnly_movesOnlySelectedDateAndPreservesNextDay() = runBlocking {
        val med = Medication(id = 10L, name = "Metformin", dosage = "500", unit = "mg", frequency = "DAILY", isActive = true)
        val reminder = MedicationReminder(id = 100L, medicationId = 10L, hour = 9, minute = 0, daysOfWeekMask = 127, isEnabled = true, startDate = testDate.toString())
        medicationDao.medicationsFlow.value = listOf(MedicationWithReminders(med, listOf(reminder)))

        val initial = withTimeout(5.seconds) { viewModel.uiState.first { it.medsCount == 1 } }
        val dose = (initial.hourSlots[9].events.single() as TimelineEvent.Medication).doseItem

        viewModel.onRequestMoveDose(dose)
        withTimeout(5.seconds) { viewModel.uiState.first { it.doseToMove == dose } }

        viewModel.onPickMoveTime(11, 30)
        val pendingState = withTimeout(5.seconds) { viewModel.uiState.first { it.pendingDoseMove != null } }
        val pendingMove = requireNotNull(pendingState.pendingDoseMove)
        assertEquals(11, pendingMove.newHour)
        assertEquals(30, pendingMove.newMinute)

        viewModel.onConfirmMoveScope(MedicationActions.RecurringDoseScope.THIS_DAY_ONLY)
        val movedState = withTimeout(5.seconds) {
            viewModel.uiState.first { it.hourSlots[11].isPopulated && !it.hourSlots[9].isPopulated }
        }
        val movedEvent = movedState.hourSlots[11].events.single() as TimelineEvent.Medication
        assertEquals("11:30", movedEvent.doseItem.scheduledTime)
        assertEquals("09:00", movedEvent.doseItem.originalTime)

        // Tomorrow still keeps the original 09:00 schedule
        viewModel.onNextDay()
        val nextDayState = withTimeout(5.seconds) {
            viewModel.uiState.first { it.selectedDate == testDate.plusDays(1) && it.hourSlots[9].isPopulated }
        }
        assertEquals("09:00", (nextDayState.hourSlots[9].events.single() as TimelineEvent.Medication).doseItem.scheduledTime)
    }

    @Test
    fun moveRecurringDose_thisAndFutureOnFutureDate_splitsReminder() = runBlocking {
        val med = Medication(id = 10L, name = "Amlodipine", dosage = "5", unit = "mg", frequency = "DAILY", isActive = true)
        val reminder = MedicationReminder(id = 100L, medicationId = 10L, hour = 8, minute = 0, daysOfWeekMask = 127, isEnabled = true, startDate = testDate.toString())
        medicationDao.medicationsFlow.value = listOf(MedicationWithReminders(med, listOf(reminder)))

        // Navigate to tomorrow (future date relative to todayProvider = testDate)
        viewModel.onNextDay()
        val tomorrowState = withTimeout(5.seconds) {
            viewModel.uiState.first { it.selectedDate == testDate.plusDays(1) && it.hourSlots[8].isPopulated }
        }
        val tomorrowDose = (tomorrowState.hourSlots[8].events.single() as TimelineEvent.Medication).doseItem

        viewModel.onRequestMoveDose(tomorrowDose)
        viewModel.onPickMoveTime(10, 15)
        viewModel.onConfirmMoveScope(MedicationActions.RecurringDoseScope.THIS_AND_FUTURE)

        val movedTomorrow = withTimeout(5.seconds) {
            viewModel.uiState.first { it.hourSlots[10].isPopulated && !it.hourSlots[8].isPopulated }
        }
        assertEquals("10:15", (movedTomorrow.hourSlots[10].events.single() as TimelineEvent.Medication).doseItem.scheduledTime)

        // Today still keeps 08:00 because the reminder was split at tomorrow
        viewModel.onPreviousDay()
        val todayState = withTimeout(5.seconds) {
            viewModel.uiState.first { it.selectedDate == testDate && it.hourSlots[8].isPopulated }
        }
        assertEquals("08:00", (todayState.hourSlots[8].events.single() as TimelineEvent.Medication).doseItem.scheduledTime)
    }

    @Test
    fun deleteRecurringDose_thisDayOnlyAndAllFuture_behaveCorrectly() = runBlocking {
        val med = Medication(id = 10L, name = "Vitamin D", dosage = "1000", unit = "IU", frequency = "DAILY", isActive = true)
        val reminder = MedicationReminder(id = 100L, medicationId = 10L, hour = 9, minute = 0, daysOfWeekMask = 127, isEnabled = true, startDate = testDate.toString())
        medicationDao.medicationsFlow.value = listOf(MedicationWithReminders(med, listOf(reminder)))

        val initial = withTimeout(5.seconds) { viewModel.uiState.first { it.medsCount == 1 } }
        val doseToday = (initial.hourSlots[9].events.single() as TimelineEvent.Medication).doseItem

        // Delete today only -> cancelled on testDate, still present tomorrow
        viewModel.onRequestDeleteDose(doseToday)
        viewModel.onConfirmDeleteDose(MedicationActions.RecurringDoseScope.THIS_DAY_ONLY)
        withTimeout(5.seconds) { viewModel.uiState.first { it.medsCount == 0 } }

        viewModel.onNextDay()
        val tomorrowState = withTimeout(5.seconds) {
            viewModel.uiState.first { it.selectedDate == testDate.plusDays(1) && it.medsCount == 1 }
        }
        val doseTomorrow = (tomorrowState.hourSlots[9].events.single() as TimelineEvent.Medication).doseItem

        // Delete tomorrow & all future -> empty on tomorrow and day after
        viewModel.onRequestDeleteDose(doseTomorrow)
        viewModel.onConfirmDeleteDose(MedicationActions.RecurringDoseScope.THIS_AND_FUTURE)
        withTimeout(5.seconds) { viewModel.uiState.first { it.medsCount == 0 } }

        viewModel.onNextDay()
        val dayAfter = withTimeout(5.seconds) {
            viewModel.uiState.first { it.selectedDate == testDate.plusDays(2) }
        }
        assertEquals(0, dayAfter.medsCount)
    }

    @Test
    fun oneTimeDose_moveAndDelete_updatesLogDirectlyWithoutScopeDialog() = runBlocking {
        val med = Medication(id = 20L, name = "Ibuprofen", dosage = "400", unit = "mg", frequency = "ONE_TIME", isActive = true)
        medicationDao.medicationsFlow.value = listOf(MedicationWithReminders(med, emptyList()))
        medicationDao.insertDoseLog(
            MedicationDoseLog(
                medicationId = 20L,
                scheduledDate = testDate.toString(),
                scheduledTime = "13:00",
                status = "TAKEN"
            )
        )

        val initial = withTimeout(5.seconds) { viewModel.uiState.first { it.hourSlots[13].isPopulated } }
        val oneTimeDose = (initial.hourSlots[13].events.single() as TimelineEvent.Medication).doseItem
        assertFalse(oneTimeDose.isRecurring)

        // Move one-time dose directly to 16:45 (no scope dialog needed)
        viewModel.onRequestMoveDose(oneTimeDose)
        viewModel.onPickMoveTime(16, 45)
        val movedState = withTimeout(5.seconds) {
            viewModel.uiState.first { it.hourSlots[16].isPopulated && !it.hourSlots[13].isPopulated }
        }
        assertNull(movedState.pendingDoseMove)
        val movedDose = (movedState.hourSlots[16].events.single() as TimelineEvent.Medication).doseItem
        assertEquals("16:45", movedDose.scheduledTime)

        // Delete one-time dose removes the single log
        viewModel.onRequestDeleteDose(movedDose)
        viewModel.onConfirmDeleteDose(MedicationActions.RecurringDoseScope.THIS_DAY_ONLY)
        val deletedState = withTimeout(5.seconds) { viewModel.uiState.first { it.medsCount == 0 } }
        assertEquals(0, deletedState.medsCount)
    }
}
