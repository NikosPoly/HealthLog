package com.example.healthlog.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.theme.HealthLogTheme

@Composable
fun MeasurementPickerCard(
    title: String,
    unit: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                title,

                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                unit,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MeasurementPickerCardPreview() {
    HealthLogTheme {
        MeasurementPickerCard(
            stringResource(MeasurementType.BLOOD_PRESSURE.label),
            MeasurementType.BLOOD_PRESSURE.fields.first().unit
        ) {}
    }
}
