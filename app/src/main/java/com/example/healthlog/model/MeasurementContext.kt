package com.example.healthlog.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.healthlog.R

// Arm used when taking a blood pressure measurement.
enum class MeasurementArm(
    @param:StringRes val label: Int,
    @param:DrawableRes val iconRes: Int
) {
    LEFT(R.string.arm_left, R.drawable.ic_arm_left),
    RIGHT(R.string.arm_right, R.drawable.ic_arm_right);

    companion object {
        fun fromNameOrNull(name: String?): MeasurementArm? =
            entries.firstOrNull { it.name == name }
    }
}

// Body posture during a blood pressure measurement.
enum class BodyPosition(
    @param:StringRes val label: Int,
    @param:DrawableRes val iconRes: Int
) {
    SITTING(R.string.position_sitting, R.drawable.ic_position_sitting),
    STANDING(R.string.position_standing, R.drawable.ic_position_standing),
    LYING_DOWN(R.string.position_lying_down, R.drawable.ic_position_lying);

    companion object {
        fun fromNameOrNull(name: String?): BodyPosition? =
            entries.firstOrNull { it.name == name }
    }
}

// Meal context for a blood glucose measurement.
enum class MealContext(
    @param:StringRes val label: Int,
    @param:DrawableRes val iconRes: Int
) {
    BEFORE_MEAL(R.string.meal_before, R.drawable.ic_meal_before),
    AFTER_MEAL_2H(R.string.meal_after_2h, R.drawable.ic_meal_after);

    companion object {
        fun fromNameOrNull(name: String?): MealContext? =
            entries.firstOrNull { it.name == name }
    }
}

// Arrhythmia label and icon for whether an arrhythmia was detected.
@StringRes
fun arrhythmiaLabel(hasArrhythmia: Boolean): Int =
    if (hasArrhythmia) R.string.arrhythmia_yes else R.string.arrhythmia_no

@DrawableRes
fun arrhythmiaIcon(hasArrhythmia: Boolean): Int =
    if (hasArrhythmia) R.drawable.ic_arrhythmia_yes else R.drawable.ic_arrhythmia_no
