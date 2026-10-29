package com.example.healthlog.ui.history

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Uses the history screen's layout with sample data: tabs, swiping, empty pages and deleting.
// Runs on an emulator (it uninstalls the app afterwards, like every androidTest).
@RunWith(AndroidJUnit4::class)
class HistoryContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // Sample data: one blood pressure and one pulse. Whole numbers, so the texts are the
    // same in Greek and English.
    private val bloodPressure = Measurement(
        id = 1,
        type = MeasurementType.BLOOD_PRESSURE,
        value = 120.0,
        secondaryValue = 80.0,
        tertiaryValue = 72.0
    )
    private val pulse = Measurement(id = 2, type = MeasurementType.PULSE, value = 70.0)

    private val sampleData = mapOf(
        MeasurementType.BLOOD_PRESSURE to listOf(bloodPressure),
        MeasurementType.PULSE to listOf(pulse)
    )

    // What the screen reported.
    private var selectedType = MeasurementType.BLOOD_PRESSURE
    private var requestedDelete: Measurement? = null

    private fun open(startType: MeasurementType) {
        selectedType = startType
        composeRule.setContent {
            var shownType by remember { mutableStateOf(startType) }
            HealthLogTheme {
                HistoryContent(
                    measurementsByType = sampleData,
                    selectedType = shownType,
                    measurementToDelete = null,
                    snackbarHostState = remember { SnackbarHostState() },
                    onBack = {},
                    onSelectType = {
                        shownType = it
                        selectedType = it
                    },
                    onRequestDelete = { requestedDelete = it },
                    onConfirmDelete = {},
                    onCancelDelete = {}
                )
            }
        }
    }

    // A tab, found by its type name. Tabs are "selectable"; the card titles with the
    // same name are not.
    private fun tab(type: MeasurementType) =
        composeRule.onNode(hasText(context.getString(type.label)) and isSelectable())

    private val bloodPressureText = "120/80 mmHg · 72 bpm"
    private val pulseText = "70 bpm"

    @Test
    fun opensOnTheGivenType_andShowsOnlyItsCards() {
        open(MeasurementType.BLOOD_PRESSURE)

        tab(MeasurementType.BLOOD_PRESSURE).assertIsSelected()
        composeRule.onNodeWithText(bloodPressureText).assertExists()
        composeRule.onNodeWithText(pulseText).assertDoesNotExist()
    }

    @Test
    fun tappingATab_showsThatTypesCards_andReportsIt() {
        open(MeasurementType.BLOOD_PRESSURE)

        tab(MeasurementType.PULSE).performClick()
        composeRule.waitForIdle()

        tab(MeasurementType.PULSE).assertIsSelected()
        composeRule.onNodeWithText(pulseText).assertExists()
        assertEquals(MeasurementType.PULSE, selectedType)
    }

    @Test
    fun swipingLeft_showsTheNextType() {
        open(MeasurementType.BLOOD_PRESSURE)

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        tab(MeasurementType.PULSE).assertIsSelected()
        assertEquals(MeasurementType.PULSE, selectedType)
    }

    @Test
    fun emptyType_showsTheEmptyText() {
        open(MeasurementType.HEIGHT)

        tab(MeasurementType.HEIGHT).assertIsSelected()
        composeRule.onNodeWithText(context.getString(R.string.history_empty)).assertExists()
    }

    @Test
    fun deleteOnACard_reportsThatMeasurement() {
        open(MeasurementType.BLOOD_PRESSURE)

        composeRule.onNodeWithText(context.getString(R.string.action_delete)).performClick()

        assertEquals(bloodPressure, requestedDelete)
    }
}
