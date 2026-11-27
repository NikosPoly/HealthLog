package com.example.healthlog.backup

import com.example.healthlog.database.Measurement
import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationDoseLog
import com.example.healthlog.database.MedicationReminder
import com.example.healthlog.database.MedicationScheduleOverride
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.database.UserProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data container representing a complete snapshot of HealthLog user data.
 */
data class BackupData(
    val profile: UserProfile? = null,
    val measurements: List<Measurement> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val medicationReminders: List<MedicationReminder> = emptyList(),
    val scheduleOverrides: List<MedicationScheduleOverride> = emptyList(),
    val doseLogs: List<MedicationDoseLog> = emptyList(),
    val symptoms: List<SymptomEntry> = emptyList()
) {
    val totalItemsCount: Int
        get() = (if (profile?.hasAnyDetails == true) 1 else 0) +
                measurements.size +
                medications.size +
                medicationReminders.size +
                scheduleOverrides.size +
                doseLogs.size +
                symptoms.size
}

/**
 * Metadata header for backups.
 */
data class BackupMetadata(
    val version: Int = BackupSerializer.CURRENT_VERSION,
    val appVersion: String = "1.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val measurementsCount: Int = 0,
    val medicationsCount: Int = 0,
    val symptomsCount: Int = 0,
    val isEncrypted: Boolean = false
)

/**
 * Container holding metadata alongside the data payload.
 */
data class BackupPayload(
    val metadata: BackupMetadata,
    val data: BackupData
)

/**
 * Pure JSON serializer and parser for HealthLog backups.
 * Designed to be forward- and backward-compatible (missing fields default safely; unknown keys are ignored).
 */
object BackupSerializer {
    const val CURRENT_VERSION: Int = 1
    const val MAGIC_HEADER: String = "HEALTHLOG_BACKUP"
    const val ENCRYPTED_MAGIC_HEADER: String = "HEALTHLOG_ENCRYPTED_BACKUP"

