package com.example.healthlog.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

// The range limits shown in "The value must be between … and …" (formatLimit).
class ValidationMessagesTest {

    @Test
    fun wholeLimit_hasNoDecimals() {
        assertEquals("30", formatLimit(30.0, '.'))
        assertEquals("300", formatLimit(300.0, '.'))
    }

    @Test
    fun wholeLimit_isTheSameInEveryLanguage() {
        assertEquals("30", formatLimit(30.0, ','))
    }

    @Test
    fun decimalLimit_usesTheLanguagesSeparator() {
        assertEquals("1.5", formatLimit(1.5, '.'))
        assertEquals("1,5", formatLimit(1.5, ','))
    }
}
