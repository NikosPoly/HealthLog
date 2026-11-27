package com.example.healthlog.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class UserGenderTest {

    @Test
    fun fromNameOrNull_resolvesMaleAndFemaleCaseInsensitive() {
        assertEquals(UserGender.MALE, UserGender.fromNameOrNull("MALE"))
        assertEquals(UserGender.MALE, UserGender.fromNameOrNull("male"))
        assertEquals(UserGender.MALE, UserGender.fromNameOrNull(" Male "))

        assertEquals(UserGender.FEMALE, UserGender.fromNameOrNull("FEMALE"))
        assertEquals(UserGender.FEMALE, UserGender.fromNameOrNull("female"))
        assertEquals(UserGender.FEMALE, UserGender.fromNameOrNull(" Female "))
    }

    @Test
    fun fromNameOrNull_returnsNullOnInvalidOrBlank() {
        assertNull(UserGender.fromNameOrNull(null))
        assertNull(UserGender.fromNameOrNull(""))
        assertNull(UserGender.fromNameOrNull("   "))
        assertNull(UserGender.fromNameOrNull("OTHER"))
        assertNull(UserGender.fromNameOrNull("UNKNOWN"))
    }

    @Test
    fun parseDateOfBirthOrNull_parsesValidIso() {
        assertEquals(LocalDate.of(1990, 5, 15), parseDateOfBirthOrNull("1990-05-15"))
        assertEquals(LocalDate.of(2000, 1, 1), parseDateOfBirthOrNull("  2000-01-01  "))
    }

    @Test
    fun parseDateOfBirthOrNull_returnsNullOnInvalidOrBlank() {
        assertNull(parseDateOfBirthOrNull(null))
        assertNull(parseDateOfBirthOrNull(""))
        assertNull(parseDateOfBirthOrNull("not-a-date"))
        assertNull(parseDateOfBirthOrNull("15/05/1990"))
    }

    @Test
    fun calculateAgeYears_calculatesCompletedYears() {
        val today = LocalDate.of(2026, 11, 27)

        // Birthday has occurred this year: exactly 36
        val dob1 = LocalDate.of(1990, 5, 15)
        assertEquals(36, calculateAgeYears(dob1, today))

        // Birthday has not occurred yet this year: 35
        val dob2 = LocalDate.of(1990, 12, 1)
        assertEquals(35, calculateAgeYears(dob2, today))

        // Born today: 0
        assertEquals(0, calculateAgeYears(today, today))

        // String overload
        assertEquals(36, calculateAgeYears("1990-05-15", today))
    }

    @Test
    fun calculateAgeYears_returnsNullForNullOrFutureDates() {
        val today = LocalDate.of(2026, 11, 27)
        assertNull(calculateAgeYears(null as LocalDate?, today))
        assertNull(calculateAgeYears(null as String?, today))
        assertNull(calculateAgeYears("invalid", today))

        val future = LocalDate.of(2027, 1, 1)
        assertNull(calculateAgeYears(future, today))
        assertNull(calculateAgeYears("2027-01-01", today))
    }
}
