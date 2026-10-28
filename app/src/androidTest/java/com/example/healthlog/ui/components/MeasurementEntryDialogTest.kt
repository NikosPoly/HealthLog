package com.example.healthlog.ui.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class MeasurementEntryDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // The measurement the dialog delivered on Save, if any.
    private var saved: Measurement? = null

    // ---- helpers ----

    private fun open(type: MeasurementType) {
        composeRule.setContent {
            HealthLogTheme {
                MeasurementEntryDialog(type = type, onDismiss = {}, onSave = { saved = it })
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
                MeasurementEntryDialog(type = height, onDismiss = {}, onSave = { saved = it })
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
}
