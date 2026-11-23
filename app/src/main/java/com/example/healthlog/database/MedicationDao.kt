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
    @Query("SELECT * FROM medications WHERE isActive = 1 AND isDeleted = 0 ORDER BY name ASC")
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

    @Query("SELECT * FROM medication_reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): MedicationReminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<MedicationReminder>): List<Long>

    @Update
    suspend fun updateReminder(reminder: MedicationReminder)

    @Query("DELETE FROM medication_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

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

    @Query("SELECT * FROM medication_dose_logs WHERE medicationId = :medicationId AND scheduledDate = :date AND scheduledTime = :time LIMIT 1")
    suspend fun getDoseLog(medicationId: Long, date: String, time: String): MedicationDoseLog?

    @Query("SELECT COUNT(*) FROM medication_dose_logs WHERE medicationId = :medicationId")
    suspend fun getDoseLogCountForMedication(medicationId: Long): Int

    // All dose logs within inclusive ISO-8601 calendar dates [startDate, endDate].
    @Query(
        "SELECT * FROM medication_dose_logs WHERE scheduledDate >= :startDate AND scheduledDate <= :endDate " +
            "ORDER BY scheduledDate DESC, scheduledTime DESC, actionTimestamp DESC"
    )
    fun getDoseLogsInDateRange(startDate: String, endDate: String): Flow<List<MedicationDoseLog>>

    // --- Single-day schedule overrides ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: MedicationScheduleOverride): Long

    @Query("SELECT * FROM medication_schedule_overrides WHERE id = :id LIMIT 1")
    suspend fun getOverrideById(id: Long): MedicationScheduleOverride?

    @Query("SELECT * FROM medication_schedule_overrides WHERE originalDate = :date")
    fun getOverridesForDate(date: String): Flow<List<MedicationScheduleOverride>>

    @Query("SELECT * FROM medication_schedule_overrides WHERE originalDate >= :startDate AND originalDate <= :endDate")
    fun getOverridesInDateRange(startDate: String, endDate: String): Flow<List<MedicationScheduleOverride>>

    @Query("SELECT * FROM medication_schedule_overrides WHERE reminderId = :reminderId")
    suspend fun getOverridesForReminder(reminderId: Long): List<MedicationScheduleOverride>

    @Query("SELECT * FROM medication_schedule_overrides WHERE originalDate >= :fromDate")
    suspend fun getOverridesFromDate(fromDate: String): List<MedicationScheduleOverride>

    @Query("DELETE FROM medication_schedule_overrides WHERE id = :id")
    suspend fun deleteOverrideById(id: Long)

    @Query("DELETE FROM medication_schedule_overrides WHERE reminderId = :reminderId AND originalDate = :date")
    suspend fun deleteOverride(reminderId: Long, date: String)

    @Query("DELETE FROM medication_schedule_overrides WHERE reminderId = :reminderId AND originalDate >= :fromDate")
    suspend fun deleteOverridesForReminderFromDate(reminderId: Long, fromDate: String)
}