    fun toJson(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("format", MAGIC_HEADER)
        root.put("version", payload.metadata.version)
        root.put("appVersion", payload.metadata.appVersion)
        root.put("exportedAt", payload.metadata.exportedAt)

        val metaObj = JSONObject().apply {
            put("measurementsCount", payload.data.measurements.size)
            put("medicationsCount", payload.data.medications.size)
            put("symptomsCount", payload.data.symptoms.size)
            put("isEncrypted", payload.metadata.isEncrypted)
        }
        root.put("metadata", metaObj)

        val dataObj = JSONObject()

        // Profile
        payload.data.profile?.let { profile ->
            dataObj.put("profile", JSONObject().apply {
                put("id", profile.id)
                profile.name?.let { put("name", it) }
                profile.dateOfBirth?.let { put("dateOfBirth", it) }
                profile.gender?.let { put("gender", it) }
                put("updatedAt", profile.updatedAt)
            })
        }

        // Measurements
        val measurementsArray = JSONArray()
        payload.data.measurements.forEach { m ->
            measurementsArray.put(JSONObject().apply {
                put("id", m.id)
                put("type", m.type.name)
                put("value", m.value)
                m.secondaryValue?.let { put("secondaryValue", it) }
                m.tertiaryValue?.let { put("tertiaryValue", it) }
                m.hasArrhythmia?.let { put("hasArrhythmia", it) }
                m.mealContext?.let { put("mealContext", it) }
                m.arm?.let { put("arm", it) }
                m.bodyPosition?.let { put("bodyPosition", it) }
                put("timestamp", m.timestamp)
            })
        }
        dataObj.put("measurements", measurementsArray)

        // Medications
        val medicationsArray = JSONArray()
        payload.data.medications.forEach { med ->
            medicationsArray.put(JSONObject().apply {
                put("id", med.id)
                put("name", med.name)
                put("dosage", med.dosage)
                put("unit", med.unit)
                put("frequency", med.frequency)
                med.instructions?.let { put("instructions", it) }
                put("isActive", med.isActive)
                put("isDeleted", med.isDeleted)
                put("createdAt", med.createdAt)
            })
        }
        dataObj.put("medications", medicationsArray)

        // Medication reminders
        val remindersArray = JSONArray()
        payload.data.medicationReminders.forEach { r ->
            remindersArray.put(JSONObject().apply {
                put("id", r.id)
                put("medicationId", r.medicationId)
                put("hour", r.hour)
                put("minute", r.minute)
                put("daysOfWeekMask", r.daysOfWeekMask)
                put("isEnabled", r.isEnabled)
                r.startDate?.let { put("startDate", it) }
                r.endDate?.let { put("endDate", it) }
            })
        }
        dataObj.put("medicationReminders", remindersArray)

        // Schedule overrides
        val overridesArray = JSONArray()
        payload.data.scheduleOverrides.forEach { o ->
            overridesArray.put(JSONObject().apply {
                put("id", o.id)
                put("reminderId", o.reminderId)
                put("medicationId", o.medicationId)
                put("originalDate", o.originalDate)
                put("overrideType", o.overrideType)
                o.newHour?.let { put("newHour", it) }
                o.newMinute?.let { put("newMinute", it) }
            })
        }
        dataObj.put("scheduleOverrides", overridesArray)

        // Dose logs
        val doseLogsArray = JSONArray()
        payload.data.doseLogs.forEach { d ->
            doseLogsArray.put(JSONObject().apply {
                put("id", d.id)
                put("medicationId", d.medicationId)
                put("scheduledDate", d.scheduledDate)
                put("scheduledTime", d.scheduledTime)
                put("actionTimestamp", d.actionTimestamp)
                put("status", d.status)
                d.notes?.let { put("notes", it) }
            })
        }
        dataObj.put("doseLogs", doseLogsArray)

        // Symptoms
        val symptomsArray = JSONArray()
        payload.data.symptoms.forEach { s ->
            symptomsArray.put(JSONObject().apply {
                put("id", s.id)
                put("symptomType", s.symptomType)
                s.customName?.let { put("customName", it) }
                put("severity", s.severity)
                put("timestamp", s.timestamp)
                s.notes?.let { put("notes", it) }
            })
        }
        dataObj.put("symptoms", symptomsArray)

        root.put("data", dataObj)
        return root.toString()
    }

