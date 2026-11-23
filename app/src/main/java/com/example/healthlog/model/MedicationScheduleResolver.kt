package com.example.healthlog.model

import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import com.example.healthlog.database.MedicationWithReminders
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

enum class ScheduledDoseStatus {
    PENDING,
    TAKEN,
    SKIPPED
}

data class ScheduledDoseItem(
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val unit: String,
    val instructions: String?,
    val scheduledDate: String,
    val scheduledTime: String,
    val hour: Int,
    val minute: Int,
    val status: ScheduledDoseStatus,
    val isRecurring: Boolean,
    val reminderId: Long? = null,
    val overrideId: Long? = null,
    val originalTime: String? = null,
    val doseLogId: Long? = null
)

object MedicationScheduleResolver {

    fun formatTime(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", hour, minute)

    fun parseTimeOrNull(time: String): Pair<Int, Int>? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h to m
    }

    fun parseIsoDateOrNull(dateString: String?): LocalDate? {
        if (dateString.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(dateString) }.getOrNull()
    }

    /**
     * Bitmask mapping matching [MedicationReminder.daysOfWeekMask]:
     * 1=Mon, 2=Tue, 4=Wed, 8=Thu, 16=Fri, 32=Sat, 64=Sun.
     */
    fun dayOfWeekBit(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 4
        DayOfWeek.THURSDAY -> 8
        DayOfWeek.FRIDAY -> 16
        DayOfWeek.SATURDAY -> 32
        DayOfWeek.SUNDAY -> 64
    }

    fun matchesDayOfWeek(date: LocalDate, daysOfWeekMask: Int): Boolean {
        return (daysOfWeekMask and dayOfWeekBit(date.dayOfWeek)) != 0
    }

    fun isReminderActiveOnDate(
        reminder: MedicationReminder,
        medicationCreatedAt: Long,
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        if (!reminder.isEnabled) return false
        if (!matchesDayOfWeek(date, reminder.daysOfWeekMask)) return false

        val startDate = parseIsoDateOrNull(reminder.startDate)
            ?: Instant.ofEpochMilli(medicationCreatedAt).atZone(zoneId).toLocalDate()
        if (date.isBefore(startDate)) return false

        val endDate = parseIsoDateOrNull(reminder.endDate)
        if (endDate != null && date.isAfter(endDate)) return false

        return true
    }

