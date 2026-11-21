package com.example.healthlog.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SymptomDomainTest {

    @Test
    fun symptomType_fromNameOrNull_matchesCorrectEnum() {
        assertEquals(SymptomType.HEADACHE, SymptomType.fromNameOrNull("HEADACHE"))
        assertEquals(SymptomType.HEADACHE, SymptomType.fromNameOrNull("headache"))
        assertEquals(SymptomType.PALPITATIONS, SymptomType.fromNameOrNull("PALPITATIONS"))
        assertEquals(SymptomType.CUSTOM, SymptomType.fromNameOrNull("custom"))
        assertNull(SymptomType.fromNameOrNull(null))
        assertNull(SymptomType.fromNameOrNull("UNKNOWN_SYMPTOM"))
    }

    @Test
    fun symptomSeverity_fromLevel_mapsLevelsAccurately() {
        assertEquals(SymptomSeverity.VERY_MILD, SymptomSeverity.fromLevel(1))
        assertEquals(SymptomSeverity.MILD, SymptomSeverity.fromLevel(2))
        assertEquals(SymptomSeverity.MODERATE, SymptomSeverity.fromLevel(3))
        assertEquals(SymptomSeverity.SEVERE, SymptomSeverity.fromLevel(4))
        assertEquals(SymptomSeverity.VERY_SEVERE, SymptomSeverity.fromLevel(5))

        // Fallback on invalid level
        assertEquals(SymptomSeverity.MODERATE, SymptomSeverity.fromLevel(0))
        assertEquals(SymptomSeverity.MODERATE, SymptomSeverity.fromLevel(99))
    }
}
