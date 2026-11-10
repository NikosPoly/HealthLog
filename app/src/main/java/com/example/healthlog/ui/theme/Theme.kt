package com.example.healthlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.materialkolor.PaletteStyle
import com.materialkolor.material3.rememberDynamicColorScheme


@Composable
fun HealthLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = rememberDynamicColorScheme(
        seedColor = BrandSeedColor,
        isDark = darkTheme,
        style = PaletteStyle.TonalSpot
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}