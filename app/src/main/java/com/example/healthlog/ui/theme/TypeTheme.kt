package com.example.healthlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.healthlog.model.MeasurementType
import com.materialkolor.PaletteStyle
import com.materialkolor.material3.rememberDynamicColorScheme

// Each type has its own color: red for blood pressure, green for weight, and so on.
// TypeTheme gives everything inside it a full color scheme built from that type's color,
// the way Android builds one from the wallpaper. So the cards, chips, buttons and the chart
// inside take the type's color by themselves, in light and in dark mode.
// Inside a TypeTheme, the type colors replace the wallpaper (dynamic) colors.
@Composable
fun TypeTheme(type: MeasurementType, content: @Composable () -> Unit) {
    val colorScheme = rememberDynamicColorScheme(
        seedColor = type.seedColor,
        isDark = isSystemInDarkTheme(),
        style = TYPE_COLOR_STYLE
    )

    // Only the colors change; the text styles and shapes stay those of HealthLogTheme.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}

// How the scheme is built from the type's color:
// TonalSpot = soft, like the wallpaper colors; Fidelity = vivid, close to the color itself.
// Still being tried out: switch here to compare.
private val TYPE_COLOR_STYLE = PaletteStyle.TonalSpot

// The color each type's scheme is built from.
private val MeasurementType.seedColor: Color
    get() = when (this) {
        MeasurementType.BLOOD_PRESSURE -> Color(0xFFD32F2F) // red
        MeasurementType.PULSE -> Color(0xFFD81B60)          // pink
        MeasurementType.BLOOD_SUGAR -> Color(0xFFEF6C00)    // orange
        MeasurementType.WEIGHT -> Color(0xFF2E7D32)         // green
        MeasurementType.HEIGHT -> Color(0xFF1565C0)         // blue
    }
