package com.example.healthlog.ui.chart

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R

// The chips that choose how far back the chart looks: 7 days, 30 days, 6 months, 1 year, All.
// Centered; on a narrow screen the row scrolls sideways.
@Composable
fun PeriodChips(
    selected: ChartPeriod,
    onSelect: (ChartPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChartPeriod.entries.forEach { period ->
                FilterChip(
                    selected = period == selected,
                    onClick = { onSelect(period) },
                    label = { Text(stringResource(period.label)) }
                )
            }
        }
    }
}

// The text of each period's chip.
@get:StringRes
private val ChartPeriod.label: Int
    get() = when (this) {
        ChartPeriod.SEVEN_DAYS -> R.string.chart_period_7_days
        ChartPeriod.THIRTY_DAYS -> R.string.chart_period_30_days
        ChartPeriod.SIX_MONTHS -> R.string.chart_period_6_months
        ChartPeriod.ONE_YEAR -> R.string.chart_period_1_year
        ChartPeriod.ALL -> R.string.chart_period_all
    }
