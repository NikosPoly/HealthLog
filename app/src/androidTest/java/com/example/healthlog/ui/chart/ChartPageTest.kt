package com.example.healthlog.ui.chart

import android.icu.text.RelativeDateTimeFormatter
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.theme.HealthLogTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

// Uses the trend page of one type with sample data: the chart or the empty-period message,
// the period stats (count, highest, lowest, average, blood pressure pairs) and the latest
// measurement. Tapping a dot isn't tested here: Vico draws the dots on a canvas, so the
// test can't find them; that stays a manual check.
// Runs on an emulator (it uninstalls the app afterwards, like every androidTest).
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

    // "yesterday", "3 hours ago"… as the app writes them in the emulator's language.
    private val relative = RelativeDateTimeFormatter.getInstance(
        Locale.forLanguageTag(context.getString(R.string.format_locale))
    )

    @Test
    fun withMeasurements_showsTheChartAndTheStats() {
        open(MeasurementType.PULSE, listOf(pulse(1, 70.0, hour)), ChartPeriod.ALL)

        chart(MeasurementType.PULSE).assertExists()
        text(R.string.stats_period_title).assertExists()
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
        exactly("80 bpm").assertCountEquals(1)
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

        // Highest, lowest, and the average 72.4633… → 72.46.
        exactly("72${separator}80 kg").assertCountEquals(1)
        exactly("72${separator}12 kg").assertCountEquals(1)
        exactly("72${separator}46 kg").assertCountEquals(1)
    }

    @Test
    fun oneMeasurement_isTheHighestLowestAndAverage() {
        open(MeasurementType.WEIGHT, listOf(weight(1, 72.5, hour)), ChartPeriod.ALL)

        exactly("1").assertCountEquals(1)
        exactly("72${separator}50 kg").assertCountEquals(3)
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
        // The average of both.
        exactly("130/85 mmHg").assertCountEquals(1)
        // No pulse anywhere.
        composeRule.onAllNodesWithText("bpm", substring = true).assertCountEquals(0)
    }

    @Test
    fun emptyPeriod_showsTheMessage_andStillTheLatestMeasurement() {
        open(MeasurementType.WEIGHT, listOf(weight(1, 72.5, 40 * day)), ChartPeriod.SEVEN_DAYS)

        text(R.string.chart_empty).assertExists()
        chart(MeasurementType.WEIGHT).assertDoesNotExist()
        text(R.string.stats_period_title).assertDoesNotExist()
        text(R.string.stats_last_recorded).assertExists()
        text(R.string.stats_latest_measurement).assertExists()
    }

    @Test
    fun noMeasurementsAtAll_showsOnlyTheMessage() {
        open(MeasurementType.HEIGHT, emptyList(), ChartPeriod.ALL)

        text(R.string.chart_empty).assertExists()
        text(R.string.stats_period_title).assertDoesNotExist()
        text(R.string.stats_last_recorded).assertDoesNotExist()
    }

    @Test
    fun lastRecorded_hoursAgo() {
        open(MeasurementType.WEIGHT, listOf(weight(1, 72.5, 3 * hour + 5 * minute)), ChartPeriod.ALL)

        val threeHoursAgo = relative.format(
            3.0,
            RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.RelativeUnit.HOURS
        )
        exactly(threeHoursAgo).assertCountEquals(1)
    }

    @Test
    fun lastRecorded_yesterday() {
        open(MeasurementType.WEIGHT, listOf(weight(1, 72.5, 30 * hour)), ChartPeriod.ALL)

        val yesterday = relative.format(
            RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.AbsoluteUnit.DAY
        )
        exactly(yesterday).assertCountEquals(1)
    }
}
