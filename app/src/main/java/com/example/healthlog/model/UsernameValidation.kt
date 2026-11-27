package com.example.healthlog.model

sealed interface UsernameValidationError {
    data object MustStartWithLetter : UsernameValidationError
    data object TooShort : UsernameValidationError
    data object TooLong : UsernameValidationError
    data object InvalidCharacters : UsernameValidationError
}

object UsernameValidationRules {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 30

    // Must start with an English or Greek letter; remaining characters may be letters, digits, underscores, or hyphens.
    private val STARTS_WITH_LETTER_REGEX = Regex("^[a-zA-Z\\u0370-\\u03FF\\u1F00-\\u1FFF]")
    private val ALLOWED_CHARS_REGEX = Regex("^[a-zA-Z0-9_\\-\\u0370-\\u03FF\\u1F00-\\u1FFF]+$")

    /**
     * Validates [input] format for a personal username / display name.
     * Since the field is optional in the personal profile, a blank string returns null (valid / unset).
     * Non-blank strings are validated against standard naming rules:
     * 1. Must start with a letter (Latin or Greek).
     * 2. Length must be between [MIN_LENGTH] and [MAX_LENGTH] characters.
     * 3. Allowed characters: letters, digits, underscores (_), and hyphens (-).
     */
    fun validate(input: String?): UsernameValidationError? {
        val trimmed = input?.trim() ?: return null
        if (trimmed.isEmpty()) return null

        if (!STARTS_WITH_LETTER_REGEX.containsMatchIn(trimmed)) {
            return UsernameValidationError.MustStartWithLetter
        }

        if (trimmed.length < MIN_LENGTH) {
            return UsernameValidationError.TooShort
        }

        if (trimmed.length > MAX_LENGTH) {
            return UsernameValidationError.TooLong
        }

        if (!ALLOWED_CHARS_REGEX.matches(trimmed)) {
            return UsernameValidationError.InvalidCharacters
        }

        return null
    }
}
