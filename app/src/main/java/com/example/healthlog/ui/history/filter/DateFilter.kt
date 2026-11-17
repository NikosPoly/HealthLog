package com.example.healthlog.ui.history.filter

import androidx.annotation.StringRes
import com.example.healthlog.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Represents date or time-period filtering criteria for the history list.
sealed interface DateFilter {
    @get:StringRes
    val labelRes: Int?

    // Formatted label for display in UI chips.
    fun displayLabel(locale: Locale): String

    // Returns true if the given measurement timestamp falls within this filter's time bounds.
    fun includes(timestampMillis: Long, now: ZonedDateTime = ZonedDateTime.now()): Boolean

    // Today (from local midnight 00:00:00 to now / end of day).
    data object Today : DateFilter {
        override val labelRes: Int = R.string.date_filter_today

        override fun displayLabel(locale: Locale): String = ""

        override fun includes(timestampMillis: Long, now: ZonedDateTime): Boolean {
            val startOfToday = now.toLocalDate().atStartOfDay(now.zone).toInstant().toEpochMilli()
            val endOfToday = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).toInstant().toEpochMilli() - 1
            return timestampMillis in (startOfToday..endOfToday)
        }
    }

    // Last 7 days rolling period.
    data object Last7Days : DateFilter {
        override val labelRes: Int = R.string.date_filter_7_days

        override fun displayLabel(locale: Locale): String = ""

        override fun includes(timestampMillis: Long, now: ZonedDateTime): Boolean {
            val start = now.minusDays(7).toInstant().toEpochMilli()
            val end = now.toInstant().toEpochMilli()
            return timestampMillis in (start..end)
        }
    }

    // Last 30 days rolling period.
    data object Last30Days : DateFilter {
        override val labelRes: Int = R.string.date_filter_30_days

        override fun displayLabel(locale: Locale): String = ""

        override fun includes(timestampMillis: Long, now: ZonedDateTime): Boolean {
            val start = now.minusDays(30).toInstant().toEpochMilli()
            val end = now.toInstant().toEpochMilli()
            return timestampMillis in (start..end)
        }
    }

    // Custom date or date span (e.g. 12 Nov – 28 Nov, or single day when startDate == endDate).
    data class CustomRange(
        val startDate: LocalDate,
        val endDate: LocalDate = startDate,
    ) : DateFilter {
        override val labelRes: Int? = null

        override fun displayLabel(locale: Locale): String {
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            return if (startDate == endDate) {
                startDate.format(formatter)
            } else {
                "${startDate.format(formatter)} – ${endDate.format(formatter)}"
            }
        }

        override fun includes(timestampMillis: Long, now: ZonedDateTime): Boolean {
            val zone = now.zone
            val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return timestampMillis in (startMillis..endMillis)
        }

        companion object {
            // Converts Material 3 DateRangePicker UTC epoch milliseconds to a local CustomRange.
            fun fromUtcMillis(startUtcMillis: Long, endUtcMillis: Long? = null): CustomRange {
                val utcZone = ZoneId.of("UTC")
                val start = Instant.ofEpochMilli(startUtcMillis).atZone(utcZone).toLocalDate()
                val end = if (endUtcMillis != null) {
                    Instant.ofEpochMilli(endUtcMillis).atZone(utcZone).toLocalDate()
                } else {
                    start
                }
                return CustomRange(startDate = start, endDate = end)
            }
        }
    }
}
