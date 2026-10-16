
package com.example.healthlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthlog.database.HealthDatabase
import com.example.healthlog.ui.HealthLogScreen
import com.example.healthlog.ui.theme.HealthLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dao = HealthDatabase
            .getDatabase(applicationContext)
            .measurementDao()

        setContent {
            HealthLogTheme{
                HealthLogScreen(dao)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HealthLogPreview() {
    HealthLogTheme {
        Text("HealthDiary")
    }
}
