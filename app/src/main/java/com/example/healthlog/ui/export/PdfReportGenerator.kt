package com.example.healthlog.ui.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Generates a native clinical PDF doctor report formatted for A4 print and documentation.
class PdfReportGenerator(private val context: Context) {

    companion object {
        // A4 Dimensions in PostScript points at 72 DPI
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
        const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

        private const val COLOR_PRIMARY = 0xFF1E88E5.toInt()
        private const val COLOR_SECONDARY = 0xFF0D47A1.toInt()
        private const val COLOR_SURFACE = 0xFFF5F5F5.toInt()
        private const val COLOR_LINE = 0xFFE0E0E0.toInt()
        private const val COLOR_TEXT_MAIN = 0xFF212121.toInt()
        private const val COLOR_TEXT_MUTED = 0xFF757575.toInt()
        private const val COLOR_STAGE_ALERT = 0xFFD32F2F.toInt()
        private const val COLOR_STAGE_WARN = 0xFFF57C00.toInt()
        private const val COLOR_STAGE_NORMAL = 0xFF388E3C.toInt()
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_PRIMARY
        textSize = 18f
        isFakeBoldText = true
    }

    private val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_SECONDARY
        textSize = 12f
        isFakeBoldText = true
    }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_MAIN
        textSize = 9.5f
    }

    private val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_MAIN
        textSize = 9.5f
        isFakeBoldText = true
    }

    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_MUTED
        textSize = 8.5f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_LINE
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val bgBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_SURFACE
        style = Paint.Style.FILL
    }

    // Resolves current formatting locale based on strings.xml
    private fun resolveLocale(): Locale {
        val tag = context.getString(R.string.format_locale)
        return Locale.forLanguageTag(tag)
    }

    // Formats numbers consistently with decimal separator
    private fun formatDec(value: Double, decimals: Int): String {
        val decSep = context.getString(R.string.decimal_separator).first()
        return String.format(Locale.ROOT, "%.${decimals}f", value).replace('.', decSep)
    }

    // Generates the PDF report and writes it to the destination file.
    fun generateReport(
        config: ExportConfig,
        measurements: List<Measurement>,
        outputFile: File,
        latestHeightCm: Double? = null,
    ) {
        val document = PdfDocument()
        val locale = resolveLocale()

        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var yPos = MARGIN

        // 1. Header (Brand, title, date range)
        yPos = drawHeader(canvas, config, locale, yPos)

        // 2. Summary Statistics Box
        yPos = drawSummaryStats(canvas, config, measurements, locale, yPos)

        // 3. Mini-Trajectory Graph (if measurements >= 2)
        if (measurements.size >= 2) {
            yPos = drawTrajectoryGraph(canvas, config, measurements, yPos)
        }

        // 4. Chronological Table Header
        yPos = drawTableHeader(canvas, yPos)

        val rowHeight = 22f

        for (measurement in measurements) {
            // Check if page overflow requires a new page
            if (yPos + rowHeight > PAGE_HEIGHT - MARGIN - 20f) {
                drawPageFooter(canvas, currentPageNumber)
                document.finishPage(page)

                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = MARGIN

                // On subsequent pages, draw a mini header and repeat table header
                yPos = drawSubsequentPageHeader(canvas, yPos)
                yPos = drawTableHeader(canvas, yPos)
            }

            yPos = drawTableRow(canvas, measurement, locale, yPos, latestHeightCm)
        }

        drawPageFooter(canvas, currentPageNumber)
        document.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun drawHeader(
        canvas: Canvas,
        config: ExportConfig,
        locale: Locale,
        startY: Float,
    ): Float {
        var y = startY

        // Title
        val appName = context.getString(R.string.app_name)
        val reportTitle = context.getString(R.string.export_title)
        canvas.drawText("$appName – $reportTitle", MARGIN, y + 16f, titlePaint)

        // Date generated
        val now = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        val generatedText = dateFormatter.format(now)
        val genTextWidth = mutedPaint.measureText(generatedText)
        canvas.drawText(generatedText, PAGE_WIDTH - MARGIN - genTextWidth, y + 16f, mutedPaint)

        y += 26f

        // Metric scope & Date filter subtitle
        val scopeText = when (config.scope) {
            ExportScope.CURRENT_TYPE -> context.getString(config.selectedType.label)
            ExportScope.ALL_TYPES -> context.getString(R.string.export_scope_all)
        }
        val dateRangeText = when (val filter = config.dateFilter) {
            null -> context.getString(R.string.date_filter_all_time)
            is com.example.healthlog.ui.history.filter.DateFilter.Today -> context.getString(R.string.date_filter_today)
            is com.example.healthlog.ui.history.filter.DateFilter.Last7Days -> context.getString(R.string.date_filter_7_days)
            is com.example.healthlog.ui.history.filter.DateFilter.Last30Days -> context.getString(R.string.date_filter_30_days)
            is com.example.healthlog.ui.history.filter.DateFilter.CustomRange -> filter.displayLabel(locale)
        }

        canvas.drawText("$scopeText  •  $dateRangeText", MARGIN, y + 10f, sectionHeaderPaint)
        y += 18f

        // Divider
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 14f

        return y
    }

    private fun drawSummaryStats(
        canvas: Canvas,
        config: ExportConfig,
        measurements: List<Measurement>,
        locale: Locale,
        startY: Float,
    ): Float {
        var y = startY
        val boxHeight = 58f
        val boxRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + boxHeight)
        canvas.drawRoundRect(boxRect, 8f, 8f, bgBoxPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_LINE
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(boxRect, 8f, 8f, borderPaint)

        val totalReadings = measurements.size
        val countLabel = context.getString(R.string.stats_count)
        val avgLabel = context.getString(R.string.stats_average)
        val highestLabel = context.getString(R.string.stats_highest)
        val lowestLabel = context.getString(R.string.stats_lowest)

        val colWidth = CONTENT_WIDTH / 4f
        val col1X = MARGIN + 12f
        val col2X = MARGIN + colWidth + 8f
        val col3X = MARGIN + (colWidth * 2) + 8f
        val col4X = MARGIN + (colWidth * 3) + 8f

        // Total count
        canvas.drawText(countLabel.uppercase(locale), col1X, y + 18f, mutedPaint)
        canvas.drawText("$totalReadings", col1X, y + 38f, boldBodyPaint)

        // Average
        val primaryValues = measurements.map { it.value }
        val avg = if (primaryValues.isNotEmpty()) primaryValues.average() else 0.0
        canvas.drawText(avgLabel.uppercase(locale), col2X, y + 18f, mutedPaint)
        canvas.drawText(formatDec(avg, 1), col2X, y + 38f, boldBodyPaint)

        // Lowest
        val lowest = primaryValues.minOrNull() ?: 0.0
        canvas.drawText(lowestLabel.uppercase(locale), col3X, y + 18f, mutedPaint)
        canvas.drawText(formatDec(lowest, 1), col3X, y + 38f, boldBodyPaint)

        // Highest
        val highest = primaryValues.maxOrNull() ?: 0.0
        canvas.drawText(highestLabel.uppercase(locale), col4X, y + 18f, mutedPaint)
        canvas.drawText(formatDec(highest, 1), col4X, y + 38f, boldBodyPaint)

        y += boxHeight + 14f
        return y
    }

    private fun drawTrajectoryGraph(
        canvas: Canvas,
        config: ExportConfig,
        measurements: List<Measurement>,
        startY: Float,
    ): Float {
        var y = startY
        val graphHeight = 80f
        val graphWidth = CONTENT_WIDTH
        val graphRect = RectF(MARGIN, y, MARGIN + graphWidth, y + graphHeight)

        canvas.drawRoundRect(graphRect, 6f, 6f, bgBoxPaint)

        val sorted = measurements.sortedBy { it.timestamp }
        val values = sorted.map { it.value }
        val minVal = (values.minOrNull() ?: 0.0) * 0.95
        val maxVal = (values.maxOrNull() ?: 100.0) * 1.05
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        val paddingX = 16f
        val paddingY = 12f
        val plotWidth = graphWidth - (paddingX * 2)
        val plotHeight = graphHeight - (paddingY * 2)

        val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            strokeWidth = 2.0f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY
            style = Paint.Style.FILL
        }

        val stepX = plotWidth / (sorted.size - 1).coerceAtLeast(1)

        var prevX = 0f
        var prevY = 0f

        sorted.forEachIndexed { index, m ->
            val curX = MARGIN + paddingX + (index * stepX)
            val normalizedY = ((m.value - minVal) / range).toFloat()
            val curY = y + graphHeight - paddingY - (normalizedY * plotHeight)

            if (index > 0) {
                canvas.drawLine(prevX, prevY, curX, curY, pathPaint)
            }
            canvas.drawCircle(curX, curY, 3f, dotPaint)

            prevX = curX
            prevY = curY
        }

        canvas.drawText(formatDec(maxVal, 0), MARGIN + 4f, y + 12f, mutedPaint)
        canvas.drawText(formatDec(minVal, 0), MARGIN + 4f, y + graphHeight - 4f, mutedPaint)

        y += graphHeight + 14f
        return y
    }

    private fun drawTableHeader(
        canvas: Canvas,
        startY: Float,
    ): Float {
        var y = startY
        val headerHeight = 20f

        val headerRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + headerHeight)
        canvas.drawRect(headerRect, bgBoxPaint)

        val dateLabel = context.getString(R.string.filter_section_date)
        val stageLabel = context.getString(R.string.filter_section_stage)
        val conditionsLabel = context.getString(R.string.filter_section_conditions)

        val colDateX = MARGIN + 6f
        val colTimeX = MARGIN + 85f
        val colValueX = MARGIN + 155f
        val colStageX = MARGIN + 270f
        val colCondX = MARGIN + 390f

        canvas.drawText(dateLabel, colDateX, y + 14f, sectionHeaderPaint)
        canvas.drawText("Time", colTimeX, y + 14f, sectionHeaderPaint)
        canvas.drawText("Reading", colValueX, y + 14f, sectionHeaderPaint)
        canvas.drawText(stageLabel, colStageX, y + 14f, sectionHeaderPaint)
        canvas.drawText(conditionsLabel, colCondX, y + 14f, sectionHeaderPaint)

        y += headerHeight
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)

        return y
    }

    private fun drawTableRow(
        canvas: Canvas,
        measurement: Measurement,
        locale: Locale,
        startY: Float,
        latestHeightCm: Double?,
    ): Float {
        val y = startY
        val rowHeight = 22f

        val zdt = Instant.ofEpochMilli(measurement.timestamp).atZone(ZoneId.systemDefault())
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)

        val dateText = dateFormatter.format(zdt)
        val timeText = timeFormatter.format(zdt)

        val colDateX = MARGIN + 6f
        val colTimeX = MARGIN + 85f
        val colValueX = MARGIN + 155f
        val colStageX = MARGIN + 270f
        val colCondX = MARGIN + 390f

        canvas.drawText(dateText, colDateX, y + 14f, bodyPaint)
        canvas.drawText(timeText, colTimeX, y + 14f, mutedPaint)

        val valueStr = formatMeasurementSummary(measurement)
        canvas.drawText(valueStr, colValueX, y + 14f, boldBodyPaint)

        // Stage evaluation
        val evaluation = evaluateMeasurement(measurement, latestHeightCm)
        if (evaluation != null) {
            val (stageName, isAlert, isWarn) = getStageInfo(evaluation)
            val stageColor = when {
                isAlert -> COLOR_STAGE_ALERT
                isWarn -> COLOR_STAGE_WARN
                else -> COLOR_STAGE_NORMAL
            }
            val stagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stageColor
                textSize = 9f
                isFakeBoldText = true
            }
            canvas.drawText(stageName, colStageX, y + 14f, stagePaint)
        } else {
            canvas.drawText("—", colStageX, y + 14f, mutedPaint)
        }

        val conditionsSummary = formatConditions(measurement)
        canvas.drawText(conditionsSummary, colCondX, y + 14f, mutedPaint)

        canvas.drawLine(MARGIN, y + rowHeight, PAGE_WIDTH - MARGIN, y + rowHeight, linePaint)

        return y + rowHeight
    }

    private fun getStageInfo(evaluation: GuidelineEvaluation): Triple<String, Boolean, Boolean> {
        return when (evaluation) {
            is GuidelineEvaluation.BloodPressure -> {
                val isAlert = evaluation.stage in listOf(
                    BloodPressureStage.GRADE_1_HYPERTENSION,
                    BloodPressureStage.GRADE_2_HYPERTENSION,
                    BloodPressureStage.HYPERTENSIVE_CRISIS,
                )
                val isWarn = evaluation.stage in listOf(
                    BloodPressureStage.LOW,
                    BloodPressureStage.HIGH_NORMAL,
                )
                Triple(context.getString(evaluation.stage.labelRes), isAlert, isWarn)
            }
            is GuidelineEvaluation.BloodGlucose -> {
                val isAlert = evaluation.category in listOf(
                    BloodGlucoseCategory.HYPOGLYCEMIA,
                    BloodGlucoseCategory.DIABETES,
                )
                val isWarn = evaluation.category == BloodGlucoseCategory.PREDIABETES
                Triple(context.getString(evaluation.category.labelRes), isAlert, isWarn)
            }
            is GuidelineEvaluation.Pulse -> {
                val isAlert = evaluation.zone != PulseZone.NORMAL
                Triple(context.getString(evaluation.zone.labelRes), isAlert, false)
            }
            is GuidelineEvaluation.Weight -> {
                val cat = evaluation.bmiEvaluation?.category
                if (cat != null) {
                    val isAlert = cat == BmiCategory.OBESE
                    val isWarn = cat in listOf(BmiCategory.UNDERWEIGHT, BmiCategory.OVERWEIGHT)
                    Triple(context.getString(cat.labelRes), isAlert, isWarn)
                } else {
                    Triple("—", false, false)
                }
            }
            is GuidelineEvaluation.Height -> Triple("—", false, false)
        }
    }

    private fun formatMeasurementSummary(measurement: Measurement): String {
        return when (measurement.type) {
            MeasurementType.BLOOD_PRESSURE -> {
                val sys = measurement.value.toInt()
                val dia = measurement.secondaryValue?.toInt() ?: 0
                val pulse = measurement.tertiaryValue?.toInt()
                if (pulse != null && pulse > 0) {
                    "$sys/$dia ($pulse bpm)"
                } else {
                    "$sys/$dia mmHg"
                }
            }
            MeasurementType.PULSE -> "${measurement.value.toInt()} bpm"
            MeasurementType.BLOOD_SUGAR -> "${formatDec(measurement.value, 1)} mg/dL"
            MeasurementType.WEIGHT -> "${formatDec(measurement.value, 1)} kg"
            MeasurementType.HEIGHT -> "${formatDec(measurement.value, 1)} cm"
        }
    }

    private fun formatConditions(measurement: Measurement): String {
        val list = mutableListOf<String>()
        measurement.arm?.let {
            list.add(it)
        }
        measurement.bodyPosition?.let {
            list.add(it)
        }
        measurement.mealContext?.let {
            list.add(it)
        }
        if (measurement.hasArrhythmia == true) {
            list.add("Arrhythmia")
        }
        return if (list.isNotEmpty()) list.joinToString(", ") else "—"
    }

    private fun drawSubsequentPageHeader(
        canvas: Canvas,
        startY: Float,
    ): Float {
        var y = startY
        val appName = context.getString(R.string.app_name)
        val reportTitle = context.getString(R.string.export_title)

        canvas.drawText("$appName – $reportTitle", MARGIN, y + 14f, sectionHeaderPaint)
        y += 24f
        return y
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int) {
        val footerText = "Page $pageNumber"
        val footerWidth = mutedPaint.measureText(footerText)
        canvas.drawText(
            footerText,
            (PAGE_WIDTH - footerWidth) / 2f,
            PAGE_HEIGHT - MARGIN + 10f,
            mutedPaint,
        )
    }
}
