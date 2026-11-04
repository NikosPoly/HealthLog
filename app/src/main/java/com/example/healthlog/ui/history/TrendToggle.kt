package com.example.healthlog.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R

// The button at the top center that switches between the list and the chart.
// It shows what it switches to: "See trend" on the list, "See history" on the chart.
@Composable
internal fun TrendToggle(
    showTrend: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        OutlinedButton(onClick = onToggle) {
            Icon(
                painter = painterResource(
                    if (showTrend) R.drawable.ic_history else R.drawable.ic_trend
                ),
                // The text next to it already says what the button does.
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(
                stringResource(
                    if (showTrend) R.string.chart_show_history else R.string.chart_show_trend
                )
            )
        }
    }
}
