package com.example.healthlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.healthlog.database.HealthDatabase
import com.example.healthlog.repository.MeasurementRepository
import com.example.healthlog.repository.MedicationRepository
import com.example.healthlog.ui.HealthLogNavHost
import com.example.healthlog.ui.theme.HealthLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = HealthDatabase.getDatabase(applicationContext)
        val measurementRepository = MeasurementRepository(database.measurementDao())
        val medicationRepository = MedicationRepository(database.medicationDao())

        setContent {
            HealthLogTheme {
                HealthLogNavHost(
                    repository = measurementRepository,
                    medicationRepository = medicationRepository
                )
            }
        }
    }
}
