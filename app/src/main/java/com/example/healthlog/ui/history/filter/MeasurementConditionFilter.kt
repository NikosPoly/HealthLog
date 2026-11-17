package com.example.healthlog.ui.history.filter

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.healthlog.model.BodyPosition
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementArm
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.arrhythmiaIcon
import com.example.healthlog.model.arrhythmiaLabel

// Represents a physical measurement condition/metadata that a user can filter by in history.
sealed interface MeasurementConditionFilter {
    @get:StringRes
    val labelRes: Int

    @get:DrawableRes
    val iconRes: Int

    // Arm used for blood pressure measurement (Left or Right).
    data class Arm(val arm: MeasurementArm) : MeasurementConditionFilter {
        override val labelRes: Int = arm.label
        override val iconRes: Int = arm.iconRes
    }

    // Body posture during blood pressure measurement (Sitting, Standing, Lying down).
    data class Position(val position: BodyPosition) : MeasurementConditionFilter {
        override val labelRes: Int = position.label
        override val iconRes: Int = position.iconRes
    }

    // Meal timing context for blood glucose (Before meal, 2h After meal).
    data class Meal(val meal: MealContext) : MeasurementConditionFilter {
        override val labelRes: Int = meal.label
        override val iconRes: Int = meal.iconRes
    }

    // Arrhythmia / irregular heart rhythm detection (Yes or No).
    data class Arrhythmia(val detected: Boolean) : MeasurementConditionFilter {
        override val labelRes: Int = arrhythmiaLabel(detected)
        override val iconRes: Int = arrhythmiaIcon(detected)
    }
}

// Returns the list of standard measurement conditions available for filtering the given measurement type.
fun availableConditionsForType(type: MeasurementType): List<MeasurementConditionFilter> {
    return when (type) {
        MeasurementType.BLOOD_PRESSURE -> listOf(
            MeasurementConditionFilter.Arm(MeasurementArm.LEFT),
            MeasurementConditionFilter.Arm(MeasurementArm.RIGHT),
            MeasurementConditionFilter.Position(BodyPosition.SITTING),
            MeasurementConditionFilter.Position(BodyPosition.STANDING),
            MeasurementConditionFilter.Position(BodyPosition.LYING_DOWN),
            MeasurementConditionFilter.Arrhythmia(detected = true),
            MeasurementConditionFilter.Arrhythmia(detected = false),
        )
        MeasurementType.PULSE -> listOf(
            MeasurementConditionFilter.Arrhythmia(detected = true),
            MeasurementConditionFilter.Arrhythmia(detected = false),
        )
        MeasurementType.BLOOD_SUGAR -> listOf(
            MeasurementConditionFilter.Meal(MealContext.BEFORE_MEAL),
            MeasurementConditionFilter.Meal(MealContext.AFTER_MEAL_2H),
        )
        MeasurementType.WEIGHT,
        MeasurementType.HEIGHT,
        -> emptyList()
    }
}
