package com.example.healthlog.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsernameValidationTest {

    @Test
    fun nullOrBlank_isValid() {
        assertNull(UsernameValidationRules.validate(null))
        assertNull(UsernameValidationRules.validate(""))
        assertNull(UsernameValidationRules.validate("   "))
    }

    @Test
    fun validUsernames_englishAndGreek_returnNull() {
        assertNull(UsernameValidationRules.validate("nikos"))
        assertNull(UsernameValidationRules.validate("Nikos_99"))
        assertNull(UsernameValidationRules.validate("john-doe"))
        assertNull(UsernameValidationRules.validate("Μαρία"))
        assertNull(UsernameValidationRules.validate("Νίκος_123"))
        assertNull(UsernameValidationRules.validate("Alex-9"))
    }

    @Test
    fun mustStartWithLetter_failsOnNumberOrSpecialChar() {
        assertEquals(
            UsernameValidationError.MustStartWithLetter,
            UsernameValidationRules.validate("123nikos")
        )
        assertEquals(
            UsernameValidationError.MustStartWithLetter,
            UsernameValidationRules.validate("_nikos")
        )
        assertEquals(
            UsernameValidationError.MustStartWithLetter,
            UsernameValidationRules.validate("-maria")
        )
        assertEquals(
            UsernameValidationError.MustStartWithLetter,
            UsernameValidationRules.validate("@john")
        )
    }

    @Test
    fun tooShort_failsUnder3Chars() {
        assertEquals(
            UsernameValidationError.TooShort,
            UsernameValidationRules.validate("ab")
        )
        assertEquals(
            UsernameValidationError.TooShort,
            UsernameValidationRules.validate("Ν")
        )
    }

    @Test
    fun tooLong_failsOver30Chars() {
        val longName = "A".repeat(31)
        assertEquals(
            UsernameValidationError.TooLong,
            UsernameValidationRules.validate(longName)
        )
    }

    @Test
    fun invalidCharacters_failsOnSpacesOrSymbols() {
        assertEquals(
            UsernameValidationError.InvalidCharacters,
            UsernameValidationRules.validate("Nikos Doe")
        )
        assertEquals(
            UsernameValidationError.InvalidCharacters,
            UsernameValidationRules.validate("user@name")
        )
        assertEquals(
            UsernameValidationError.InvalidCharacters,
            UsernameValidationRules.validate("user.name")
        )
        assertEquals(
            UsernameValidationError.InvalidCharacters,
            UsernameValidationRules.validate("user!123")
        )
    }
}
