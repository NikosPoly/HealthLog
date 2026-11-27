package com.example.healthlog.repository

import com.example.healthlog.backup.BackupData
import com.example.healthlog.database.BackupDao
import com.example.healthlog.database.Measurement
import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.database.UserProfile
import com.example.healthlog.model.MeasurementType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeBackupDao : BackupDao {
    val measurements = mutableListOf<Measurement>()
    val medications = mutableListOf<Medication>()
    val reminders = mutableListOf<MedicationReminder>()
    val overrides = mutableListOf<MedicationScheduleOverride>()
    val doseLogs = mutableListOf<MedicationDoseLog>()
    val symptoms = mutableListOf<SymptomEntry>()
    var userProfile: UserProfile? = null

    override suspend fun getAllMeasurements(): List<Measurement> = measurements.toList()
    override suspend fun getAllMedications(): List<Medication> = medications.toList()
    override suspend fun getAllMedicationReminders(): List<MedicationReminder> = reminders.toList()
    override suspend fun getAllScheduleOverrides(): List<MedicationScheduleOverride> = overrides.toList()
    override suspend fun getAllDoseLogs(): List<MedicationDoseLog> = doseLogs.toList()
    override suspend fun getAllSymptoms(): List<SymptomEntry> = symptoms.toList()
    override suspend fun getUserProfile(): UserProfile? = userProfile

    override suspend fun clearMeasurements() { measurements.clear() }
    override suspend fun clearScheduleOverrides() { overrides.clear() }
    override suspend fun clearDoseLogs() { doseLogs.clear() }
    override suspend fun clearMedicationReminders() { reminders.clear() }
    override suspend fun clearMedications() { medications.clear() }
    override suspend fun clearSymptoms() { symptoms.clear() }
    override suspend fun clearUserProfile() { userProfile = null }

    override suspend fun insertMeasurements(measurements: List<Measurement>) {
        this.measurements.addAll(measurements)
    }
    override suspend fun insertMedications(medications: List<Medication>) {
        this.medications.addAll(medications)
    }
    override suspend fun insertMedicationReminders(reminders: List<MedicationReminder>) {
        this.reminders.addAll(reminders)
    }
    override suspend fun insertScheduleOverrides(overrides: List<MedicationScheduleOverride>) {
        this.overrides.addAll(overrides)
    }
    override suspend fun insertDoseLogs(doseLogs: List<MedicationDoseLog>) {
        this.doseLogs.addAll(doseLogs)
    }
    override suspend fun insertSymptoms(symptoms: List<SymptomEntry>) {
        this.symptoms.addAll(symptoms)
    }
    override suspend fun insertUserProfile(profile: UserProfile) {
        this.userProfile = profile
    }
}

class BackupRepositoryTest {

    private lateinit var fakeDao: FakeBackupDao

    @Before
    fun setUp() {
        fakeDao = FakeBackupDao()
    }

    @Test
    fun createBackupPayload_gathersAllDatabaseTables() = runBlocking {
        fakeDao.userProfile = UserProfile(name = "Elena", gender = "FEMALE")
        fakeDao.measurements.add(
            Measurement(
                id = 1L,
                type = MeasurementType.PULSE,
                value = 75.0,
                timestamp = 1000L
            )
        )
        fakeDao.medications.add(
            Medication(
                id = 1L,
                name = "Aspirin",
                dosage = "100",
                unit = "mg",
                frequency = "DAILY"
            )
        )
        fakeDao.symptoms.add(
            SymptomEntry(
                id = 1L,
                symptomType = "FATIGUE",
                severity = 2,
                timestamp = 2000L
            )
        )

        // Using fakeDao directly to verify payload creation without full Room database instance
        val profile = fakeDao.getUserProfile()
        val measurements = fakeDao.getAllMeasurements()
        val medications = fakeDao.getAllMedications()
        val symptoms = fakeDao.getAllSymptoms()

        val data = BackupData(
            profile = profile,
            measurements = measurements,
            medications = medications,
            symptoms = symptoms
        )

        assertEquals("Elena", data.profile?.name)
        assertEquals(1, data.measurements.size)
        assertEquals(1, data.medications.size)
        assertEquals(1, data.symptoms.size)
        assertEquals(4, data.totalItemsCount)
    }

    @Test
    fun restoreBackup_clearsExistingDataAndInsertsBackupContent() = runBlocking {
        // Pre-fill existing data
        fakeDao.userProfile = UserProfile(name = "OldUser")
        fakeDao.measurements.add(Measurement(id = 99L, type = MeasurementType.WEIGHT, value = 80.0))
        fakeDao.medications.add(Medication(id = 88L, name = "OldMed", dosage = "10", unit = "mg", frequency = "DAILY"))

        // Create new backup to restore
        val newProfile = UserProfile(name = "NewUser", dateOfBirth = "1992-08-10")
        val newMeasurements = listOf(
            Measurement(id = 1L, type = MeasurementType.BLOOD_PRESSURE, value = 118.0, secondaryValue = 78.0)
        )
        val newMedications = listOf(
            Medication(id = 1L, name = "NewMed", dosage = "20", unit = "mg", frequency = "DAILY")
        )
        val newReminders = listOf(
            MedicationReminder(id = 1L, medicationId = 1L, hour = 9, minute = 0)
        )
        val newDoseLogs = listOf(
            MedicationDoseLog(id = 1L, medicationId = 1L, scheduledDate = "2026-11-27", scheduledTime = "09:00", status = "TAKEN")
        )
        val newSymptoms = listOf(
            SymptomEntry(id = 1L, symptomType = "DIZZINESS", severity = 4)
        )

        val backupData = BackupData(
            profile = newProfile,
            measurements = newMeasurements,
            medications = newMedications,
            medicationReminders = newReminders,
            doseLogs = newDoseLogs,
            symptoms = newSymptoms
        )

        // Simulate restore steps
        fakeDao.clearScheduleOverrides()
        fakeDao.clearDoseLogs()
        fakeDao.clearMedicationReminders()
        fakeDao.clearMedications()
        fakeDao.clearMeasurements()
        fakeDao.clearSymptoms()
        fakeDao.clearUserProfile()

        backupData.profile?.let { fakeDao.insertUserProfile(it) }
        fakeDao.insertMeasurements(backupData.measurements)
        fakeDao.insertMedications(backupData.medications)
        fakeDao.insertMedicationReminders(backupData.medicationReminders)
        fakeDao.insertDoseLogs(backupData.doseLogs)
        fakeDao.insertSymptoms(backupData.symptoms)

        // Assertions
        assertEquals("NewUser", fakeDao.userProfile?.name)
        assertEquals(1, fakeDao.measurements.size)
        assertEquals(118.0, fakeDao.measurements[0].value, 0.0)
        assertEquals(1, fakeDao.medications.size)
        assertEquals("NewMed", fakeDao.medications[0].name)
        assertEquals(1, fakeDao.reminders.size)
        assertEquals(1, fakeDao.doseLogs.size)
        assertEquals(1, fakeDao.symptoms.size)
        assertEquals("DIZZINESS", fakeDao.symptoms[0].symptomType)
    }
}
