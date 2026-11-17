package com.example.healthlog.ui.export

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExportGeneratorsDeviceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    private val sampleMeasurements = listOf(
        Measurement(
            id = 1,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 120.0,
            secondaryValue = 80.0,
            tertiaryValue = 72.0,
            arm = "LEFT",
            bodyPosition = "SITTING",
            hasArrhythmia = false,
            timestamp = System.currentTimeMillis() - 100000L,
        ),
        Measurement(
            id = 2,
            type = MeasurementType.BLOOD_PRESSURE,
            value = 145.0,
            secondaryValue = 92.0,
            tertiaryValue = 78.0,
            arm = "RIGHT",
            bodyPosition = "SITTING",
            hasArrhythmia = true,
            timestamp = System.currentTimeMillis(),
        ),
    )

    @Test
    fun pdfReportGenerator_createsValidA4PdfFile() {
        val pdfGenerator = PdfReportGenerator(context)
        val tempFile = File(context.cacheDir, "test_report.pdf")

        val config = ExportConfig(
            format = ExportFormat.PDF,
            scope = ExportScope.CURRENT_TYPE,
            selectedType = MeasurementType.BLOOD_PRESSURE,
            dateFilter = null,
        )

        pdfGenerator.generateReport(
            config = config,
            measurements = sampleMeasurements,
            outputFile = tempFile,
        )

        assertTrue("Generated PDF should exist", tempFile.exists())
        assertTrue("Generated PDF should not be empty", tempFile.length() > 0)

        // Validate %PDF magic header
        val header = ByteArray(4)
        tempFile.inputStream().use { it.read(header) }
        assertEquals("%PDF", String(header))

        tempFile.delete()
    }

    @Test
    fun csvReportGenerator_createsValidUtf8BomCsvFile() {
        val csvGenerator = CsvReportGenerator(context)
        val tempFile = File(context.cacheDir, "test_data.csv")

        csvGenerator.generateReport(
            measurements = sampleMeasurements,
            outputFile = tempFile,
        )

        assertTrue("Generated CSV should exist", tempFile.exists())
        assertTrue("Generated CSV should not be empty", tempFile.length() > 0)

        val content = tempFile.readText(Charsets.UTF_8)
        assertTrue("CSV should contain header line", content.contains("Date,Time,Metric,Value"))
        assertTrue("CSV should contain systolic reading", content.contains("120.0"))
        assertTrue("CSV should contain diastolic reading", content.contains("80.0"))
        assertTrue("CSV should contain arrhythmia flag", content.contains("Yes"))

        tempFile.delete()
    }

    @Test
    fun exportFileManager_saveToDownloads_returnsValidUri() {
        val tempFile = File(context.cacheDir, "save_test_${System.currentTimeMillis()}.txt").apply {
            writeText("HealthLog export test")
        }

        val uri = ExportFileManager.saveToDownloads(
            context = context,
            file = tempFile,
            mimeType = "text/plain",
        )

        assertNotNull("Saving file to downloads should return non-null Uri", uri)
        tempFile.delete()
    }
}
