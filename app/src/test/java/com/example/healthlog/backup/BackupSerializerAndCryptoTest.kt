package com.example.healthlog.backup

import com.example.healthlog.database.Measurement
import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.database.UserProfile
import com.example.healthlog.model.MeasurementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import javax.crypto.AEADBadTagException

class BackupSerializerAndCryptoTest {

    private fun sampleBackupData(): BackupData {
        val profile = UserProfile(
            name = "Nikos",
            dateOfBirth = "1990-05-15",
            gender = "MALE",
            updatedAt = 1000L
        )

        val measurements = listOf(
            Measurement(
                id = 1L,
                type = MeasurementType.BLOOD_PRESSURE,
                value = 120.0,
                secondaryValue = 80.0,
                tertiaryValue = 72.0,
                hasArrhythmia = false,
                mealContext = null,
                arm = "LEFT",
                bodyPosition = "SITTING",
                timestamp = 2000L
            ),
            Measurement(
                id = 2L,
                type = MeasurementType.BLOOD_SUGAR,
                value = 95.0,
                mealContext = "BEFORE_MEAL",
                timestamp = 3000L
            )
        )

        val medications = listOf(
            Medication(
                id = 1L,
                name = "Amlodipine",
                dosage = "5",
                unit = "mg",
                frequency = "DAILY",
                instructions = "Morning with water",
                isActive = true,
                isDeleted = false,
                createdAt = 4000L
            )
        )

        val reminders = listOf(
            MedicationReminder(
                id = 1L,
                medicationId = 1L,
                hour = 8,
                minute = 30,
                daysOfWeekMask = 127,
                isEnabled = true,
                startDate = "2026-11-20",
                endDate = null
            )
        )

        val overrides = listOf(
            MedicationScheduleOverride(
                id = 1L,
                reminderId = 1L,
                medicationId = 1L,
                originalDate = "2026-11-25",
                overrideType = "RESCHEDULED",
                newHour = 10,
                newMinute = 0
            )
        )

        val doseLogs = listOf(
            MedicationDoseLog(
                id = 1L,
                medicationId = 1L,
                scheduledDate = "2026-11-25",
                scheduledTime = "08:30",
                actionTimestamp = 5100L,
                status = MedicationDoseLog.STATUS_TAKEN,
                notes = "Taken with breakfast"
            )
        )

        val symptoms = listOf(
            SymptomEntry(
                id = 1L,
                symptomType = "HEADACHE",
                customName = null,
                severity = 3,
                timestamp = 6000L,
                notes = "Mild afternoon tension"
            )
        )

        return BackupData(
            profile = profile,
            measurements = measurements,
            medications = medications,
            medicationReminders = reminders,
            scheduleOverrides = overrides,
            doseLogs = doseLogs,
            symptoms = symptoms
        )
    }

    @Test
    fun serializeAndDeserialize_preservesAllTablesAndFields() {
        val originalData = sampleBackupData()
        val metadata = BackupMetadata(
            version = 1,
            appVersion = "1.0",
            exportedAt = 123456789L,
            measurementsCount = originalData.measurements.size,
            medicationsCount = originalData.medications.size,
            symptomsCount = originalData.symptoms.size,
            isEncrypted = false
        )
        val payload = BackupPayload(metadata, originalData)

        val jsonString = BackupSerializer.toJson(payload)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("HEALTHLOG_BACKUP"))

        val restored = BackupSerializer.fromJson(jsonString)

        assertEquals(1, restored.metadata.version)
        assertEquals(123456789L, restored.metadata.exportedAt)
        assertFalse(restored.metadata.isEncrypted)

        // Profile
        val restoredProfile = restored.data.profile
        assertNotNull(restoredProfile)
        assertEquals("Nikos", restoredProfile?.name)
        assertEquals("1990-05-15", restoredProfile?.dateOfBirth)
        assertEquals("MALE", restoredProfile?.gender)

        // Measurements
        assertEquals(2, restored.data.measurements.size)
        val bp = restored.data.measurements[0]
        assertEquals(MeasurementType.BLOOD_PRESSURE, bp.type)
        assertEquals(120.0, bp.value, 0.0)
        assertEquals(80.0, bp.secondaryValue!!, 0.0)
        assertEquals(72.0, bp.tertiaryValue!!, 0.0)
        assertEquals("LEFT", bp.arm)
        assertEquals("SITTING", bp.bodyPosition)

