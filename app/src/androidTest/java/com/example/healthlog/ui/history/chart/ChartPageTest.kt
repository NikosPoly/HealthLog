package com.example.healthlog.ui.history.chart

import android.icu.text.RelativeDateTimeFormatter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.BodyPosition
import com.example.healthlog.model.MeasurementArm
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

// Uses the chart page of one type with sample data: the period card (the chart or the
// empty-period message, and count, highest, lowest, average, blood pressure pairs) and the
// non-period stats card (the last entry and when it was recorded). Tapping a dot isn't tested here: Vico draws the dots on a canvas, so the
// test can't find them; that stays a manual check.
// Runs on an emulator (it uninstalls the app afterward, like every androidTest).
@RunWith(AndroidJUnit4::class)
class ChartPageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // The decimal separator of the emulator's language, for the weight texts.
    private val separator = context.getString(R.string.decimal_separator)

    private val now = System.currentTimeMillis()
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    private fun open(type: MeasurementType, newestFirst: List<Measurement>, period: ChartPeriod) {
        composeRule.setContent {
            HealthLogTheme {
                ChartPage(
                    type = type,
                    measurements = newestFirst,
                    period = period,
                    screenTap = null
                )
            }
        }
    }

    private fun text(id: Int) = composeRule.onNodeWithText(context.getString(id))
    private fun exactly(text: String) = composeRule.onAllNodesWithText(text)
    private fun containing(text: String) = composeRule.onAllNodesWithText(text, substring = true)

    private fun chart(type: MeasurementType) = composeRule.onNodeWithContentDescription(
        context.getString(R.string.chart_description, context.getString(type.label))
    )

    private fun pulse(id: Long, value: Double, ago: Long) =
        Measurement(id = id, type = MeasurementType.PULSE, value = value, timestamp = now - ago)

    private fun weight(id: Long, value: Double, ago: Long) =
        Measurement(id = id, type = MeasurementType.WEIGHT, value = value, timestamp = now - ago)

    private fun bloodPressure(id: Long, systolic: Double, diastolic: Double, ago: Long) =
        Measurement(
            id = id,
            type = MeasurementType.BLOOD_PRESSURE,
            value = systolic,
            secondaryValue = diastolic,
            tertiaryValue = 70.0,
            timestamp = now - ago
        )

    private fun bloodPressureWithConditions(
        id: Long,
        systolic: Double,
        diastolic: Double,
        ago: Long,
        arm: MeasurementArm? = null,
        position: BodyPosition? = null,
        arrhythmia: Boolean? = null
    ) = Measurement(
        id = id,
        type = MeasurementType.BLOOD_PRESSURE,
        value = systolic,
        secondaryValue = diastolic,
        tertiaryValue = 70.0,
        arm = arm?.name,
        bodyPosition = position?.name,
        hasArrhythmia = arrhythmia,
        timestamp = now - ago
    )

    // "yesterday", "3 hours ago"… as the app writes them in the emulator's language.
    private val relative = RelativeDateTimeFormatter.getInstance(
        Locale.forLanguageTag(context.getString(R.string.format_locale))
    )

    @Test
    fun withMeasurements_showsTheChartAndTheStats() {
        open(MeasurementType.PULSE, listOf(pulse(1, 70.0, hour)), ChartPeriod.ALL)

        chart(MeasurementType.PULSE).assertExists()
        text(R.string.stats_average).assertExists()
        text(R.string.chart_empty).assertDoesNotExist()
    }

    @Test
    fun pulse_countHighestLowestAverage() {
        val measurements = listOf(
            pulse(1, 80.0, hour),
            pulse(2, 75.0, 2 * hour),
            pulse(3, 70.0, 3 * hour)
        )
        open(MeasurementType.PULSE, measurements, ChartPeriod.ALL)

        exactly("3").assertCountEquals(1)
        // Highest, and the last entry below.
        exactly("80 bpm").assertCountEquals(2)
        exactly("70 bpm").assertCountEquals(1)
        exactly("75 bpm").assertCountEquals(1)
    }

    @Test
    fun weight_averageIsRoundedToTwoDecimals() {
        val measurements = listOf(
            weight(1, 72.12, hour),
            weight(2, 72.80, 2 * hour),
            weight(3, 72.47, 3 * hour)
        )
        open(MeasurementType.WEIGHT, measurements, ChartPeriod.ALL)

        // Highest, lowest (also the last entry), and the average 72.4633… → 72.46.
        exactly("72${separator}80 kg").assertCountEquals(1)
        exactly("72${separator}12 kg").assertCountEquals(2)
        exactly("72${separator}46 kg").assertCountEquals(1)
    }

    @Test
    fun oneMeasurement_isTheHighestLowestAndAverage() {
        open(MeasurementType.WEIGHT, listOf(weight(1, 72.5, hour)), ChartPeriod.ALL)

        exactly("1").assertCountEquals(1)
        // Highest, lowest, average, and the last entry.
        exactly("72${separator}50 kg").assertCountEquals(4)
    }

    @Test
    fun bloodPressure_showsThePairs_andTheAverage_withoutThePulse() {
        val measurements = listOf(
            bloodPressure(1, systolic = 130.0, diastolic = 85.0, ago = hour),
            bloodPressure(2, systolic = 140.0, diastolic = 80.0, ago = 2 * hour),
            bloodPressure(3, systolic = 120.0, diastolic = 90.0, ago = 3 * hour)
        )
        open(MeasurementType.BLOOD_PRESSURE, measurements, ChartPeriod.ALL)

        // 140/80: highest systolic and lowest diastolic.
        exactly("140/80 mmHg").assertCountEquals(2)
        // 120/90: lowest systolic and highest diastolic.
        exactly("120/90 mmHg").assertCountEquals(2)
        // The average of both, and the last entry (which happens to be the same).
        exactly("130/85 mmHg").assertCountEquals(2)
        // No pulse anywhere.
        composeRule.onAllNodesWithText("bpm", substring = true).assertCountEquals(0)
    }

    @Test
    fun emptyPeriod_showsTheMessage_andStillTheLastEntry() {
        open(MeasurementType.WEIGHT, listOf(weight(1, 72.5, 40 * day)), ChartPeriod.SEVEN_DAYS)

        text(R.string.chart_empty).assertExists()
        chart(MeasurementType.WEIGHT).assertDoesNotExist()
        text(R.string.stats_average).assertDoesNotExist()
        text(R.string.stats_last_entry).assertExists()
    }

    @Test
    fun noMeasurementsAtAll_showsOnlyTheMessage() {
        open(MeasurementType.HEIGHT, emptyList(), ChartPeriod.ALL)

        text(R.string.chart_empty).assertExists()
        text(R.string.stats_average).assertDoesNotExist()
        text(R.string.stats_last_entry).assertDoesNotExist()
    }

    @Test
    fun lastRecorded_hoursAgo() {
        open(MeasurementType.WEIGHT, listOf(weight(1, 72.5, 3 * hour + 5 * minute)), ChartPeriod.ALL)

        val threeHoursAgo = relative.format(
            3.0,
            RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.RelativeUnit.HOURS
        )
        // "3 hours ago · <date>": the date follows, so it's part of a longer text.
        containing(threeHoursAgo).assertCountEquals(1)
    }

    @Test
    fun lastRecorded_yesterday() {
        open(MeasurementType.WEIGHT, listOf(weight(1, 72.5, 30 * hour)), ChartPeriod.ALL)

        val yesterday = relative.format(
            RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.AbsoluteUnit.DAY
        )
        containing(yesterday).assertCountEquals(1)
    }

    @Test
    fun bloodPressure_conditionBreakdown_displaysArmAndPositionAndArrhythmia() {
        val measurements = listOf(
            bloodPressureWithConditions(
                id = 1,
                systolic = 120.0,
                diastolic = 80.0,
                ago = hour,
                arm = MeasurementArm.LEFT,
                position = BodyPosition.SITTING,
                arrhythmia = false
            ),
            bloodPressureWithConditions(
                id = 2,
                systolic = 130.0,
                diastolic = 85.0,
                ago = 2 * hour,
                arm = MeasurementArm.RIGHT,
                position = BodyPosition.STANDING,
                arrhythmia = true
            )
        )
        open(MeasurementType.BLOOD_PRESSURE, measurements, ChartPeriod.ALL)

        text(R.string.insights_title).assertExists()
        text(R.string.section_arm).assertExists()
        text(R.string.section_position).assertExists()
        text(R.string.section_arrhythmia).assertExists()
        // 4 distribution bar percentages (Left, Right, Sit, Stand) + 1 in arrhythmia summary
        containing("50%").assertCountEquals(5)
        exactly("50% ").assertCountEquals(4)
    }

    @Test
    fun conditionBreakdown_updatesWhenSwitchingPeriods() {
        val recentLeft = bloodPressureWithConditions(
            id = 1,
            systolic = 120.0,
            diastolic = 80.0,
            ago = hour,
            arm = MeasurementArm.LEFT,
            position = BodyPosition.SITTING
        )
        val oldRight = bloodPressureWithConditions(
            id = 2,
            systolic = 130.0,
            diastolic = 85.0,
            ago = 30 * day,
            arm = MeasurementArm.RIGHT,
            position = BodyPosition.SITTING
        )
        val measurements = listOf(recentLeft, oldRight)

        var period by mutableStateOf(ChartPeriod.SEVEN_DAYS)
        composeRule.setContent {
            HealthLogTheme {
                ChartPage(
                    type = MeasurementType.BLOOD_PRESSURE,
                    measurements = measurements,
                    period = period,
                    screenTap = null
                )
            }
        }

        // In 7 days: only recentLeft is in period -> Left is 100% (1/1)
        text(R.string.insights_title).assertExists()
        containing("100%").assertCountEquals(2) // Left 100%, Sit 100%
        containing("(1/1)").assertCountEquals(2)

        // Switch to ALL: both are in period -> Left is 50%, Right is 50%
        period = ChartPeriod.ALL
        composeRule.waitForIdle()

        containing("50%").assertCountEquals(2) // Left 50%, Right 50%
        containing("(1/2)").assertCountEquals(2)
    }

    @Test
    fun weight_doesNotShowConditionBreakdown() {
        open(MeasurementType.WEIGHT, listOf(weight(1, 72.5, hour)), ChartPeriod.ALL)

        text(R.string.insights_title).assertDoesNotExist()
    }

    @Test
    fun emptyPeriod_doesNotShowConditionBreakdown() {
        val oldMeasurement = bloodPressureWithConditions(
            id = 1,
            systolic = 120.0,
            diastolic = 80.0,
            ago = 40 * day,
            arm = MeasurementArm.LEFT
        )
        open(MeasurementType.BLOOD_PRESSURE, listOf(oldMeasurement), ChartPeriod.SEVEN_DAYS)

        text(R.string.chart_empty).assertExists()
        text(R.string.insights_title).assertDoesNotExist()
    }
}
