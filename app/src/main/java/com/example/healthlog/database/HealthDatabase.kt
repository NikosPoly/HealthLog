package com.example.healthlog.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Measurement::class],
    version = 4,
    exportSchema = true
)
abstract class HealthDatabase : RoomDatabase() {

    abstract fun measurementDao(): MeasurementDao

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

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
