package com.example.healthlog.ui.details

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.BmiEvaluation
import com.example.healthlog.guidelines.GuidelineEvaluation
import com.example.healthlog.guidelines.PulseZone
import com.example.healthlog.model.BodyPosition
import com.example.healthlog.model.MealContext
import com.example.healthlog.model.MeasurementArm
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeasurementDetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun displaysBloodPressure_withConditionsAndValues() {
        val measurement = Measurement(
            id = 10,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 135.0,
            secondaryValue = 85.0,
            tertiaryValue = 75.0,
            hasArrhythmia = false,
            arm = MeasurementArm.RIGHT.name,
            bodyPosition = BodyPosition.STANDING.name
        )

        composeRule.setContent {
            HealthLogTheme {
                MeasurementDetailsContent(
                    measurement = measurement,
                    onBack = {},
                    onDelete = {}
                )
            }
        }

        // Values and type
        composeRule.onNodeWithText(context.getString(R.string.type_blood_pressure)).assertExists()
        composeRule.onNodeWithText("135/85 mmHg · 75 bpm").assertExists()

        // Clinical conditions
        composeRule.onNodeWithText(context.getString(R.string.arm_right)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.position_standing)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.section_arrhythmia) + ": " + context.getString(R.string.arrhythmia_no)).assertExists()
    }

    @Test
    fun displaysBloodSugar_withMealContext() {
        val measurement = Measurement(
            id = 20,
            type = MeasurementType.BLOOD_SUGAR,
            value = 110.0,
            mealContext = MealContext.BEFORE_MEAL.name
        )

        composeRule.setContent {
            HealthLogTheme {
                MeasurementDetailsContent(
                    measurement = measurement,
                    onBack = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.type_blood_glucose)).assertExists()
        composeRule.onNodeWithText("110 mg/dL").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.meal_before)).assertExists()
    }

    @Test
    fun displaysNotFound_whenMeasurementIsNull() {
        composeRule.setContent {
            HealthLogTheme {
                MeasurementDetailsContent(
                    measurement = null,
                    onBack = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.details_not_found)).assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.action_delete)).assertDoesNotExist()
    }

    @Test
    fun backButton_callsOnBack() {
        var backCalled = false

        composeRule.setContent {
            HealthLogTheme {
                MeasurementDetailsContent(
                    measurement = Measurement(id = 1, type = MeasurementType.WEIGHT, value = 70.0),
                    onBack = { backCalled = true },
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.action_back)).performClick()
        assertTrue(backCalled)
    }

    @Test
    fun deleteButton_callsOnDelete() {
        var deleteCalled = false

        composeRule.setContent {
            HealthLogTheme {
                MeasurementDetailsContent(
                    measurement = Measurement(id = 1, type = MeasurementType.WEIGHT, value = 70.0),
                    onBack = {},
                    onDelete = { deleteCalled = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.action_delete)).performClick()
        assertTrue(deleteCalled)
    }

    @Test
    fun displaysStageBadgeAndReferenceCard_forBloodPressure() {
        val measurement = Measurement(
            id = 10,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 145.0,
            secondaryValue = 92.0,
            tertiaryValue = 74.0
        )
        val evaluation = GuidelineEvaluation.BloodPressure(
            stage = BloodPressureStage.GRADE_1_HYPERTENSION,
            systolic = 145.0,
            diastolic = 92.0,
            pulseEvaluation = GuidelineEvaluation.Pulse(
                zone = PulseZone.NORMAL,
                rate = 74.0
            )
        )

        composeRule.setContent {
            HealthLogTheme {
                MeasurementDetailsContent(
                    measurement = measurement,
                    guidelineEvaluation = evaluation,
                    onBack = {},
                    onDelete = {}
                )
            }
        }

        // Stage badge chip and reference scale active label
        composeRule.onAllNodesWithText(context.getString(R.string.bp_stage_grade_1)).assertCountEquals(2)
        composeRule.onNodeWithText(
            context.getString(R.string.type_pulse) + ": " + context.getString(R.string.pulse_zone_normal)
        ).assertExists()

        // Reference card source badge
        composeRule.onNodeWithText(context.getString(R.string.guidelines_source_ish)).assertExists()
        // Active criteria
        composeRule.onNodeWithText(context.getString(R.string.bp_criteria_grade_1)).assertExists()
    }

    @Test
    fun displaysStageBadgeAndReferenceCard_forBloodGlucose() {
        val measurement = Measurement(
            id = 20,
            type = MeasurementType.BLOOD_SUGAR,
            value = 110.0,
            mealContext = MealContext.BEFORE_MEAL.name
        )
        val evaluation = GuidelineEvaluation.BloodGlucose(
            category = BloodGlucoseCategory.PREDIABETES,
            value = 110.0,
            mealContext = MealContext.BEFORE_MEAL
        )

        composeRule.setContent {
            HealthLogTheme {
                MeasurementDetailsContent(
                    measurement = measurement,
                    guidelineEvaluation = evaluation,
                    onBack = {},
                    onDelete = {}
                )
            }
        }

        // Stage badge chip and reference scale active label
        composeRule.onAllNodesWithText(context.getString(R.string.glucose_category_prediabetes)).assertCountEquals(2)
        composeRule.onNodeWithText(context.getString(R.string.guidelines_source_who_idf)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.glucose_criteria_fasting_prediabetes)).assertExists()
    }

    @Test
    fun displaysBmiAndTargetWeightRange_forWeight() {
        val measurement = Measurement(
            id = 30,
            type = MeasurementType.WEIGHT,
            value = 86.5
        )
        val bmiEval = BmiEvaluation(
            bmi = 27.2,
            newBmi = 26.6,
            category = BmiCategory.OVERWEIGHT,
            heightCm = 178.0,
            weightKg = 86.5,
            healthyWeightMinKg = 58.6,
            healthyWeightMaxKg = 78.9,
            weightDeltaKg = 7.6
        )
        val evaluation = GuidelineEvaluation.Weight(
            weightKg = 86.5,
            bmiEvaluation = bmiEval
        )

        composeRule.setContent {
            HealthLogTheme {
                MeasurementDetailsContent(
                    measurement = measurement,
                    guidelineEvaluation = evaluation,
                    onBack = {},
                    onDelete = {}
                )
            }
        }

        val separator = context.getString(R.string.decimal_separator).first()
        val formattedBmi = "27.2".replace('.', separator)

        // Stage chip for overweight
        composeRule.onNodeWithText(
            context.getString(R.string.bmi_category_overweight) + " · " +
                context.getString(R.string.bmi_value_label, formattedBmi)
        ).assertExists()

        // Reference card source badge
        composeRule.onNodeWithText(context.getString(R.string.guidelines_source_who)).assertExists()

        // Asian population footnote
        composeRule.onNodeWithText(context.getString(R.string.bmi_asian_note)).assertExists()
    }
}
