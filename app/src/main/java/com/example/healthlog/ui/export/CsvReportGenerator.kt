package com.example.healthlog.ui.export

import android.content.Context
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.guidelines.BloodGlucoseCategory
import com.example.healthlog.guidelines.BloodPressureStage
import com.example.healthlog.guidelines.BmiCategory
import com.example.healthlog.guidelines.GuidelineEvaluation
import com.example.healthlog.guidelines.PulseZone
import com.example.healthlog.guidelines.evaluateMeasurement
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.details.references.labelRes
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Streams exported clinical readings to standard comma-separated values (CSV) format.
class CsvReportGenerator(private val context: Context) {

    private fun resolveLocale(): Locale {
        val tag = context.getString(R.string.format_locale)
        return Locale.forLanguageTag(tag)
    }

    fun generateReport(
        measurements: List<Measurement>,
        outputFile: File,
        latestHeightCm: Double? = null,
    ) {
        val locale = resolveLocale()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", locale)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)

        FileOutputStream(outputFile).use { fos ->
            // Write UTF-8 BOM so spreadsheet tools (like Microsoft Excel) properly decode Greek characters.
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            OutputStreamWriter(fos, StandardCharsets.UTF_8).buffered().use { writer ->
                // Header row
                writer.appendLine("Date,Time,Metric,Value,Secondary Value,Pulse (bpm),Unit,Stage,Arm,Body Position,Meal Context,Arrhythmia")

                for (m in measurements) {
                    val zdt = Instant.ofEpochMilli(m.timestamp).atZone(ZoneId.systemDefault())
                    val date = dateFormatter.format(zdt)
                    val time = timeFormatter.format(zdt)
                    val metric = context.getString(m.type.label)
                    val value = m.value.toString()
                    val secVal = m.secondaryValue?.toString().orEmpty()
                    val pulse = m.tertiaryValue?.toInt()?.toString().orEmpty()
                    val unit = getUnitForType(m.type)

                    val evaluation = evaluateMeasurement(m, latestHeightCm)
                    val stage = if (evaluation != null) getStageName(evaluation) else ""

                    val arm = m.arm.orEmpty()
                    val position = m.bodyPosition.orEmpty()
                    val meal = m.mealContext.orEmpty()
                    val arrhythmia = when (m.hasArrhythmia) {
                        true -> "Yes"
                        false -> "No"
                        null -> ""
                    }

                    val row = listOf(
                        escapeCsv(date),
                        escapeCsv(time),
                        escapeCsv(metric),
                        escapeCsv(value),
                        escapeCsv(secVal),
                        escapeCsv(pulse),
                        escapeCsv(unit),
                        escapeCsv(stage),
                        escapeCsv(arm),
                        escapeCsv(position),
                        escapeCsv(meal),
                        escapeCsv(arrhythmia),
                    ).joinToString(",")

                    writer.appendLine(row)
                }
                writer.flush()
            }
        }
    }

    private fun getUnitForType(type: MeasurementType): String {
        return type.fields.firstOrNull()?.unit.orEmpty()
    }

    private fun getStageName(evaluation: GuidelineEvaluation): String {
        return when (evaluation) {
            is GuidelineEvaluation.BloodPressure -> context.getString(evaluation.stage.labelRes)
            is GuidelineEvaluation.BloodGlucose -> context.getString(evaluation.category.labelRes)
            is GuidelineEvaluation.Pulse -> context.getString(evaluation.zone.labelRes)
            is GuidelineEvaluation.Weight -> evaluation.bmiEvaluation?.category?.let { context.getString(it.labelRes) }.orEmpty()
            is GuidelineEvaluation.Height -> ""
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
