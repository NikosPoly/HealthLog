package com.example.healthlog.ui.format

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

// The date shown in the history (formatTimestamp) with the patterns of both languages.
// The patterns are repeated from strings.xml (date_time_pattern), because plain unit tests
// can't read resources: if a pattern changes there, change it here too.
class DateFormattingTest {

    private val greek = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("el"))
    private val english = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.forLanguageTag("en"))

    // A stored timestamp for the given date and time in the computer's time zone,
    // so the tests give the same result wherever they run.
    private fun timestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    @Test
    fun greekFormat_afternoon() {
        assertEquals("27/10/2026 18:41", formatTimestamp(timestamp(2026, 10, 27, 18, 41), greek))
    }

    @Test
    fun englishFormat_afternoon() {
        assertEquals("Oct 27, 2026, 6:41 PM", formatTimestamp(timestamp(2026, 10, 27, 18, 41), english))
    }

    @Test
    fun greekFormat_morning_hasLeadingZeros() {
        assertEquals("05/03/2026 09:05", formatTimestamp(timestamp(2026, 3, 5, 9, 5), greek))
    }

    @Test
    fun englishFormat_morning_hasNoLeadingZeros() {
        assertEquals("Mar 5, 2026, 9:05 AM", formatTimestamp(timestamp(2026, 3, 5, 9, 5), english))
    }
}
