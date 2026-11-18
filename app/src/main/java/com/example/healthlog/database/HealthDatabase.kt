package com.example.healthlog.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Measurement::class,
        Medication::class,
        MedicationReminder::class,
        MedicationDoseLog::class,
    ],
    version = 5,
    exportSchema = true
)
abstract class HealthDatabase : RoomDatabase() {

    abstract fun measurementDao(): MeasurementDao
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: HealthDatabase? = null

        // Version 1 → 2: add the pulse column for blood pressure measurements.
        // Existing rows keep their data; the new column is empty (null) for them.
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE measurements ADD COLUMN tertiaryValue REAL")
            }
        }

        // Version 2 → 3: the type is now stored as the MeasurementType name
        // instead of the Greek text, so the existing rows are converted.
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE measurements SET type = 'BLOOD_PRESSURE' WHERE type = 'Αρτηριακή πίεση'")
                db.execSQL("UPDATE measurements SET type = 'PULSE' WHERE type = 'Παλμοί'")
                db.execSQL("UPDATE measurements SET type = 'BLOOD_SUGAR' WHERE type = 'Σάκχαρο'")
                db.execSQL("UPDATE measurements SET type = 'WEIGHT' WHERE type = 'Βάρος'")
                db.execSQL("UPDATE measurements SET type = 'HEIGHT' WHERE type = 'Ύψος'")
            }
        }

        // Version 3 → 4: add clinical context columns:
        // - hasArrhythmia: 0/1 integer (boolean) for arrhythmia detection
        // - mealContext: text name of MealContext enum
        // - arm: text name of MeasurementArm enum
        // - bodyPosition: text name of BodyPosition enum
        // Older records have null for all new columns.
        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE measurements ADD COLUMN hasArrhythmia INTEGER")
                db.execSQL("ALTER TABLE measurements ADD COLUMN mealContext TEXT")
                db.execSQL("ALTER TABLE measurements ADD COLUMN arm TEXT")
                db.execSQL("ALTER TABLE measurements ADD COLUMN bodyPosition TEXT")
            }
        }

        // Version 4 → 5: add tables for medications, reminders, and dose tracking:
        // - medications
        // - medication_reminders
        // - medication_dose_logs
        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `medications` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `dosage` TEXT NOT NULL,
                        `unit` TEXT NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `instructions` TEXT,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `medication_reminders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `medicationId` INTEGER NOT NULL,
                        `hour` INTEGER NOT NULL,
                        `minute` INTEGER NOT NULL,
                        `daysOfWeekMask` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        FOREIGN KEY(`medicationId`) REFERENCES `medications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_medication_reminders_medicationId` ON `medication_reminders` (`medicationId`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `medication_dose_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `medicationId` INTEGER NOT NULL,
                        `scheduledDate` TEXT NOT NULL,
                        `scheduledTime` TEXT NOT NULL,
                        `actionTimestamp` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `notes` TEXT,
                        FOREIGN KEY(`medicationId`) REFERENCES `medications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_medication_dose_logs_medicationId` ON `medication_dose_logs` (`medicationId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_medication_dose_logs_scheduledDate` ON `medication_dose_logs` (`scheduledDate`)"
                )
            }
        }

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
