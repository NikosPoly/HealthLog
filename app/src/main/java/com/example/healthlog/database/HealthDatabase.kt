package com.example.healthlog.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Measurement::class],
    version = 3,
    exportSchema = true
)
abstract class HealthDatabase : RoomDatabase() {

    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile
        private var INSTANCE: HealthDatabase? = null

        // Version 1 → 2: add the pulse column for blood pressure entries.
        // Existing rows keep their data; the new column is empty (null) for them.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE measurements ADD COLUMN tertiaryValue REAL")
            }
        }

        // Version 2 → 3: the type is now stored as the MeasurementType name
        // instead of the Greek text, so the existing rows are converted.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE measurements SET type = 'BLOOD_PRESSURE' WHERE type = 'Αρτηριακή πίεση'")
                db.execSQL("UPDATE measurements SET type = 'PULSE' WHERE type = 'Παλμοί'")
                db.execSQL("UPDATE measurements SET type = 'BLOOD_SUGAR' WHERE type = 'Σάκχαρο'")
                db.execSQL("UPDATE measurements SET type = 'WEIGHT' WHERE type = 'Βάρος'")
                db.execSQL("UPDATE measurements SET type = 'HEIGHT' WHERE type = 'Ύψος'")
            }
        }

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
