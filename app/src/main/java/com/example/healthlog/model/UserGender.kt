package com.example.healthlog.model

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

enum class UserGender {
    MALE,
    FEMALE;

    companion object {
        fun fromNameOrNull(name: String?): UserGender? {
            if (name.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
        }
    }
}

/**
 * Parses an ISO-8601 date string ("YYYY-MM-DD") into a [LocalDate], or returns null if blank/invalid.
 */
fun parseDateOfBirthOrNull(isoDate: String?): LocalDate? {
    val trimmed = isoDate?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return try {
        LocalDate.parse(trimmed)
    } catch (_: DateTimeParseException) {
        null
    }
}

/**
 * Calculates age in completed years from [dateOfBirth] as of [today].
 * Returns null if [dateOfBirth] is null or lies in the future relative to [today].
 */
fun calculateAgeYears(
    dateOfBirth: LocalDate?,
    today: LocalDate = LocalDate.now()
): Int? {
    if (dateOfBirth == null || dateOfBirth.isAfter(today)) return null
    return Period.between(dateOfBirth, today).years
}

/**
 * Convenience overload accepting an ISO-8601 date string ("YYYY-MM-DD").
 */
fun calculateAgeYears(
    isoDateOfBirth: String?,
    today: LocalDate = LocalDate.now()
): Int? = calculateAgeYears(parseDateOfBirthOrNull(isoDateOfBirth), today)
