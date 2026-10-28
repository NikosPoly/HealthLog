package com.example.healthlog.ui.components

import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// The rules of the entry dialog (MeasurementEntryState), step by step as a user would act.
class MeasurementEntryStateTest {

    private val height = MeasurementEntryState.initial(MeasurementType.HEIGHT)
    private val weight = MeasurementEntryState.initial(MeasurementType.WEIGHT)
    private val bloodPressure = MeasurementEntryState.initial(MeasurementType.BLOOD_PRESSURE)

    // ---- when the dialog opens ----

    @Test
    fun newDialog_showsNoErrors_andCannotBeSaved() {
        assertNull(height.visibleError(0))
        assertFalse(height.isValid)
        assertNull(height.toMeasurement())
    }

    // ---- errors shown only after leaving the field ----

    @Test
    fun tooLowValue_isHiddenWhileTyping() {
        val state = height.focusField(0).withText(0, "18")
        assertNull(state.visibleError(0))
        assertFalse(state.isValid)
    }

    @Test
    fun tooLowValue_isShownAfterLeavingTheField() {
        val state = height.focusField(0).withText(0, "18").leaveFocusedField()
        assertEquals(ValidationError.OutOfRange(30.0, 250.0), state.visibleError(0))
    }

    @Test
    fun emptyField_isShownAsRequiredAfterLeaving() {
        val state = height.focusField(0).leaveFocusedField()
        assertEquals(ValidationError.Required, state.visibleError(0))
    }

    @Test
    fun incompleteDecimal_isShownOnlyAfterLeaving() {
        val typing = weight.focusField(0).withText(0, "72,")
        assertNull(typing.visibleError(0))
        assertEquals(ValidationError.IncompleteDecimal, typing.leaveFocusedField().visibleError(0))
    }

    // ---- errors shown immediately ----

    @Test
    fun tooHighValue_isShownImmediately() {
        val state = height.focusField(0).withText(0, "3000")
        assertEquals(ValidationError.OutOfRange(30.0, 250.0), state.visibleError(0))
    }

    @Test
    fun diastolicNotBelowSystolic_isShownImmediately() {
        val state = bloodPressure
            .focusField(0).withText(0, "120")
            .focusField(1).withText(1, "130")
        assertEquals(ValidationError.DiastolicNotBelowSystolic, state.visibleError(1))
    }

    // ---- focus ----

    @Test
    fun movingToAnotherField_leavesThePreviousOne() {
        val state = bloodPressure.focusField(0).focusField(1)
        assertEquals(ValidationError.Required, state.visibleError(0))
        assertNull(state.visibleError(1))
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun focusingTheSameFieldAgain_doesNotLeaveIt() {
        // As after a rotation: the field is remembered, and tapping back into it continues.
        val state = height.focusField(0).withText(0, "18").focusField(0)
        assertNull(state.visibleError(0))
        assertEquals(0, state.focusedIndex)
    }

    @Test
    fun leavingWithoutAFocusedField_changesNothing() {
        assertEquals(height, height.leaveFocusedField())
    }

    // ---- separator ----

    @Test
    fun separatorChange_convertsTheTypedValue() {
        val english = weight.withText(0, "82.56")
        assertEquals("82,56", english.withSeparator(',').texts[0])
        assertEquals("82.56", english.withSeparator(',').withSeparator('.').texts[0])
    }

    @Test
    fun greekDecimal_isValid() {
        val state = weight.withText(0, "72,5")
        assertTrue(state.isValid)
        assertNull(state.visibleError(0))
    }

    // ---- saving ----

    @Test
    fun validWeight_isSavedWithDotDecimal() {
        val measurement = weight.withText(0, "72,5").toMeasurement()
        assertEquals(MeasurementType.WEIGHT, measurement?.type)
        assertEquals(72.5, measurement!!.value, 0.0)
    }

    @Test
    fun bloodPressure_isSavedInThreeColumns() {
        val measurement = bloodPressure
            .withText(0, "120").withText(1, "80").withText(2, "72")
            .toMeasurement()!!
        assertEquals(120.0, measurement.value, 0.0)
        assertEquals(80.0, measurement.secondaryValue!!, 0.0)
        assertEquals(72.0, measurement.tertiaryValue!!, 0.0)
    }

    @Test
    fun invalidState_cannotBeSaved() {
        assertNull(bloodPressure.withText(0, "80").withText(1, "120").withText(2, "72").toMeasurement())
    }
}
