package com.example.healthlog.repository

import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationDao
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import com.example.healthlog.database.MedicationWithReminders
import com.example.healthlog.model.MedicationScheduleResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

class MedicationRepository(
    private val dao: MedicationDao
) {
    val allMedications: Flow<List<MedicationWithReminders>> =
        dao.getAllMedicationsWithReminders()

    val visibleMedications: Flow<List<MedicationWithReminders>> =
        dao.getAllMedicationsWithReminders().map { list ->
            list.filter { !it.medication.isDeleted }
        }

    val activeMedications: Flow<List<MedicationWithReminders>> =
        dao.getActiveMedicationsWithReminders()

    fun getMedicationWithReminders(id: Long): Flow<MedicationWithReminders?> =
        dao.getMedicationWithReminders(id)

    suspend fun getMedicationById(id: Long): Medication? =
        dao.getMedicationById(id)

    suspend fun getAllActiveReminders(): List<MedicationReminder> =
        dao.getAllActiveReminders()

    suspend fun getRemindersForMedication(medicationId: Long): List<MedicationReminder> =
        dao.getRemindersForMedication(medicationId)

    suspend fun getReminderById(id: Long): MedicationReminder? =
        dao.getReminderById(id)

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

    /**
     * Removes a medication from the Medications cabinet, clears all its reminder schedules
     * (and any associated per-day overrides), and deletes any unresolved `UNLOGGED` past rows.
     *
     * - If the medication has any explicitly recorded dose logs (`TAKEN` or `SKIPPED` — whether
     *   from recurring reminders or as-needed/one-time intakes), it is soft-deleted (`isActive = false`,
     *   `isDeleted = true`) so past dose history remains intact on the Calendar and Day Timeline.
     * - If it has no remaining recorded dose logs, the row is hard-deleted from the database.
     */
    suspend fun deleteMedication(medication: Medication) {
        dao.deleteRemindersForMedication(medication.id)
        dao.deleteUnloggedDoseLogsForMedication(medication.id)
        val doseLogCount = dao.getDoseLogCountForMedication(medication.id)
        if (doseLogCount > 0) {
            dao.updateMedication(medication.copy(isActive = false, isDeleted = true))
        } else {
            dao.deleteMedication(medication)
        }
    }

    suspend fun getUnloggedDoseLogCount(medicationId: Long): Int =
        dao.getUnloggedDoseLogCountForMedication(medicationId)

    /**
     * Sweeps any un-swept past days up through `today - 1` and persists
     * [MedicationDoseLog.STATUS_UNLOGGED] rows for active, non-deleted recurring reminders
     * that were neither logged nor cancelled.
     *
     * Returns the new `lastSweptDate` (`today - 1` when swept, or [lastSweptDate] if already
     * up to date) so callers can store it and avoid re-sweeping the same dates.
     */
    suspend fun sweepUnloggedPastDoses(
        today: LocalDate = LocalDate.now(),
        lastSweptDate: LocalDate? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis()
    ): LocalDate = sweepMutex.withLock {
        val yesterday = today.minusDays(1)
        if (lastSweptDate != null && !lastSweptDate.isBefore(yesterday)) {
            return@withLock lastSweptDate
        }

        val activeMeds = dao.getActiveMedicationsWithReminders().first()
        val sweepStart = lastSweptDate?.plusDays(1)
            ?: MedicationScheduleResolver.earliestActiveReminderStartDate(activeMeds, today, zoneId)
            ?: return@withLock yesterday

        if (sweepStart.isAfter(yesterday)) {
            return@withLock yesterday
        }

        val startIso = sweepStart.toString()
        val endIso = yesterday.toString()
        val existingLogs = dao.getDoseLogsInDateRange(startIso, endIso).first()
        val overrides = dao.getOverridesInDateRange(startIso, endIso).first()

        val unloggedDoses = MedicationScheduleResolver.findUnloggedPastDoses(
            medicationsWithReminders = activeMeds,
            existingDoseLogs = existingLogs,
            overrides = overrides,
            today = today,
            lastSweptDate = lastSweptDate,
            zoneId = zoneId,
            nowMillis = nowMillis
        )

        unloggedDoses.forEach { log ->
            dao.insertDoseLog(log)
        }

        yesterday
    }

    fun getDoseLogsForDate(date: String): Flow<List<MedicationDoseLog>> =
        dao.getDoseLogsForDate(date)

    fun getDoseLogsInDateRange(startDate: String, endDate: String): Flow<List<MedicationDoseLog>> =
        dao.getDoseLogsInDateRange(startDate, endDate)

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

    suspend fun getDoseLog(medicationId: Long, scheduledDate: String, scheduledTime: String): MedicationDoseLog? =
        dao.getDoseLog(medicationId, scheduledDate, scheduledTime)

    suspend fun recordAsNeededDose(
        medicationId: Long,
        scheduledDate: String,
        scheduledTime: String,
        status: String = "TAKEN",
        notes: String? = null
    ): Long {
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

    suspend fun deleteDoseById(id: Long) {
        dao.deleteDoseLog(id)
    }

    // --- Single-day & all-future schedule overrides ---

    fun getOverridesForDate(date: String): Flow<List<MedicationScheduleOverride>> =
        dao.getOverridesForDate(date)

    fun getOverridesInDateRange(startDate: String, endDate: String): Flow<List<MedicationScheduleOverride>> =
        dao.getOverridesInDateRange(startDate, endDate)

    suspend fun getOverridesForReminder(reminderId: Long): List<MedicationScheduleOverride> =
        dao.getOverridesForReminder(reminderId)

    suspend fun getOverridesFromDate(fromDate: String): List<MedicationScheduleOverride> =
        dao.getOverridesFromDate(fromDate)

    suspend fun getOverrideById(id: Long): MedicationScheduleOverride? =
        dao.getOverrideById(id)

    /**
     * Moves a recurring reminder's occurrence on [date] ("This day only") to [newHour]:[newMinute].
     * If the new time matches the reminder's base time, any existing override for [date] is cleared
     * and null is returned. Otherwise returns the persisted [MedicationScheduleOverride].
     */
    suspend fun rescheduleDoseForDate(
        reminderId: Long,
        date: String,
        newHour: Int,
        newMinute: Int
    ): MedicationScheduleOverride? {
        val reminder = dao.getReminderById(reminderId) ?: return null
        if (reminder.hour == newHour && reminder.minute == newMinute) {
            dao.deleteOverride(reminderId, date)
            return null
        }

        val override = MedicationScheduleOverride(
            reminderId = reminderId,
            medicationId = reminder.medicationId,
            originalDate = date,
            overrideType = MedicationScheduleOverride.TYPE_RESCHEDULED,
            newHour = newHour,
            newMinute = newMinute
        )
        val savedId = dao.upsertOverride(override)
        return override.copy(id = savedId)
    }

    /**
     * Cancels a recurring reminder's occurrence on [date] ("This day only").
     * Returns the persisted [MedicationScheduleOverride], or null if the reminder does not exist.
     */
    suspend fun cancelDoseForDate(
        reminderId: Long,
        date: String
    ): MedicationScheduleOverride? {
        val reminder = dao.getReminderById(reminderId) ?: return null
        val override = MedicationScheduleOverride(
            reminderId = reminderId,
            medicationId = reminder.medicationId,
            originalDate = date,
            overrideType = MedicationScheduleOverride.TYPE_CANCELLED,
            newHour = null,
            newMinute = null
        )
        val savedId = dao.upsertOverride(override)
        return override.copy(id = savedId)
    }

    suspend fun clearOverrideForDate(reminderId: Long, date: String) {
        dao.deleteOverride(reminderId, date)
    }

    /**
     * Reschedules a recurring reminder for [fromDate] and all future dates ("This & all future days").
     *
     * - If [fromDate] is on or before [today] (or on/before the reminder's startDate), updates the
     *   reminder in place and clears overrides from [fromDate] onward.
     * - If [fromDate] is strictly in the future, caps the existing reminder at `fromDate - 1 day`
     *   and inserts a new reminder starting on [fromDate] at [newHour]:[newMinute].
     *
     * Returns `(updatedOriginalReminder, newlySplitReminder)`.
     */
    suspend fun rescheduleReminderFromDate(
        reminderId: Long,
        fromDate: LocalDate,
        newHour: Int,
        newMinute: Int,
        today: LocalDate = LocalDate.now()
    ): Pair<MedicationReminder?, MedicationReminder?> {
        val reminder = dao.getReminderById(reminderId) ?: return null to null
        val fromIso = fromDate.toString()
        val effectiveStart = reminder.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        dao.deleteOverridesForReminderFromDate(reminderId, fromIso)

        val updateInPlace = !fromDate.isAfter(today) ||
            (effectiveStart != null && !fromDate.isAfter(effectiveStart))

        return if (updateInPlace) {
            val updated = reminder.copy(hour = newHour, minute = newMinute)
            dao.updateReminder(updated)
            updated to null
        } else {
            val cappedOriginal = reminder.copy(endDate = fromDate.minusDays(1).toString())
            dao.updateReminder(cappedOriginal)

            val splitReminder = reminder.copy(
                id = 0,
                hour = newHour,
                minute = newMinute,
                startDate = fromIso,
                endDate = reminder.endDate
            )
            val newId = dao.insertReminder(splitReminder)
            cappedOriginal to splitReminder.copy(id = newId)
        }
    }

    /**
     * Deletes a recurring reminder from [fromDate] onward ("This & all future days").
     *
     * - If [fromDate] is on or before [today] (or on/before the reminder's startDate), removes the
     *   reminder completely (preserving past dose logs on the parent medication) and returns null.
     * - If [fromDate] is strictly in the future, caps the reminder with `endDate = fromDate - 1 day`,
     *   clears overrides from [fromDate] onward, and returns the updated reminder.
     */
    suspend fun deleteReminderFromDate(
        reminderId: Long,
        fromDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): MedicationReminder? {
        val reminder = dao.getReminderById(reminderId) ?: return null
        val fromIso = fromDate.toString()
        val effectiveStart = reminder.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val deleteCompletely = !fromDate.isAfter(today) ||
            (effectiveStart != null && !fromDate.isAfter(effectiveStart))

        return if (deleteCompletely) {
            dao.deleteReminderById(reminderId)
            null
        } else {
            dao.deleteOverridesForReminderFromDate(reminderId, fromIso)
            val capped = reminder.copy(endDate = fromDate.minusDays(1).toString())
            dao.updateReminder(capped)
            capped
        }
    }

    companion object {
        private val sweepMutex = Mutex()
    }
}
