package com.example.healthlog.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton

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
        OutlinedButton(
            onClick = onToggle,
            // An outline, so it reads as an action and not as something selected (like the chips);
            // in the main color, to match the rest of the chart screen.
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                painter = painterResource(
                    if (showTrend) R.drawable.ic_entries else R.drawable.ic_chart
                ),
                // The text next to it already says what the button does.
                contentDescription = null,
                // Multicolored icons: keep their own colors instead of the button's color.
                tint = Color.Unspecified,
                // A little bigger than the default 18 dp, so the details stay readable.
                modifier = Modifier.size(20.dp)
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
