package com.example.healthlog.model

import androidx.annotation.StringRes
import com.example.healthlog.R

enum class SymptomType(
    @StringRes val titleRes: Int,
) {
    HEADACHE(R.string.symptom_headache),
    DIZZINESS(R.string.symptom_dizziness),
    PALPITATIONS(R.string.symptom_palpitations),
    CHEST_DISCOMFORT(R.string.symptom_chest_discomfort),
    SHORTNESS_OF_BREATH(R.string.symptom_shortness_of_breath),
    FATIGUE(R.string.symptom_fatigue),
    NAUSEA(R.string.symptom_nausea),
    CUSTOM(R.string.symptom_custom);

    companion object {
        fun fromNameOrNull(name: String?): SymptomType? {
            if (name == null) return null
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }
    }
}
