package com.example.healthlog.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.healthlog.R

enum class SymptomSeverity(
    val level: Int,
    @StringRes val labelRes: Int,
    val lightColor: Color,
    val darkColor: Color
) {
    VERY_MILD(
        level = 1,
        labelRes = R.string.severity_very_mild,
        lightColor = Color(0xFF64B5F6), // Soft Sky Blue
        darkColor = Color(0xFF90CAF9)
    ),
    MILD(
        level = 2,
        labelRes = R.string.severity_mild,
        lightColor = Color(0xFF4CAF50), // Green
        darkColor = Color(0xFF81C784)
    ),
    MODERATE(
        level = 3,
        labelRes = R.string.severity_moderate,
        lightColor = Color(0xFFFFB300), // Amber
        darkColor = Color(0xFFFFD54F)
    ),
    SEVERE(
        level = 4,
        labelRes = R.string.severity_severe,
        lightColor = Color(0xFFFF7043), // Deep Orange
        darkColor = Color(0xFFFF8A65)
    ),
    VERY_SEVERE(
        level = 5,
        labelRes = R.string.severity_very_severe,
        lightColor = Color(0xFFE53935), // Red
        darkColor = Color(0xFFEF5350)
    );

    companion object {
        fun fromLevel(level: Int): SymptomSeverity {
            return entries.firstOrNull { it.level == level } ?: MODERATE
        }
    }
}
