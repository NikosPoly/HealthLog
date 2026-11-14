package com.example.healthlog.guidelines.protocols

import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementProtocolTest {

    @Test
    fun allMeasurementTypes_haveDefinedProtocol() {
        MeasurementType.entries.forEach { type ->
            val protocol = getMeasurementProtocol(type)
            assertEquals(type, protocol.type)
            assertNotEquals(0, protocol.titleRes)
            assertNotEquals(0, protocol.summaryRes)
            assertNotEquals(0, protocol.clinicalReferenceRes)
            assertTrue("Steps must not be empty for $type", protocol.steps.isNotEmpty())
            assertTrue("Tips must not be empty for $type", protocol.tips.isNotEmpty())

            protocol.steps.forEach { step ->
                assertNotEquals(0, step.titleRes)
                assertNotEquals(0, step.descriptionRes)
            }

            protocol.tips.forEach { tipRes ->
                assertNotEquals(0, tipRes)
            }
        }
    }

    @Test
    fun bloodPressureProtocol_hasCorrectStepCountAndTips() {
        val protocol = getMeasurementProtocol(MeasurementType.BLOOD_PRESSURE)
        assertEquals(4, protocol.steps.size)
        assertEquals(3, protocol.tips.size)
    }

    @Test
    fun bloodSugarProtocol_hasCorrectStepCountAndTips() {
        val protocol = getMeasurementProtocol(MeasurementType.BLOOD_SUGAR)
        assertEquals(4, protocol.steps.size)
        assertEquals(3, protocol.tips.size)
    }

    @Test
    fun pulseProtocol_hasCorrectStepCountAndTips() {
        val protocol = getMeasurementProtocol(MeasurementType.PULSE)
        assertEquals(3, protocol.steps.size)
        assertEquals(2, protocol.tips.size)
    }

    @Test
    fun weightProtocol_hasCorrectStepCountAndTips() {
        val protocol = getMeasurementProtocol(MeasurementType.WEIGHT)
        assertEquals(3, protocol.steps.size)
        assertEquals(2, protocol.tips.size)
    }

    @Test
    fun heightProtocol_hasCorrectStepCountAndTips() {
        val protocol = getMeasurementProtocol(MeasurementType.HEIGHT)
        assertEquals(3, protocol.steps.size)
        assertEquals(2, protocol.tips.size)
    }
}
