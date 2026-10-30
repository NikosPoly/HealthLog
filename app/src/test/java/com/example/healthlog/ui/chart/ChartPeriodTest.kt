package com.example.healthlog.ui.chart

import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ChartPeriodTest {

    private val zone = ZoneId.of("Europe/Athens")
    private val now = ZonedDateTime.of(2026, 8, 31, 10, 0, 0, 0, zone)

    private fun recordedAt(time: ZonedDateTime, id: Long = 0) = Measurement(
        id = id,
        type = MeasurementType.WEIGHT,
        value = 70.0,
        timestamp = time.toInstant().toEpochMilli()
    )

    @Test
    fun all_keepsEverything() {
        val measurements = listOf(recordedAt(now), recordedAt(now.minusYears(5)))

        assertEquals(measurements, measurements.inPeriod(ChartPeriod.ALL, now))
    }

    @Test
    fun sevenDays_startsAtTheSameTimeSevenDaysAgo() {
        val start = now.minusDays(7)
        val atStart = recordedAt(start, id = 1)
        val justBefore = recordedAt(start.minusMinutes(1), id = 2)

        assertEquals(listOf(atStart), listOf(atStart, justBefore).inPeriod(ChartPeriod.SEVEN_DAYS, now))
    }

    @Test
    fun thirtyDays_startsThirtyDaysAgo() {
        assertEquals(
            ZonedDateTime.of(2026, 8, 1, 10, 0, 0, 0, zone).toInstant().toEpochMilli(),
            ChartPeriod.THIRTY_DAYS.startTime(now)
        )
    }

    @Test
    fun sixMonths_fromTheEndOfAMonth_startsOnTheLastDayOfThatMonth() {
        assertEquals(
            ZonedDateTime.of(2026, 2, 28, 10, 0, 0, 0, zone).toInstant().toEpochMilli(),
            ChartPeriod.SIX_MONTHS.startTime(now)
        )
    }

    @Test
    fun oneYear_startsOnTheSameDateLastYear() {
        assertEquals(
            ZonedDateTime.of(2025, 8, 31, 10, 0, 0, 0, zone).toInstant().toEpochMilli(),
            ChartPeriod.ONE_YEAR.startTime(now)
        )
    }

    @Test
    fun inPeriod_keepsTheOriginalOrder() {
        val newer = recordedAt(now.minusDays(1), id = 1)
        val older = recordedAt(now.minusDays(2), id = 2)

        assertEquals(listOf(newer, older), listOf(newer, older).inPeriod(ChartPeriod.SEVEN_DAYS, now))
    }
}
