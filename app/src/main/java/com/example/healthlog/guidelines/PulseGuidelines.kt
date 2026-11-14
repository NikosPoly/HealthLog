package com.example.healthlog.guidelines

// Resting heart rate zones according to the World Heart Federation (WHF) and
// World Health Organization (WHO) clinical consensus.
enum class PulseZone {
    BRADYCARDIA,    // < 60 bpm (resting)
    NORMAL,         // 60–100 bpm (resting)
    TACHYCARDIA     // > 100 bpm (resting)
}

// Evaluates a resting pulse reading into a clinical heart rate zone.
fun evaluatePulse(bpm: Double): PulseZone {
    return when {
        bpm < 60.0 -> PulseZone.BRADYCARDIA
        bpm > 100.0 -> PulseZone.TACHYCARDIA
        else -> PulseZone.NORMAL
    }
}
