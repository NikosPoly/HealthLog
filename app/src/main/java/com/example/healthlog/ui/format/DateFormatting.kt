package com.example.healthlog.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.example.healthlog.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// The date formatter of the current language, e.g. "27/10/2026 18:41" in Greek
// and "Oct 27, 2026, 6:41 PM" in English. The pattern comes from strings.xml.
// DateTimeFormatter is immutable and thread-safe; it is only rebuilt when the language changes.
@Composable
fun rememberDateTimeFormatter(): DateTimeFormatter {
    val pattern = stringResource(R.string.date_time_pattern)
    val locale = formatLocale()
    return remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
}

// The date and time of a stored timestamp (milliseconds), shown in the phone's time zone.
fun formatTimestamp(timestamp: Long, formatter: DateTimeFormatter): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
