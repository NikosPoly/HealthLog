package com.example.healthlog.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.ui.theme.HealthLogTheme
import com.example.healthlog.ui.theme.TypeTheme
import com.example.healthlog.ui.theme.icon

// One type on the home screen: its icon, its name and its unit. Tapping it opens the
// entry dialog of that type.
@Composable
fun TypePickerCard(
    @DrawableRes icon: Int,
    title: String,
    unit: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        // The type's color, given by the TypeTheme around the card (see HomeContent).
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                // The name next to it says what the card is.
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                // Multicolored icon: keep its own colors.
                tint = Color.Unspecified
            )

            Column {
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
}

@Preview(showBackground = true)
@Composable
private fun TypePickerCardPreview() {
    HealthLogTheme {
        TypeTheme(MeasurementType.BLOOD_PRESSURE) {
            TypePickerCard(
                MeasurementType.BLOOD_PRESSURE.icon,
                stringResource(MeasurementType.BLOOD_PRESSURE.label),
                MeasurementType.BLOOD_PRESSURE.fields.first().unit
            ) {}
        }
    }
}
