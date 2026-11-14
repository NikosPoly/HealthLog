package com.example.healthlog.guidelines.protocols

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.healthlog.R
import com.example.healthlog.model.MeasurementType

// Represents an individual step in a clinical measurement protocol.
data class ProtocolStep(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:DrawableRes val iconRes: Int? = null,
)

// Standardized clinical measurement protocol and key guidelines for a measurement type.
data class MeasurementProtocol(
    val type: MeasurementType,
    @param:StringRes val titleRes: Int,
    @param:StringRes val summaryRes: Int,
    val steps: List<ProtocolStep>,
    val tips: List<Int>,
    @param:StringRes val clinicalReferenceRes: Int,
)

// Returns the evidence-based clinical measurement protocol for a given type.
fun getMeasurementProtocol(type: MeasurementType): MeasurementProtocol {
    return when (type) {
        MeasurementType.BLOOD_PRESSURE -> MeasurementProtocol(
            type = MeasurementType.BLOOD_PRESSURE,
            titleRes = R.string.protocol_bp_title,
            summaryRes = R.string.protocol_bp_summary,
            steps = listOf(
                ProtocolStep(
                    titleRes = R.string.protocol_bp_step1_title,
                    descriptionRes = R.string.protocol_bp_step1_desc,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_bp_step2_title,
                    descriptionRes = R.string.protocol_bp_step2_desc,
                    iconRes = R.drawable.ic_position_sitting,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_bp_step3_title,
                    descriptionRes = R.string.protocol_bp_step3_desc,
                    iconRes = R.drawable.ic_arm_left,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_bp_step4_title,
                    descriptionRes = R.string.protocol_bp_step4_desc,
                ),
            ),
            tips = listOf(
                R.string.protocol_bp_tip1,
                R.string.protocol_bp_tip2,
                R.string.protocol_bp_tip3,
            ),
            clinicalReferenceRes = R.string.protocol_bp_reference,
        )

        MeasurementType.BLOOD_SUGAR -> MeasurementProtocol(
            type = MeasurementType.BLOOD_SUGAR,
            titleRes = R.string.protocol_glucose_title,
            summaryRes = R.string.protocol_glucose_summary,
            steps = listOf(
                ProtocolStep(
                    titleRes = R.string.protocol_glucose_step1_title,
                    descriptionRes = R.string.protocol_glucose_step1_desc,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_glucose_step2_title,
                    descriptionRes = R.string.protocol_glucose_step2_desc,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_glucose_step3_title,
                    descriptionRes = R.string.protocol_glucose_step3_desc,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_glucose_step4_title,
                    descriptionRes = R.string.protocol_glucose_step4_desc,
                ),
            ),
            tips = listOf(
                R.string.protocol_glucose_tip1,
                R.string.protocol_glucose_tip2,
                R.string.protocol_glucose_tip3,
            ),
            clinicalReferenceRes = R.string.protocol_glucose_reference,
        )

        MeasurementType.PULSE -> MeasurementProtocol(
            type = MeasurementType.PULSE,
            titleRes = R.string.protocol_pulse_title,
            summaryRes = R.string.protocol_pulse_summary,
            steps = listOf(
                ProtocolStep(
                    titleRes = R.string.protocol_pulse_step1_title,
                    descriptionRes = R.string.protocol_pulse_step1_desc,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_pulse_step2_title,
                    descriptionRes = R.string.protocol_pulse_step2_desc,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_pulse_step3_title,
                    descriptionRes = R.string.protocol_pulse_step3_desc,
                ),
            ),
            tips = listOf(
                R.string.protocol_pulse_tip1,
                R.string.protocol_pulse_tip2,
            ),
            clinicalReferenceRes = R.string.protocol_pulse_reference,
        )

        MeasurementType.WEIGHT -> MeasurementProtocol(
            type = MeasurementType.WEIGHT,
            titleRes = R.string.protocol_weight_title,
            summaryRes = R.string.protocol_weight_summary,
            steps = listOf(
                ProtocolStep(
                    titleRes = R.string.protocol_weight_step1_title,
                    descriptionRes = R.string.protocol_weight_step1_desc,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_weight_step2_title,
                    descriptionRes = R.string.protocol_weight_step2_desc,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_weight_step3_title,
                    descriptionRes = R.string.protocol_weight_step3_desc,
                    iconRes = R.drawable.ic_position_standing,
                ),
            ),
            tips = listOf(
                R.string.protocol_weight_tip1,
                R.string.protocol_weight_tip2,
            ),
            clinicalReferenceRes = R.string.protocol_weight_reference,
        )

        MeasurementType.HEIGHT -> MeasurementProtocol(
            type = MeasurementType.HEIGHT,
            titleRes = R.string.protocol_height_title,
            summaryRes = R.string.protocol_height_summary,
            steps = listOf(
                ProtocolStep(
                    titleRes = R.string.protocol_height_step1_title,
                    descriptionRes = R.string.protocol_height_step1_desc,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_height_step2_title,
                    descriptionRes = R.string.protocol_height_step2_desc,
                    iconRes = R.drawable.ic_position_standing,
                ),
                ProtocolStep(
                    titleRes = R.string.protocol_height_step3_title,
                    descriptionRes = R.string.protocol_height_step3_desc,
                ),
            ),
            tips = listOf(
                R.string.protocol_height_tip1,
                R.string.protocol_height_tip2,
            ),
            clinicalReferenceRes = R.string.protocol_height_reference,
        )
    }
}
