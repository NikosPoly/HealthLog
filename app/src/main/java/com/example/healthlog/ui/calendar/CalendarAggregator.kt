package com.example.healthlog.ui.calendar

import com.example.healthlog.database.Measurement
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.SymptomSeverity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object CalendarAggregator {

    // Converts epoch millisecond timestamp to local calendar date using the given timezone.
    fun timestampToLocalDate(
        timestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LocalDate {
        return Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
    }

    // Parses an ISO-8601 string "YYYY-MM-DD" safely into LocalDate, or null if invalid.
    fun parseIsoDateOrNull(dateString: String): LocalDate? {
        return try {
            LocalDate.parse(dateString)
        } catch (_: Exception) {
            null
        }
    }

    // Computes start (inclusive) and end (exclusive) epoch milliseconds for a full YearMonth.
    fun monthTimeRange(
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Pair<Long, Long> {
        val startInstant = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant()
        val nextMonth = yearMonth.plusMonths(1)
        val endInstant = nextMonth.atDay(1).atStartOfDay(zoneId).toInstant()
        return Pair(startInstant.toEpochMilli(), endInstant.toEpochMilli())
    }

    // Computes start and end ISO-8601 date strings ("YYYY-MM-DD") for a YearMonth.
    fun monthIsoDateRange(yearMonth: YearMonth): Pair<String, String> {
        val startDate = yearMonth.atDay(1).toString()
        val endDate = yearMonth.atEndOfMonth().toString()
        return Pair(startDate, endDate)
    }

    // Filters out independent Pulse entries that were automatically saved alongside a Blood Pressure
    // reading (same timestamp and pulse value), so the calendar page displays only the Blood Pressure
    // entry (which already includes the pulse) without duplicating it as a separate Pulse item.
    internal fun filterCompanionPulses(measurements: List<Measurement>): List<Measurement> {
        val bpPulseKeys = measurements
            .filter { it.type == MeasurementType.BLOOD_PRESSURE && it.tertiaryValue != null }
            .map { it.timestamp to it.tertiaryValue }
            .toSet()

        if (bpPulseKeys.isEmpty()) return measurements

        return measurements.filterNot { measurement ->
            measurement.type == MeasurementType.PULSE &&
                (measurement.timestamp to measurement.value) in bpPulseKeys
        }
    }

    // Aggregates raw lists of measurements, dose logs, and symptoms for a month into
    // a Map of LocalDate -> CalendarDaySummary.
    fun aggregateMonth(
        yearMonth: YearMonth,
        measurements: List<Measurement>,
        doseLogs: List<MedicationDoseLog>,
        symptoms: List<SymptomEntry>,
        latestHeightCm: Double? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Map<LocalDate, CalendarDaySummary> {
        // Group measurements by LocalDate
        val measurementsByDate = measurements.groupBy {
            timestampToLocalDate(it.timestamp, zoneId)
        }

        // Group dose logs by scheduledDate
        val doseLogsByDate = doseLogs.groupBy {
            parseIsoDateOrNull(it.scheduledDate)
        }.filterKeys { it != null }.mapKeys { it.key!! }

        // Group symptoms by LocalDate
        val symptomsByDate = symptoms.groupBy {
            timestampToLocalDate(it.timestamp, zoneId)
        }

        val summaries = mutableMapOf<LocalDate, CalendarDaySummary>()
        val daysInMonth = yearMonth.lengthOfMonth()

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dayMeasurements = filterCompanionPulses(measurementsByDate[date].orEmpty())
            val dayDoseLogs = doseLogsByDate[date].orEmpty()
            val daySymptoms = symptomsByDate[date].orEmpty()

            val highestRank = calculateHighestVitalRank(dayMeasurements, latestHeightCm)
            val maxSeverity = daySymptoms.map { SymptomSeverity.fromLevel(it.severity) }
                .maxByOrNull { it.level }

            summaries[date] = CalendarDaySummary(
                date = date,
                measurements = dayMeasurements,
                doseLogs = dayDoseLogs,
                symptoms = daySymptoms,
                highestVitalRank = highestRank,
                maxSymptomSeverity = maxSeverity
            )
        }

        return summaries
    }
}
