package com.example.healthlog.backup

import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Frequency intervals for automatic background backups.
 */
enum class BackupSchedule(val days: Long) {
    DAILY(1),
    THREE_DAYS(3),
    WEEKLY(7),
    TWO_WEEKS(14),
    MONTHLY(30),
    OFF(0);

    val isEnabled: Boolean
        get() = this != OFF

    val intervalMillis: Long
        get() = TimeUnit.DAYS.toMillis(days)

    val repeatIntervalDays: Long
        get() = days.coerceAtLeast(1)

    companion object {
        val DEFAULT = WEEKLY

        fun fromNameOrNull(name: String?): BackupSchedule? {
            if (name == null) return null
            return try {
                valueOf(name)
            } catch (e: Exception) {
                null
            }
        }
    }
}
