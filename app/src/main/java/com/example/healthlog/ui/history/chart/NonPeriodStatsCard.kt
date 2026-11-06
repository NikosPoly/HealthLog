package com.example.healthlog.ui.history.chart

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.ui.format.decimalSeparator
import com.example.healthlog.ui.format.formatTimestamp
import com.example.healthlog.ui.format.rememberDateTimeFormatter
import com.example.healthlog.ui.format.timeAgo
import com.example.healthlog.ui.format.timeAgoText
import com.example.healthlog.ui.history.chart.stats.statValueText
import java.time.ZonedDateTime

// The stats that don't depend on the period, below the period card. They're shown
// whatever the period, even an empty one. For now: the latest entry of the type.
// Same look as the period card: a rounded tinted card with a gray box inside, with the
// icon and "Last Entry" on top, the value large, and when it was recorded below,
// e.g. "yesterday · Nov 4, 2026, 8:07 PM".
@Composable
fun NonPeriodStatsCard(
    // The most recent measurement of the type.
    latest: Measurement,
    modifier: Modifier = Modifier
) {
    val value = statValueText(latest, decimalSeparator())
    val dateTimeFormatter = rememberDateTimeFormatter()
    val recorded = timeAgoText(timeAgo(latest.timestamp, ZonedDateTime.now())) +
        " · " + formatTimestamp(latest.timestamp, dateTimeFormatter)

    Card(
        modifier = modifier.fillMaxWidth(),
        // Same as the period card.
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                // Same as the stat boxes in the period card.
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
                    painter = painterResource(R.drawable.ic_history),
                    // The name next to it says what this is.
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    // Multicolored icon: keep its own colors.
                    tint = Color.Unspecified
                )
                Text(
                    text = stringResource(R.string.stats_latest_measurement),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = recorded,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
