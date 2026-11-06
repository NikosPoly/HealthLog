package com.example.healthlog.ui.theme

import androidx.annotation.DrawableRes
import com.example.healthlog.R
import com.example.healthlog.model.MeasurementType

// The icon of each type, drawn in the type's color (see TypeTheme for the colors).
// Shown on the type's home card and in its entry dialog.
// Multi-colored: show it with tint = Color.Unspecified, so the colors are kept.
@get:DrawableRes
val MeasurementType.icon: Int
    get() = when (this) {
        MeasurementType.BLOOD_PRESSURE -> R.drawable.ic_type_blood_pressure
        MeasurementType.PULSE -> R.drawable.ic_type_pulse
        MeasurementType.BLOOD_SUGAR -> R.drawable.ic_type_blood_sugar
        MeasurementType.WEIGHT -> R.drawable.ic_type_weight
        MeasurementType.HEIGHT -> R.drawable.ic_type_height
    }