        val sugar = restored.data.measurements[1]
        assertEquals(MeasurementType.BLOOD_SUGAR, sugar.type)
        assertEquals(95.0, sugar.value, 0.0)
        assertEquals("BEFORE_MEAL", sugar.mealContext)

        // Medications & Reminders
        assertEquals(1, restored.data.medications.size)
        assertEquals("Amlodipine", restored.data.medications[0].name)
        assertEquals(1, restored.data.medicationReminders.size)
        assertEquals(8, restored.data.medicationReminders[0].hour)
        assertEquals(30, restored.data.medicationReminders[0].minute)

        // Overrides & Dose Logs
        assertEquals(1, restored.data.scheduleOverrides.size)
        assertEquals("RESCHEDULED", restored.data.scheduleOverrides[0].overrideType)
        assertEquals(1, restored.data.doseLogs.size)
        assertEquals(MedicationDoseLog.STATUS_TAKEN, restored.data.doseLogs[0].status)

        // Symptoms
        assertEquals(1, restored.data.symptoms.size)
        assertEquals("HEADACHE", restored.data.symptoms[0].symptomType)
        assertEquals(3, restored.data.symptoms[0].severity)
        assertEquals("Mild afternoon tension", restored.data.symptoms[0].notes)
    }

    @Test
    fun encryptAndDecrypt_withCorrectPassword_restoresExactPlaintext() {
        val originalData = sampleBackupData()
        val metadata = BackupMetadata(
            version = 1,
            exportedAt = 999999L,
            measurementsCount = 2,
            medicationsCount = 1,
            symptomsCount = 1
        )
        val plainJson = BackupSerializer.toJson(BackupPayload(metadata, originalData))
        val password = "MySecretHealthPassword123!".toCharArray()

        val encryptedJson = BackupCrypto.encrypt(plainJson, password, metadata)
        assertTrue(BackupCrypto.isEncryptedBackup(encryptedJson))
        assertFalse(encryptedJson.contains("Amlodipine"))
        assertFalse(encryptedJson.contains("Nikos"))

        val peeked = BackupCrypto.peekMetadata(encryptedJson)
        assertNotNull(peeked)
        assertTrue(peeked!!.isEncrypted)
        assertEquals(2, peeked.measurementsCount)

        val decryptedJson = BackupCrypto.decrypt(encryptedJson, password)
        assertEquals(plainJson, decryptedJson)

        val payload = BackupSerializer.fromJson(decryptedJson)
        assertEquals("Nikos", payload.data.profile?.name)
    }

    @Test
    fun decrypt_withWrongPassword_throwsAEADBadTagException() {
        val originalData = sampleBackupData()
        val metadata = BackupMetadata(version = 1)
        val plainJson = BackupSerializer.toJson(BackupPayload(metadata, originalData))
        val password = "CorrectPassword123".toCharArray()
        val wrongPassword = "WrongPassword456".toCharArray()

        val encryptedJson = BackupCrypto.encrypt(plainJson, password, metadata)

        try {
            BackupCrypto.decrypt(encryptedJson, wrongPassword)
            fail("Expected exception when decrypting with wrong password")
        } catch (e: Exception) {
            assertTrue(
                e is AEADBadTagException ||
                        e.cause is AEADBadTagException ||
                        e.message?.contains("Tag mismatch", ignoreCase = true) == true
            )
        }
    }

    @Test
    fun peekMetadata_worksOnPlainAndEncryptedJson() {
        val metadata = BackupMetadata(
            version = 1,
            appVersion = "1.0",
            exportedAt = 77777L,
            measurementsCount = 5,
            medicationsCount = 2,
            symptomsCount = 3
        )
        val plainPayload = BackupPayload(metadata, sampleBackupData())
        val plainJson = BackupSerializer.toJson(plainPayload)

        val peekPlain = BackupCrypto.peekMetadata(plainJson)
        assertNotNull(peekPlain)
        assertFalse(peekPlain!!.isEncrypted)
        assertEquals(2, peekPlain.measurementsCount) // actual count from sampleBackupData
        assertEquals(1, peekPlain.medicationsCount)

        val encryptedJson = BackupCrypto.encrypt(plainJson, "secret".toCharArray(), metadata)
        val peekEncrypted = BackupCrypto.peekMetadata(encryptedJson)
        assertNotNull(peekEncrypted)
        assertTrue(peekEncrypted!!.isEncrypted)
    }
}
