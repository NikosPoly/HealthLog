
package com.example.healthlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.healthlog.database.HealthDatabase
import com.example.healthlog.repository.MeasurementRepository
import com.example.healthlog.ui.HealthLogNavHost
import com.example.healthlog.ui.theme.HealthLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = HealthDatabase
            .getDatabase(applicationContext)
            .measurementDao()

        val repository = MeasurementRepository(dao)

        setContent {
            HealthLogTheme{
                HealthLogNavHost(repository)
            }
        }
    }
}
