package com.example.healthlog.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// What can be typed into a number field (isAllowedInput in NumberInput.kt).
class NumberInputTest {

    // ---- whole numbers (pulse, height, blood pressure, blood sugar) ----

    @Test
    fun emptyText_isAllowed() {
        assertTrue(isAllowedInput("", maxDecimals = 0, separator = '.'))
    }

    @Test
    fun wholeNumber_isAllowed() {
        assertTrue(isAllowedInput("185", maxDecimals = 0, separator = '.'))
    }

    @Test
    fun sixDigits_areAllowed_sevenAreNot() {
        assertTrue(isAllowedInput("123456", maxDecimals = 0, separator = '.'))
        assertFalse(isAllowedInput("1234567", maxDecimals = 0, separator = '.'))
    }

    @Test
    fun leadingZero_isNotAllowed() {
        assertFalse(isAllowedInput("0", maxDecimals = 0, separator = '.'))
        assertFalse(isAllowedInput("08", maxDecimals = 0, separator = '.'))
    }

    @Test
    fun separator_isNotAllowedInWholeNumbers() {
        assertFalse(isAllowedInput("72.5", maxDecimals = 0, separator = '.'))
        assertFalse(isAllowedInput("72,5", maxDecimals = 0, separator = ','))
    }

    @Test
    fun lettersAndSigns_areNotAllowed() {
        assertFalse(isAllowedInput("abc", maxDecimals = 0, separator = '.'))
        assertFalse(isAllowedInput("-5", maxDecimals = 0, separator = '.'))
        assertFalse(isAllowedInput("7 2", maxDecimals = 0, separator = '.'))
    }

    // ---- decimals in English (weight, separator ".") ----

    @Test
    fun englishWeight_upToTwoDecimals_isAllowed() {
        assertTrue(isAllowedInput("72.5", maxDecimals = 2, separator = '.'))
        assertTrue(isAllowedInput("72.55", maxDecimals = 2, separator = '.'))
    }

    @Test
    fun englishWeight_thirdDecimal_isNotAllowed() {
        assertFalse(isAllowedInput("72.555", maxDecimals = 2, separator = '.'))
    }

    @Test
    fun englishWeight_endingWithSeparator_isAllowedWhileTyping() {
        assertTrue(isAllowedInput("72.", maxDecimals = 2, separator = '.'))
    }

    @Test
    fun englishWeight_startingWithSeparator_isNotAllowed() {
        assertFalse(isAllowedInput(".5", maxDecimals = 2, separator = '.'))
    }

    @Test
    fun englishWeight_comma_isNotAllowed() {
        assertFalse(isAllowedInput("72,5", maxDecimals = 2, separator = '.'))
    }

    @Test
    fun englishWeight_onlyOneSeparator_isAllowed() {
        assertFalse(isAllowedInput("72.5.5", maxDecimals = 2, separator = '.'))
    }

    // ---- decimals in Greek (weight, separator ",") ----

    @Test
    fun greekWeight_commaWithTwoDecimals_isAllowed() {
        assertTrue(isAllowedInput("72,55", maxDecimals = 2, separator = ','))
    }

    @Test
    fun greekWeight_endingWithComma_isAllowedWhileTyping() {
        assertTrue(isAllowedInput("72,", maxDecimals = 2, separator = ','))
    }

    @Test
    fun greekWeight_dot_isNotAllowed() {
        assertFalse(isAllowedInput("72.55", maxDecimals = 2, separator = ','))
    }

    @Test
    fun greekWeight_startingWithComma_isNotAllowed() {
        assertFalse(isAllowedInput(",5", maxDecimals = 2, separator = ','))
    }
}
