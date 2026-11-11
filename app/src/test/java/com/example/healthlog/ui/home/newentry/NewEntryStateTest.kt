package com.example.healthlog.ui.home.newentry

import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// The rules of the entry dialog (NewEntryState), step by step as a user would act.
class NewEntryStateTest {

    private val height = NewEntryState.initial(MeasurementType.HEIGHT)
    private val weight = NewEntryState.initial(MeasurementType.WEIGHT)
    private val bloodPressure = NewEntryState.initial(MeasurementType.BLOOD_PRESSURE)

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
    fun bloodPressure_requiresArmPositionAndArrhythmia() {
        val numbersOnly = bloodPressure
            .withText(0, "120").withText(1, "80").withText(2, "72")
        assertFalse(numbersOnly.isValid)
        assertNull(numbersOnly.toMeasurement())

        val withArmOnly = numbersOnly.withArm(com.example.healthlog.model.MeasurementArm.LEFT)
        assertFalse(withArmOnly.isValid)

        val withPositionOnly = withArmOnly.withBodyPosition(com.example.healthlog.model.BodyPosition.SITTING)
        assertFalse(withPositionOnly.isValid)

        val complete = withPositionOnly.withArrhythmia(false)
        assertTrue(complete.isValid)

        val measurement = complete.toMeasurement()!!
        assertEquals(120.0, measurement.value, 0.0)
        assertEquals(80.0, measurement.secondaryValue!!, 0.0)
        assertEquals(72.0, measurement.tertiaryValue!!, 0.0)
        assertEquals("LEFT", measurement.arm)
        assertEquals("SITTING", measurement.bodyPosition)
        assertEquals(false, measurement.hasArrhythmia)
    }

    @Test
    fun pulse_requiresArrhythmiaSelection() {
        val pulse = NewEntryState.initial(MeasurementType.PULSE)
        val withNumber = pulse.withText(0, "75")
        assertFalse(withNumber.isValid)

        val withArrhythmia = withNumber.withArrhythmia(true)
        assertTrue(withArrhythmia.isValid)
        val measurement = withArrhythmia.toMeasurement()!!
        assertEquals(75.0, measurement.value, 0.0)
        assertEquals(true, measurement.hasArrhythmia)
    }

    @Test
    fun bloodSugar_requiresMealContext() {
        val sugar = NewEntryState.initial(MeasurementType.BLOOD_SUGAR)
        val withNumber = sugar.withText(0, "105")
        assertFalse(withNumber.isValid)

        val withMeal = withNumber.withMealContext(com.example.healthlog.model.MealContext.BEFORE_MEAL)
        assertTrue(withMeal.isValid)
        val measurement = withMeal.toMeasurement()!!
        assertEquals(105.0, measurement.value, 0.0)
        assertEquals("BEFORE_MEAL", measurement.mealContext)
    }

    @Test
    fun customDateTime_isUsedWhenUseCurrentDateTimeIsFalse() {
        val customTime = 1700000000000L
        val measurement = weight
            .withText(0, "70")
            .withUseCurrentDateTime(false)
            .withCustomTimestamp(customTime)
            .toMeasurement()!!
        assertEquals(customTime, measurement.timestamp)
    }

    @Test
    fun uncheckingUseCurrentDateTime_resetsCustomTimestampToNow() {
        val time1 = 1000L
        val time2 = 2000L
        val time3 = 3000L

        // Initialized at time1
        val initial = NewEntryState.initial(MeasurementType.HEIGHT, now = time1)
        assertEquals(time1, initial.customTimestamp)
        assertTrue(initial.useCurrentDateTime)

        // Untick at time2 -> customTimestamp updates to time2 (now)
        val unticked = initial.withUseCurrentDateTime(false, now = time2)
        assertFalse(unticked.useCurrentDateTime)
        assertEquals(time2, unticked.customTimestamp)

        // Select a custom date/time
        val custom = unticked.withCustomTimestamp(999999L)
        assertEquals(999999L, custom.customTimestamp)

        // Tick auto again
        val reticked = custom.withUseCurrentDateTime(true)
        assertTrue(reticked.useCurrentDateTime)

        // Untick at time3 -> customTimestamp resets to time3 (now), not the old custom date
        val untickedAgain = reticked.withUseCurrentDateTime(false, now = time3)
        assertFalse(untickedAgain.useCurrentDateTime)
        assertEquals(time3, untickedAgain.customTimestamp)
    }