    /**
     * Resolves the full list of medication items for [date]:
     * - On [today] and future dates: projects enabled reminders of active medications (honoring
     *   `startDate`, `endDate`, `daysOfWeekMask`, and per-date [overrides]) merged with persisted [doseLogs].
     * - On past dates (`date < today`): returns persisted [doseLogs] only so historical logs are never
     *   polluted with retroactive pending reminders.
     * - Always includes unconsumed one-time / as-needed / paused medication dose logs for [date].
     */
    fun resolveForDate(
        date: LocalDate,
        medicationsWithReminders: List<MedicationWithReminders>,
        doseLogs: List<MedicationDoseLog>,
        overrides: List<MedicationScheduleOverride>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ScheduledDoseItem> {
        val dateIso = date.toString()
        val medMap = medicationsWithReminders.associateBy { it.medication.id }
        val dayLogs = doseLogs.filter { it.scheduledDate == dateIso }
        val dayOverridesByReminder = overrides
            .filter { it.originalDate == dateIso }
            .associateBy { it.reminderId }

        val result = mutableListOf<ScheduledDoseItem>()
        val consumedLogIds = mutableSetOf<Long>()

        if (!date.isBefore(today)) {
            medicationsWithReminders
                .filter { it.medication.isActive }
                .forEach { medWithReminders ->
                    val med = medWithReminders.medication
                    medWithReminders.reminders.forEach { reminder ->
                        if (!isReminderActiveOnDate(reminder, med.createdAt, date, zoneId)) {
                            return@forEach
                        }

                        val override = dayOverridesByReminder[reminder.id]
                        if (override?.overrideType == MedicationScheduleOverride.TYPE_CANCELLED) {
                            return@forEach
                        }

                        val rescheduledHour = if (override?.overrideType == MedicationScheduleOverride.TYPE_RESCHEDULED) override.newHour else null
                        val rescheduledMinute = if (override?.overrideType == MedicationScheduleOverride.TYPE_RESCHEDULED) override.newMinute else null
                        val isRescheduled = rescheduledHour != null && rescheduledMinute != null
                        val effectiveHour = rescheduledHour ?: reminder.hour
                        val effectiveMinute = rescheduledMinute ?: reminder.minute
                        val effectiveTime = formatTime(effectiveHour, effectiveMinute)
                        val baseTime = formatTime(reminder.hour, reminder.minute)

                        val matchingLog = dayLogs.firstOrNull { log ->
                            log.id !in consumedLogIds &&
                                log.medicationId == med.id &&
                                log.scheduledTime == effectiveTime
                        }

                        if (matchingLog != null) {
                            consumedLogIds.add(matchingLog.id)
                        }

                        val status = when (matchingLog?.status) {
                            "TAKEN" -> ScheduledDoseStatus.TAKEN
                            "SKIPPED" -> ScheduledDoseStatus.SKIPPED
                            else -> ScheduledDoseStatus.PENDING
                        }

                        result.add(
                            ScheduledDoseItem(
                                medicationId = med.id,
                                medicationName = med.name,
                                dosage = med.dosage,
                                unit = med.unit,
                                instructions = med.instructions,
                                scheduledDate = dateIso,
                                scheduledTime = effectiveTime,
                                hour = effectiveHour,
                                minute = effectiveMinute,
                                status = status,
                                isRecurring = true,
                                reminderId = reminder.id,
                                overrideId = if (isRescheduled) override?.id else null,
                                originalTime = if (isRescheduled) baseTime else null,
                                doseLogId = matchingLog?.id
                            )
                        )
                    }
                }
        }

        // Append any remaining dose logs (one-timers, as-needed doses, past records, or logs of paused meds)
        dayLogs
            .filter { it.id !in consumedLogIds }
            .forEach { log ->
                val med = medMap[log.medicationId]?.medication
                val (hour, minute) = parseTimeOrNull(log.scheduledTime) ?: (0 to 0)
                val status = if (log.status == "SKIPPED") {
                    ScheduledDoseStatus.SKIPPED
                } else {
                    ScheduledDoseStatus.TAKEN
                }
                result.add(
                    ScheduledDoseItem(
                        medicationId = log.medicationId,
                        medicationName = med?.name ?: "",
                        dosage = med?.dosage ?: "",
                        unit = med?.unit ?: "",
                        instructions = med?.instructions,
                        scheduledDate = dateIso,
                        scheduledTime = log.scheduledTime,
                        hour = hour,
                        minute = minute,
                        status = status,
                        isRecurring = false,
                        reminderId = null,
                        overrideId = null,
                        originalTime = null,
                        doseLogId = log.id
                    )
                )
            }

        return result.sortedWith(
            compareBy<ScheduledDoseItem> { it.hour }
                .thenBy { it.minute }
                .thenBy { it.medicationName.lowercase(Locale.getDefault()) }
        )
    }

    fun resolveForMonth(
        yearMonth: YearMonth,
        medicationsWithReminders: List<MedicationWithReminders>,
        doseLogs: List<MedicationDoseLog>,
        overrides: List<MedicationScheduleOverride>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Map<LocalDate, List<ScheduledDoseItem>> {
        val logsByDate = doseLogs.groupBy { it.scheduledDate }
        val overridesByDate = overrides.groupBy { it.originalDate }
        val result = mutableMapOf<LocalDate, List<ScheduledDoseItem>>()

        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            val dateIso = date.toString()
            result[date] = resolveForDate(
                date = date,
                medicationsWithReminders = medicationsWithReminders,
                doseLogs = logsByDate[dateIso].orEmpty(),
                overrides = overridesByDate[dateIso].orEmpty(),
                today = today,
                zoneId = zoneId
            )
        }
        return result
    }
}
