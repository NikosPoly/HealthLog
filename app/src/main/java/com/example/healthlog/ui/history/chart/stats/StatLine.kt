package com.example.healthlog.ui.history.chart.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

// One stat: the name on the left, the value on the right (on several lines if needed,
// aligned to the right). Used by the non-period stats.
// Values are medium weight; a value with an emphasis is regular, with that number bold,
// so it stands out.
@Composable
internal fun StatLine(label: String, value: String, emphasis: Emphasis) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = emphasizedValue(value, emphasis),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasis == Emphasis.NONE) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.End
        )
    }
}
