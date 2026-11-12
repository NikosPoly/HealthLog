package com.example.healthlog.ui.details

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
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
}
