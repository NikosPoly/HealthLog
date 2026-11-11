package com.example.healthlog.ui.home.newentry

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementField
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Uses the real entry dialog as a person would: types into fields, taps, and checks what is
// shown. The texts come from strings.xml, so the test works in the emulator's language.
// Runs on an emulator (it uninstalls the app afterwards, like every androidTest).
@RunWith(AndroidJUnit4::class)
class NewEntryDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // The measurement the dialog delivered on Save, if any.
    private var saved: Measurement? = null

    // ---- helpers ----

    private fun open(type: MeasurementType) {
        composeRule.setContent {
            HealthLogTheme {
                NewEntryDialog(type = type, onDismiss = {}, onSave = { saved = it })
            }
        }
    }

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    // A field, found by its label, e.g. "Height (cm)".
    private fun field(field: MeasurementField) =
        composeRule.onNodeWithText(text(R.string.field_label_with_unit, text(field.label), field.unit))

    private fun title(type: MeasurementType) = composeRule.onNodeWithText(text(type.label))

    private fun saveButton() = composeRule.onNodeWithText(text(R.string.action_save))

    private fun outOfRange(min: Int, max: Int) =
        text(R.string.error_value_out_of_range, min.toString(), max.toString())

    private fun hasErrorMessage(message: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.Error, message)

    private val height = MeasurementType.HEIGHT
    private val heightField = height.fields[0]

    // ---- tests ----

    @Test
    fun tooHighValue_showsErrorImmediately_andSaveIsDisabled() {
        open(height)
        field(heightField).performTextInput("3000")

        composeRule.onNodeWithText(outOfRange(30, 250), substring = true).assertExists()
        saveButton().assertIsNotEnabled()
    }

    @Test
    fun tooLowValue_showsErrorAfterTappingTheTitle() {
        open(height)
        field(heightField).performTextInput("18")
        composeRule.onNodeWithText(outOfRange(30, 250), substring = true).assertDoesNotExist()

        title(height).performClick()

        composeRule.onNodeWithText(outOfRange(30, 250), substring = true).assertExists()
    }

    @Test
    fun tooLowValue_showsErrorAfterTappingTheDisabledSave() {
        open(height)
        field(heightField).performTextInput("18")

        saveButton().performClick()

        composeRule.onNodeWithText(outOfRange(30, 250), substring = true).assertExists()
        assertEquals(null, saved)
    }

    @Test
    fun letters_areNotAccepted() {
        open(height)
        field(heightField).performTextInput("abc")

        field(heightField).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
        )
        saveButton().assertIsNotEnabled()
    }

    @Test
    fun validValue_enablesSave_andDeliversTheMeasurement() {
        open(height)
        field(heightField).performTextInput("185")

        saveButton().assertIsEnabled()
        saveButton().performClick()

        assertEquals(MeasurementType.HEIGHT, saved?.type)
        assertEquals(185.0, saved!!.value, 0.0)
    }

    @Test
    fun rotation_keepsTheValue_andShowsNoError() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            HealthLogTheme {
                NewEntryDialog(type = height, onDismiss = {}, onSave = { saved = it })
            }
        }
        field(heightField).performTextInput("18")

        // Simulates the activity being recreated, as on rotation.
        restorationTester.emulateSavedInstanceStateRestore()

        field(heightField).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("18"))
        )
        composeRule.onNodeWithText(outOfRange(30, 250), substring = true).assertDoesNotExist()
    }

    @Test
    fun diastolicNotBelowSystolic_showsErrorImmediately() {
        val bloodPressure = MeasurementType.BLOOD_PRESSURE
        open(bloodPressure)
        field(bloodPressure.fields[0]).performTextInput("120")
        field(bloodPressure.fields[1]).performTextInput("130")

        composeRule.onNodeWithText(
            text(R.string.error_diastolic_not_below_systolic), substring = true
        ).assertExists()
        saveButton().assertIsNotEnabled()
    }

    @Test
    fun bloodPressure_enablesSaveOnlyWhenAllOptionsSelected() {
        val bp = MeasurementType.BLOOD_PRESSURE
        open(bp)
        field(bp.fields[0]).performTextInput("120")
        field(bp.fields[1]).performTextInput("80")
        field(bp.fields[2]).performTextInput("72")

        // Numbers are valid, but options are not selected yet -> Save disabled
        saveButton().assertIsNotEnabled()

        // Select Left arm (found by icon's contentDescription)
        composeRule.onNodeWithContentDescription(text(R.string.arm_left)).performScrollTo().performClick()
        saveButton().assertIsNotEnabled()

        // Select Sitting
        composeRule.onNodeWithContentDescription(text(R.string.position_sitting)).performScrollTo().performClick()
        saveButton().assertIsNotEnabled()

        // Select No arrhythmia
        composeRule.onNodeWithContentDescription(text(R.string.arrhythmia_no)).performScrollTo().performClick()

        // Now all required fields and options are selected -> Save enabled
        saveButton().assertIsEnabled()
        saveButton().performClick()

        assertEquals(MeasurementType.BLOOD_PRESSURE, saved?.type)
        assertEquals("LEFT", saved?.arm)
        assertEquals("SITTING", saved?.bodyPosition)
        assertEquals(false, saved?.hasArrhythmia)
    }

    @Test
    fun customDateTime_checkboxTogglesDateTimeButtons() {
        open(height)

        val checkboxText = text(R.string.entry_use_current_datetime)

        // Checkbox is checked by default
        composeRule.onNodeWithText(checkboxText).assertExists()

        // Uncheck to show date/time picker button
        composeRule.onNodeWithText(checkboxText).performScrollTo().performClick()

        // Checkbox is still present and clickable
        composeRule.onNodeWithText(checkboxText).assertExists()
    }

    @Test
    fun tooLowValue_showsErrorAfterTappingOptionIcon() {
        val pulse = MeasurementType.PULSE
        open(pulse)
        field(pulse.fields[0]).performTextInput("8")
        composeRule.onNodeWithText(outOfRange(30, 300), substring = true).assertDoesNotExist()

        // Tap the arrhythmia "No" option icon
        composeRule.onNodeWithContentDescription(text(R.string.arrhythmia_no)).performScrollTo().performClick()

        // Error should now appear because tapping the option leaves the focused field
        composeRule.onNodeWithText(outOfRange(30, 300), substring = true).assertExists()
    }

    @Test
    fun tooLowValue_showsErrorAfterTappingAutoDateCheckbox() {
        open(height)
        field(heightField).performTextInput("18")
        composeRule.onNodeWithText(outOfRange(30, 250), substring = true).assertDoesNotExist()

        // Tap the auto date checkbox
        composeRule.onNodeWithText(text(R.string.entry_use_current_datetime)).performScrollTo().performClick()

        // Error should now appear because tapping the checkbox leaves the focused field
        composeRule.onNodeWithText(outOfRange(30, 250), substring = true).assertExists()
    }

    @Test
    fun optionErrors_andFieldErrors_shownWhenTappingDisabledSave() {
        val bp = MeasurementType.BLOOD_PRESSURE
        open(bp)

        // Initially no option error or required field error is displayed
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(0)
        composeRule.onAllNodes(androidx.compose.ui.test.hasText(text(R.string.error_required))).assertCountEquals(0)

        // Tap disabled Save
        saveButton().performClick()

        // All 3 required option groups (Arm, Position, Arrhythmia) show errors
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(3)

        // All 3 numeric fields show Required error message
        composeRule.onAllNodes(androidx.compose.ui.test.hasText(text(R.string.error_required))).assertCountEquals(3)

        // Selecting Arm clears its error (2 remain)
        composeRule.onNodeWithContentDescription(text(R.string.arm_left)).performScrollTo().performClick()
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(2)
    }

    @Test
    fun optionErrors_shownWhenAllNumbersEnteredAndDonePressed() {
        val pulse = MeasurementType.PULSE
        open(pulse)
        field(pulse.fields[0]).performTextInput("75")

        // While editing, no option error yet
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(0)

        // Press Done on the keyboard
        field(pulse.fields[0]).performImeAction()

        // Arrhythmia group now shows error because all numbers are valid
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(1)

        // Selecting arrhythmia clears the error
        composeRule.onNodeWithContentDescription(text(R.string.arrhythmia_no)).performScrollTo().performClick()
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(0)
    }

    @Test
    fun optionErrors_remainStickyWhenNumbersBecomeInvalid() {
        val bp = MeasurementType.BLOOD_PRESSURE
        open(bp)

        // Enter valid numbers in all 3 fields
        field(bp.fields[0]).performTextInput("120")
        field(bp.fields[1]).performTextInput("80")
        field(bp.fields[2]).performTextInput("72")

        // Done on last field leaves it and reveals option errors
        field(bp.fields[2]).performImeAction()
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(3)

        // Select Left arm -> Arm error clears, 2 remain
        composeRule.onNodeWithContentDescription(text(R.string.arm_left)).performScrollTo().performClick()
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(2)

        // User goes back to systolic field and replaces with invalid "8"
        field(bp.fields[0]).performScrollTo().performTextClearance()
        field(bp.fields[0]).performTextInput("8")

        // Position and Arrhythmia must remain in error state
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(2)

        // Tap outside (e.g. Save button)
        saveButton().performClick()
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(2)

        // Selecting Sitting position clears Position error (1 remains: Arrhythmia)
        composeRule.onNodeWithContentDescription(text(R.string.position_sitting)).performScrollTo().performClick()
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(1)

        // Selecting No arrhythmia clears all option errors
        composeRule.onNodeWithContentDescription(text(R.string.arrhythmia_no)).performScrollTo().performClick()
        composeRule.onAllNodes(hasErrorMessage("Selection required")).assertCountEquals(0)
    }
}
