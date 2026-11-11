package com.example.healthlog.model

import androidx.annotation.StringRes
import com.example.healthlog.R

// Arm used when taking a blood pressure measurement.
enum class MeasurementArm(@param:StringRes val label: Int) {
    LEFT(R.string.arm_left),
    RIGHT(R.string.arm_right)
}

// Body posture during a blood pressure measurement.
enum class BodyPosition(@param:StringRes val label: Int) {
    SITTING(R.string.position_sitting),
    STANDING(R.string.position_standing),
    LYING_DOWN(R.string.position_lying_down)
}

// Meal context for a blood glucose measurement.
enum class MealContext(@param:StringRes val label: Int) {
    BEFORE_MEAL(R.string.meal_before),
    AFTER_MEAL_2H(R.string.meal_after_2h)
}