    @Test
    fun invalidState_cannotBeSaved() {
        assertNull(bloodPressure.withText(0, "80").withText(1, "120").withText(2, "72").toMeasurement())
    }

    @Test
    fun optionErrors_notShownInitially_orWhileSwitchingFields() {
        val bp = NewEntryState.initial(MeasurementType.BLOOD_PRESSURE)
        assertFalse(bp.showOptionErrors)
        assertFalse(bp.isArmError)
        assertFalse(bp.isBodyPositionError)
        assertFalse(bp.isArrhythmiaError)

        // User enters systolic and moves to diastolic field
        val movingToNext = bp.focusField(0).withText(0, "120").focusField(1)
        assertFalse(movingToNext.showOptionErrors)
        assertFalse(movingToNext.isArmError)
        assertFalse(movingToNext.isBodyPositionError)
        assertFalse(movingToNext.isArrhythmiaError)
    }

    @Test
    fun optionErrors_shownOnAttemptSave_andStickyUntilSelected() {
        val bp = NewEntryState.initial(MeasurementType.BLOOD_PRESSURE)
            .focusField(0).withText(0, "120")
            .focusField(1).withText(1, "80")
            .focusField(2).withText(2, "72")

        // Tapping Save or tapping elsewhere reveals all missing option errors
        val revealed = bp.attemptSave()
        assertTrue(revealed.showOptionErrors)
        assertTrue(revealed.isArmError)
        assertTrue(revealed.isBodyPositionError)
        assertTrue(revealed.isArrhythmiaError)

        // Selecting arm clears its error, while position and arrhythmia remain red
        val withArm = revealed.withArm(com.example.healthlog.model.MeasurementArm.LEFT)
        assertFalse(withArm.isArmError)
        assertTrue(withArm.isBodyPositionError)
        assertTrue(withArm.isArrhythmiaError)

        // User goes back to systolic field and enters invalid number "8"
        val editingSystolic = withArm.focusField(0).withText(0, "8")
        assertTrue("showOptionErrors must remain true while editing", editingSystolic.showOptionErrors)
        assertFalse(editingSystolic.isArmError)
        assertTrue("body position must remain in error", editingSystolic.isBodyPositionError)
        assertTrue("arrhythmia must remain in error", editingSystolic.isArrhythmiaError)

        // User leaves the field (or taps elsewhere again)
        val leftFieldAgain = editingSystolic.attemptSave()
        assertTrue("showOptionErrors must remain true after leaving field", leftFieldAgain.showOptionErrors)
        assertFalse(leftFieldAgain.isArmError)
        assertTrue("body position must remain in error", leftFieldAgain.isBodyPositionError)
        assertTrue("arrhythmia must remain in error", leftFieldAgain.isArrhythmiaError)

        // Selecting remaining options clears their respective errors one by one
        val withPosition = leftFieldAgain.withBodyPosition(com.example.healthlog.model.BodyPosition.SITTING)
        assertFalse(withPosition.isBodyPositionError)
        assertTrue(withPosition.isArrhythmiaError)

        val withArrhythmia = withPosition.withArrhythmia(false)
        assertFalse(withArrhythmia.isArrhythmiaError)
    }

    @Test
    fun attemptSave_immediatelyRevealsAllFieldErrorsAndOptionErrors() {
        val bp = NewEntryState.initial(MeasurementType.BLOOD_PRESSURE)

        // Initially no errors visible
        assertEquals(listOf(false, false, false), bp.leftFields)
        assertNull(bp.visibleError(0))
        assertNull(bp.visibleError(1))
        assertNull(bp.visibleError(2))
        assertFalse(bp.isArmError)
        assertFalse(bp.isBodyPositionError)
        assertFalse(bp.isArrhythmiaError)

        // User immediately clicks save on empty dialog
        val saved = bp.attemptSave()

        // All fields marked left, all required errors visible
        assertEquals(listOf(true, true, true), saved.leftFields)
        assertEquals(ValidationError.Required, saved.visibleError(0))
        assertEquals(ValidationError.Required, saved.visibleError(1))
        assertEquals(ValidationError.Required, saved.visibleError(2))

        // All option errors visible
        assertTrue(saved.showOptionErrors)
        assertTrue(saved.isArmError)
        assertTrue(saved.isBodyPositionError)
        assertTrue(saved.isArrhythmiaError)
    }
}
