package com.example.healthlog.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeAgoTest {

    private val zone = ZoneId.of("Europe/Athens")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)

    // A measurement on 12 February 2026 at 12:00.
    private val recorded = at(2026, 2, 12, 12)

    // How long before `now` the measurement was.
    private fun agoAt(now: ZonedDateTime, from: ZonedDateTime = recorded) =
        timeAgo(from.toInstant().toEpochMilli(), now)

    @Test
    fun underAMinute_isNow() {
        assertEquals(TimeAgo(0, TimeAgo.Span.NOW), agoAt(recorded.plusSeconds(30)))
    }

    @Test
    fun inTheFuture_isNow() {
        assertEquals(TimeAgo(0, TimeAgo.Span.NOW), agoAt(recorded.minusMinutes(5)))
    }

    @Test
    fun underAnHour_isMinutes() {
        assertEquals(TimeAgo(5, TimeAgo.Span.MINUTES), agoAt(at(2026, 2, 12, 12, 5)))
    }

    @Test
    fun under24Hours_isHours_evenOnTheNextDay() {
        assertEquals(TimeAgo(3, TimeAgo.Span.HOURS), agoAt(at(2026, 2, 12, 15)))
        assertEquals(TimeAgo(22, TimeAgo.Span.HOURS), agoAt(at(2026, 2, 13, 10)))
        assertEquals(TimeAgo(23, TimeAgo.Span.HOURS), agoAt(at(2026, 2, 13, 11, 59)))
    }

    @Test
    fun from24Hours_isYesterday_until48Hours() {
        assertEquals(TimeAgo(1, TimeAgo.Span.YESTERDAY), agoAt(at(2026, 2, 13, 12)))
        assertEquals(TimeAgo(1, TimeAgo.Span.YESTERDAY), agoAt(at(2026, 2, 14, 11, 59)))
    }

    @Test
    fun then_days_untilTheSameTimeNextMonth() {
        assertEquals(TimeAgo(2, TimeAgo.Span.DAYS), agoAt(at(2026, 2, 14, 12)))
        assertEquals(TimeAgo(27, TimeAgo.Span.DAYS), agoAt(at(2026, 3, 12, 11, 59)))
    }

    @Test
    fun then_months_untilTheSameTimeNextYear() {
        assertEquals(TimeAgo(1, TimeAgo.Span.MONTHS), agoAt(at(2026, 3, 12, 12)))
        assertEquals(TimeAgo(2, TimeAgo.Span.MONTHS), agoAt(at(2026, 4, 12, 12)))
        assertEquals(TimeAgo(11, TimeAgo.Span.MONTHS), agoAt(at(2027, 2, 12, 11, 59)))
    }

    @Test
    fun then_years() {
        assertEquals(TimeAgo(1, TimeAgo.Span.YEARS), agoAt(at(2027, 2, 12, 12)))
        assertEquals(TimeAgo(2, TimeAgo.Span.YEARS), agoAt(at(2028, 2, 12, 12)))
    }

    @Test
    fun recordedOn31January_monthIsCompleteOnThe1stOfMarch() {
        val endOfJanuary = at(2026, 1, 31, 10)

        assertEquals(TimeAgo(28, TimeAgo.Span.DAYS), agoAt(at(2026, 2, 28, 10), from = endOfJanuary))
        assertEquals(TimeAgo(1, TimeAgo.Span.MONTHS), agoAt(at(2026, 3, 1, 10), from = endOfJanuary))
    }

    @Test
    fun overTheSummerTimeChange_countsRealHours() {
        // In Greece the clocks move forward on 29 March 2026, 03:00 → 04:00.
        val beforeChange = at(2026, 3, 28, 12)

        assertEquals(TimeAgo(22, TimeAgo.Span.HOURS), agoAt(at(2026, 3, 29, 11, 30), from = beforeChange))
        assertEquals(TimeAgo(1, TimeAgo.Span.YESTERDAY), agoAt(at(2026, 3, 29, 12), from = beforeChange))
    }
}
