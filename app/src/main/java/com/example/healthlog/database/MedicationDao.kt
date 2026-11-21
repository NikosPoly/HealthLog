package com.example.healthlog.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Transaction
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedicationsWithReminders(): Flow<List<MedicationWithReminders>>

    @Transaction
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveMedicationsWithReminders(): Flow<List<MedicationWithReminders>>

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    fun getMedicationWithReminders(id: Long): Flow<MedicationWithReminders?>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: Long): Medication?

    @Query("SELECT * FROM medication_reminders WHERE isEnabled = 1")
    suspend fun getAllActiveReminders(): List<MedicationReminder>

    @Query("SELECT * FROM medication_reminders WHERE medicationId = :medicationId")
    suspend fun getRemindersForMedication(medicationId: Long): List<MedicationReminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<MedicationReminder>): List<Long>

    @Query("DELETE FROM medication_reminders WHERE medicationId = :medicationId")
    suspend fun deleteRemindersForMedication(medicationId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseLog(log: MedicationDoseLog): Long

    @Query("DELETE FROM medication_dose_logs WHERE id = :id")
    suspend fun deleteDoseLog(id: Long)

    @Query("DELETE FROM medication_dose_logs WHERE medicationId = :medicationId AND scheduledDate = :date AND scheduledTime = :time")
    suspend fun deleteDoseLog(medicationId: Long, date: String, time: String)

    @Query("SELECT * FROM medication_dose_logs WHERE scheduledDate = :date")
    fun getDoseLogsForDate(date: String): Flow<List<MedicationDoseLog>>
}
