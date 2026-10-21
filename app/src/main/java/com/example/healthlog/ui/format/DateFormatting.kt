package com.example.healthlog.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// DateTimeFormatter is immutable and thread-safe, so one shared instance is enough.
private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("el", "GR"))

// The date and time of a stored timestamp (milliseconds), e.g. "21/10/2026 16:39",
// shown in the phone's time zone.
fun formatTimestamp(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(dateTimeFormatter)
