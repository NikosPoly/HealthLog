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
            testDatabase, 3, true, HealthDatabase.MIGRATION_1_2, HealthDatabase.MIGRATION_2_3
        ).close()

        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            HealthDatabase::class.java,
            testDatabase
        )
            .addMigrations(HealthDatabase.MIGRATION_1_2, HealthDatabase.MIGRATION_2_3)
            .build()

        try {
            // Newest first, as the history shows them.
            val measurements = runBlocking { database.measurementDao().getAllMeasurements().first() }

            assertEquals(2, measurements.size)
            assertEquals(MeasurementType.WEIGHT, measurements[0].type)
            assertEquals(80.25, measurements[0].value, 0.0)
            assertEquals(MeasurementType.BLOOD_PRESSURE, measurements[1].type)
            assertEquals(85.0, measurements[1].secondaryValue!!, 0.0)
            assertEquals(null, measurements[1].tertiaryValue)
        } finally {
            database.close()
        }
    }
}
