package com.example.healthlog.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthlog.model.MeasurementType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Checks that databases from older app versions are upgraded correctly and keep their data.
// Each test creates a database of an old version (from app/schemas), fills it with rows
// as the old app stored them, runs the migrations and checks the result.
// Runs on an emulator or device: Android Studio → right-click this file → Run.
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDatabase = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HealthDatabase::class.java
    )

    // Version 1 → 2: the pulse column is added; existing rows keep their values
    // and have no pulse.
    @Test
    fun migrate1To2_keepsDataAndAddsEmptyPulse() {
        helper.createDatabase(testDatabase, 1).apply {
            execSQL(
                "INSERT INTO measurements (id, type, value, secondaryValue, timestamp) " +
                        "VALUES (1, 'Αρτηριακή πίεση', 120, 80, 1000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDatabase, 2, true, HealthDatabase.MIGRATION_1_2
        )

        db.query("SELECT type, value, secondaryValue, tertiaryValue, timestamp FROM measurements").use { row ->
            assertEquals(1, row.count)
            row.moveToFirst()
            assertEquals("Αρτηριακή πίεση", row.getString(0))
            assertEquals(120.0, row.getDouble(1), 0.0)
            assertEquals(80.0, row.getDouble(2), 0.0)
            assertTrue(row.isNull(3))
            assertEquals(1000L, row.getLong(4))
        }
    }

    // Version 2 → 3: the Greek type texts are replaced by the MeasurementType names;
    // all other values stay the same.
    @Test
    fun migrate2To3_convertsGreekTypeNames() {
        helper.createDatabase(testDatabase, 2).apply {
            execSQL(
                "INSERT INTO measurements (id, type, value, secondaryValue, tertiaryValue, timestamp) " +
                        "VALUES (1, 'Αρτηριακή πίεση', 120, 80, 72, 1000)"
            )
            execSQL("INSERT INTO measurements (id, type, value, timestamp) VALUES (2, 'Παλμοί', 70, 2000)")
            execSQL("INSERT INTO measurements (id, type, value, timestamp) VALUES (3, 'Σάκχαρο', 95, 3000)")
            execSQL("INSERT INTO measurements (id, type, value, timestamp) VALUES (4, 'Βάρος', 72.5, 4000)")
            execSQL("INSERT INTO measurements (id, type, value, timestamp) VALUES (5, 'Ύψος', 180, 5000)")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDatabase, 3, true, HealthDatabase.MIGRATION_2_3
        )

        db.query("SELECT type FROM measurements ORDER BY id").use { row ->
            val types = mutableListOf<String>()
            while (row.moveToNext()) types.add(row.getString(0))
            assertEquals(
                listOf("BLOOD_PRESSURE", "PULSE", "BLOOD_SUGAR", "WEIGHT", "HEIGHT"),
                types
            )
        }

        db.query("SELECT value, secondaryValue, tertiaryValue FROM measurements WHERE id = 1").use { row ->
            row.moveToFirst()
            assertEquals(120.0, row.getDouble(0), 0.0)
            assertEquals(80.0, row.getDouble(1), 0.0)
            assertEquals(72.0, row.getDouble(2), 0.0)
        }
    }

    // Version 1 → 3 in one go, as for a user who skipped app updates.
    // Then the app's own database code reads the rows: this checks that every
    // stored type name matches a MeasurementType constant.
    @Test
    fun migrate1To3_appCanReadTheData() {
        helper.createDatabase(testDatabase, 1).apply {
            execSQL(
                "INSERT INTO measurements (id, type, value, secondaryValue, timestamp) " +
                        "VALUES (1, 'Αρτηριακή πίεση', 130, 85, 1000)"
            )
            execSQL("INSERT INTO measurements (id, type, value, timestamp) VALUES (2, 'Βάρος', 80.25, 2000)")
            close()
        }

        helper.runMigrationsAndValidate(
            testDatabase, 5, true, HealthDatabase.MIGRATION_1_2, HealthDatabase.MIGRATION_2_3, HealthDatabase.MIGRATION_3_4, HealthDatabase.MIGRATION_4_5
        ).close()

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            HealthDatabase::class.java,
            testDatabase
        )
            .addMigrations(HealthDatabase.MIGRATION_1_2, HealthDatabase.MIGRATION_2_3, HealthDatabase.MIGRATION_3_4, HealthDatabase.MIGRATION_4_5)
            .build()

        try {
            // Read as the history does: one type at a time.
            val dao = database.measurementDao()
            val weights = runBlocking { dao.getMeasurementsByType(MeasurementType.WEIGHT).first() }
            val bloodPressures = runBlocking {
                dao.getMeasurementsByType(MeasurementType.BLOOD_PRESSURE).first()
            }

            assertEquals(1, weights.size)
            assertEquals(80.25, weights[0].value, 0.0)

            assertEquals(1, bloodPressures.size)
            assertEquals(85.0, bloodPressures[0].secondaryValue!!, 0.0)
            assertEquals(null, bloodPressures[0].tertiaryValue)
            assertEquals(null, bloodPressures[0].hasArrhythmia)
            assertEquals(null, bloodPressures[0].arm)
            assertEquals(null, bloodPressures[0].bodyPosition)
            assertEquals(null, bloodPressures[0].mealContext)
        } finally {
            database.close()
        }
    }

    // Version 3 → 4: the clinical context columns (hasArrhythmia, mealContext, arm, bodyPosition)
    // are added as nullable columns; existing rows are preserved with null for the new columns.
    @Test
    fun migrate3To4_keepsDataAndAddsEmptyContextColumns() {
        helper.createDatabase(testDatabase, 3).apply {
            execSQL(
                "INSERT INTO measurements (id, type, value, secondaryValue, tertiaryValue, timestamp) " +
                        "VALUES (1, 'BLOOD_PRESSURE', 120, 80, 72, 1000)"
            )
            execSQL(
                "INSERT INTO measurements (id, type, value, timestamp) " +
                        "VALUES (2, 'BLOOD_SUGAR', 95, 2000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDatabase, 4, true, HealthDatabase.MIGRATION_3_4
        )

        db.query(
            "SELECT id, type, value, secondaryValue, tertiaryValue, timestamp, " +
                    "hasArrhythmia, mealContext, arm, bodyPosition FROM measurements ORDER BY id"
        ).use { cursor ->
            assertEquals(2, cursor.count)

            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertEquals("BLOOD_PRESSURE", cursor.getString(1))
            assertEquals(120.0, cursor.getDouble(2), 0.0)
            assertEquals(80.0, cursor.getDouble(3), 0.0)
            assertEquals(72.0, cursor.getDouble(4), 0.0)
            assertEquals(1000L, cursor.getLong(5))
            assertTrue(cursor.isNull(6))
            assertTrue(cursor.isNull(7))
            assertTrue(cursor.isNull(8))
            assertTrue(cursor.isNull(9))

            cursor.moveToNext()
            assertEquals(2L, cursor.getLong(0))
            assertEquals("BLOOD_SUGAR", cursor.getString(1))
            assertEquals(95.0, cursor.getDouble(2), 0.0)
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))
            assertEquals(2000L, cursor.getLong(5))
            assertTrue(cursor.isNull(6))
            assertTrue(cursor.isNull(7))
            assertTrue(cursor.isNull(8))
            assertTrue(cursor.isNull(9))
        }
    }

    // Version 4 → 5: creates medications, reminders, and dose log tables.
    // Existing measurements are completely preserved.
    @Test
    fun migrate4To5_createsMedicationTablesAndKeepsMeasurements() {
        helper.createDatabase(testDatabase, 4).apply {
            execSQL(
                "INSERT INTO measurements (id, type, value, secondaryValue, tertiaryValue, timestamp, " +
                        "hasArrhythmia, mealContext, arm, bodyPosition) " +
                        "VALUES (1, 'BLOOD_PRESSURE', 125, 82, 68, 1000, 0, null, 'LEFT', 'SITTING')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDatabase, 5, true, HealthDatabase.MIGRATION_4_5
        )

        // Verify existing measurement is intact
        db.query("SELECT id, type, value, secondaryValue, tertiaryValue, arm, bodyPosition FROM measurements").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertEquals("BLOOD_PRESSURE", cursor.getString(1))
            assertEquals(125.0, cursor.getDouble(2), 0.0)
            assertEquals(82.0, cursor.getDouble(3), 0.0)
            assertEquals(68.0, cursor.getDouble(4), 0.0)
            assertEquals("LEFT", cursor.getString(5))
            assertEquals("SITTING", cursor.getString(6))
        }

        // Verify new tables exist and can accept records
        db.execSQL(
            "INSERT INTO medications (id, name, dosage, unit, frequency, instructions, isActive, createdAt) " +
                    "VALUES (1, 'Amlodipine', '5', 'mg', 'DAILY', 'After breakfast', 1, 2000)"
        )
        db.execSQL(
            "INSERT INTO medication_reminders (id, medicationId, hour, minute, daysOfWeekMask, isEnabled) " +
                    "VALUES (1, 1, 8, 30, 127, 1)"
        )
        db.execSQL(
            "INSERT INTO medication_dose_logs (id, medicationId, scheduledDate, scheduledTime, actionTimestamp, status, notes) " +
                    "VALUES (1, 1, '2026-11-18', '08:30', 2100, 'TAKEN', null)"
        )
        db.execSQL(
            "INSERT INTO symptoms (id, symptomType, customName, severity, timestamp, notes) " +
                    "VALUES (1, 'HEADACHE', null, 3, 2200, 'Mild afternoon tension')"
        )

        db.query("SELECT name, dosage, unit, isActive FROM medications WHERE id = 1").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Amlodipine", cursor.getString(0))
            assertEquals("5", cursor.getString(1))
            assertEquals("mg", cursor.getString(2))
            assertEquals(1, cursor.getInt(3))
        }

        db.query("SELECT medicationId, hour, minute FROM medication_reminders WHERE id = 1").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertEquals(8, cursor.getInt(1))
            assertEquals(30, cursor.getInt(2))
        }

        db.query("SELECT medicationId, scheduledDate, status FROM medication_dose_logs WHERE id = 1").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertEquals("2026-11-18", cursor.getString(1))
            assertEquals("TAKEN", cursor.getString(2))
        }

        db.query("SELECT symptomType, customName, severity, timestamp, notes FROM symptoms WHERE id = 1").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("HEADACHE", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertEquals(3, cursor.getInt(2))
            assertEquals(2200L, cursor.getLong(3))
            assertEquals("Mild afternoon tension", cursor.getString(4))
        }
    }
}
