package com.example.healthlog.guidelines

import org.junit.Assert.assertEquals
import org.junit.Test

class PulseGuidelinesTest {

    @Test
    fun bradycardia_whenPulseBelow60() {
        assertEquals(PulseZone.BRADYCARDIA, evaluatePulse(59.0))
        assertEquals(PulseZone.BRADYCARDIA, evaluatePulse(45.0))
    }

    @Test
    fun normalPulse_atBoundaries() {
        assertEquals(PulseZone.NORMAL, evaluatePulse(60.0))
        assertEquals(PulseZone.NORMAL, evaluatePulse(72.0))
        assertEquals(PulseZone.NORMAL, evaluatePulse(100.0))
    }

    @Test
    fun tachycardia_whenPulseAbove100() {
        assertEquals(PulseZone.TACHYCARDIA, evaluatePulse(101.0))
        assertEquals(PulseZone.TACHYCARDIA, evaluatePulse(145.0))
    }
}