    fun fromJson(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val format = root.optString("format")
        if (format != MAGIC_HEADER) {
            throw IllegalArgumentException("Invalid backup format or encrypted backup: $format")
        }

        val version = root.optInt("version", CURRENT_VERSION)
        val appVersion = root.optString("appVersion", "1.0")
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

        val metaObj = root.optJSONObject("metadata")
        val metadata = BackupMetadata(
            version = version,
            appVersion = appVersion,
            exportedAt = exportedAt,
            measurementsCount = metaObj?.optInt("measurementsCount") ?: 0,
            medicationsCount = metaObj?.optInt("medicationsCount") ?: 0,
            symptomsCount = metaObj?.optInt("symptomsCount") ?: 0,
            isEncrypted = metaObj?.optBoolean("isEncrypted") ?: false
        )

        val dataObj = root.optJSONObject("data") ?: JSONObject()

        // Profile
        val profile = dataObj.optJSONObject("profile")?.let { p ->
            UserProfile(
                id = p.optLong("id", UserProfile.SINGLETON_ID),
                name = p.optStringOrNull("name"),
                dateOfBirth = p.optStringOrNull("dateOfBirth"),
                gender = p.optStringOrNull("gender"),
                updatedAt = p.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        // Measurements
        val measurements = mutableListOf<Measurement>()
        dataObj.optJSONArray("measurements")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val typeName = item.optString("type")
                val type = try {
                    com.example.healthlog.model.MeasurementType.valueOf(typeName)
                } catch (e: Exception) {
                    continue // Skip unrecognized future types gracefully
                }
                measurements.add(
                    Measurement(
                        id = item.optLong("id", 0L),
                        type = type,
                        value = item.optDouble("value"),
                        secondaryValue = item.optDoubleOrNull("secondaryValue"),
                        tertiaryValue = item.optDoubleOrNull("tertiaryValue"),
                        hasArrhythmia = item.optBooleanOrNull("hasArrhythmia"),
                        mealContext = item.optStringOrNull("mealContext"),
                        arm = item.optStringOrNull("arm"),
                        bodyPosition = item.optStringOrNull("bodyPosition"),
                        timestamp = item.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        // Medications
        val medications = mutableListOf<Medication>()
        dataObj.optJSONArray("medications")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                medications.add(
                    Medication(
                        id = item.optLong("id", 0L),
                        name = item.optString("name"),
                        dosage = item.optString("dosage"),
                        unit = item.optString("unit"),
                        frequency = item.optString("frequency"),
                        instructions = item.optStringOrNull("instructions"),
                        isActive = item.optBoolean("isActive", true),
                        isDeleted = item.optBoolean("isDeleted", false),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Medication Reminders
        val reminders = mutableListOf<MedicationReminder>()
        dataObj.optJSONArray("medicationReminders")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                reminders.add(
                    MedicationReminder(
                        id = item.optLong("id", 0L),
                        medicationId = item.optLong("medicationId"),
                        hour = item.optInt("hour"),
                        minute = item.optInt("minute"),
                        daysOfWeekMask = item.optInt("daysOfWeekMask", 127),
                        isEnabled = item.optBoolean("isEnabled", true),
                        startDate = item.optStringOrNull("startDate"),
                        endDate = item.optStringOrNull("endDate")
                    )
                )
            }
        }

        // Schedule Overrides
        val overrides = mutableListOf<MedicationScheduleOverride>()
        dataObj.optJSONArray("scheduleOverrides")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                overrides.add(
                    MedicationScheduleOverride(
                        id = item.optLong("id", 0L),
                        reminderId = item.optLong("reminderId"),
                        medicationId = item.optLong("medicationId"),
                        originalDate = item.optString("originalDate"),
                        overrideType = item.optString("overrideType"),
                        newHour = item.optIntOrNull("newHour"),
                        newMinute = item.optIntOrNull("newMinute")
                    )
                )
            }
        }

        // Dose Logs
        val doseLogs = mutableListOf<MedicationDoseLog>()
        dataObj.optJSONArray("doseLogs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                doseLogs.add(
                    MedicationDoseLog(
                        id = item.optLong("id", 0L),
                        medicationId = item.optLong("medicationId"),
                        scheduledDate = item.optString("scheduledDate"),
                        scheduledTime = item.optString("scheduledTime"),
                        actionTimestamp = item.optLong("actionTimestamp", System.currentTimeMillis()),
                        status = item.optString("status"),
                        notes = item.optStringOrNull("notes")
                    )
                )
            }
        }

        // Symptoms
        val symptoms = mutableListOf<SymptomEntry>()
        dataObj.optJSONArray("symptoms")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                symptoms.add(
                    SymptomEntry(
                        id = item.optLong("id", 0L),
                        symptomType = item.optString("symptomType"),
                        customName = item.optStringOrNull("customName"),
                        severity = item.optInt("severity", 1),
                        timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                        notes = item.optStringOrNull("notes")
                    )
                )
            }
        }

        return BackupPayload(
            metadata = metadata,
            data = BackupData(
                profile = profile,
                measurements = measurements,
                medications = medications,
                medicationReminders = reminders,
                scheduleOverrides = overrides,
                doseLogs = doseLogs,
                symptoms = symptoms
            )
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val str = optString(key)
        return if (str.isEmpty()) null else str
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key)
        return if (v.isNaN()) null else v
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }

    private fun JSONObject.optBooleanOrNull(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return optBoolean(key)
    }
}
