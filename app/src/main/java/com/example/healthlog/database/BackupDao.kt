package com.example.healthlog.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Direct database access for full-database backup export and atomic restore.
 */
@Dao
interface BackupDao {

    // --- Export queries ---

    @Query("SELECT * FROM measurements ORDER BY id ASC")
    suspend fun getAllMeasurements(): List<Measurement>

    @Query("SELECT * FROM medications ORDER BY id ASC")
    suspend fun getAllMedications(): List<Medication>

    @Query("SELECT * FROM medication_reminders ORDER BY id ASC")
    suspend fun getAllMedicationReminders(): List<MedicationReminder>

    @Query("SELECT * FROM medication_schedule_overrides ORDER BY id ASC")
    suspend fun getAllScheduleOverrides(): List<MedicationScheduleOverride>

    @Query("SELECT * FROM medication_dose_logs ORDER BY id ASC")
    suspend fun getAllDoseLogs(): List<MedicationDoseLog>

    @Query("SELECT * FROM symptoms ORDER BY id ASC")
    suspend fun getAllSymptoms(): List<SymptomEntry>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    // --- Clear queries for clean restore ---

    @Query("DELETE FROM measurements")
    suspend fun clearMeasurements()

    @Query("DELETE FROM medication_schedule_overrides")
    suspend fun clearScheduleOverrides()

    @Query("DELETE FROM medication_dose_logs")
    suspend fun clearDoseLogs()

    @Query("DELETE FROM medication_reminders")
    suspend fun clearMedicationReminders()

    @Query("DELETE FROM medications")
    suspend fun clearMedications()

    @Query("DELETE FROM symptoms")
    suspend fun clearSymptoms()

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    // --- Bulk restore queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(measurements: List<Measurement>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<Medication>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationReminders(reminders: List<MedicationReminder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleOverrides(overrides: List<MedicationScheduleOverride>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseLogs(doseLogs: List<MedicationDoseLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(symptoms: List<SymptomEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)
}
