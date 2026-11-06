package com.example.healthlog.ui.history.chart.stats

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// One stat: its icon, its name and its value, e.g. "Average" and "72.46 kg".
internal class StatItem(
    @param:DrawableRes val icon: Int,
    val label: String,
    val value: String,
    val emphasis: Emphasis = Emphasis.NONE
)

// Two stats side by side, each in its own rounded box: the icon and the name small on top,
// the value larger below, all centered.
// Values are medium weight; a value with an emphasis is regular, with that number bold,
// so it stands out.
@Composable
internal fun StatPair(start: StatItem, end: StatItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatBox(start, Modifier.weight(1f))
        StatBox(end, Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(item: StatItem, modifier: Modifier) {
    Column(
        modifier = modifier
            // A stronger tint than the period card around it.
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(item.icon),
                // The name next to it says what the stat is.
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                // Multi-colored icon: keep its own colors.
                tint = Color.Unspecified
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = emphasizedValue(item.value, item.emphasis),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (item.emphasis == Emphasis.NONE) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
