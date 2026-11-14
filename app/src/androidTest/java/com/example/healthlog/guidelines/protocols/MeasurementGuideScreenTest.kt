package com.example.healthlog.guidelines.protocols

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented UI tests for the dedicated MeasurementGuideScreen.
@RunWith(AndroidJUnit4::class)
class MeasurementGuideScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    @Test
    fun bloodPressureGuide_displaysProtocolDetails() {
        var backClicked = false
        composeRule.setContent {
            HealthLogTheme {
                MeasurementGuideScreen(
                    type = MeasurementType.BLOOD_PRESSURE,
                    showTabs = false,
                    onBack = { backClicked = true },
                )
            }
        }

        // Title
        composeRule.onNodeWithText(text(MeasurementType.BLOOD_PRESSURE.label)).assertExists()

        // Clinical Reference Authority
        composeRule.onNodeWithText(text(R.string.protocol_bp_reference)).assertExists()

        // Step titles
        composeRule.onNodeWithText(text(R.string.protocol_bp_step1_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_bp_step2_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_bp_step3_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_bp_step4_title)).assertExists()

        // Back button navigation
        composeRule.onNodeWithContentDescription(text(R.string.action_back))
            .assertExists()
            .performClick()

        assertTrue(backClicked)
    }

    @Test
    fun bloodGlucoseGuide_displaysProtocolDetails() {
        composeRule.setContent {
            HealthLogTheme {
                MeasurementGuideScreen(
                    type = MeasurementType.BLOOD_SUGAR,
                    showTabs = false,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText(text(MeasurementType.BLOOD_SUGAR.label)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_glucose_reference)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_glucose_step1_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_glucose_step2_title)).assertExists()
    }

    @Test
    fun pulseGuide_displaysProtocolDetails() {
        composeRule.setContent {
            HealthLogTheme {
                MeasurementGuideScreen(
                    type = MeasurementType.PULSE,
                    showTabs = false,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText(text(MeasurementType.PULSE.label)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_pulse_reference)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_pulse_step1_title)).assertExists()
    }

    @Test
    fun weightGuide_displaysProtocolDetails() {
        composeRule.setContent {
            HealthLogTheme {
                MeasurementGuideScreen(
                    type = MeasurementType.WEIGHT,
                    showTabs = false,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText(text(MeasurementType.WEIGHT.label)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_weight_reference)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_weight_step1_title)).assertExists()
    }

    @Test
    fun heightGuide_displaysProtocolDetails() {
        composeRule.setContent {
            HealthLogTheme {
                MeasurementGuideScreen(
                    type = MeasurementType.HEIGHT,
                    showTabs = false,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText(text(MeasurementType.HEIGHT.label)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_height_reference)).assertExists()
        composeRule.onNodeWithText(text(R.string.protocol_height_step1_title)).assertExists()
    }

    @Test
    fun guideScreenWithTabs_allowsSwitchingMetrics() {
        composeRule.setContent {
            HealthLogTheme {
                MeasurementGuideScreen(
                    type = MeasurementType.BLOOD_PRESSURE,
                    showTabs = true,
                    onBack = {},
                )
            }
        }

        // Initially Blood Pressure is displayed
        composeRule.onNodeWithText(text(R.string.protocol_bp_reference)).assertExists()

        // Click Glucose tab
        composeRule.onNodeWithText(text(MeasurementType.BLOOD_SUGAR.label)).performClick()

        // Now Glucose protocol is displayed
        composeRule.onNodeWithText(text(R.string.protocol_glucose_reference)).assertExists()
    }
}
