package com.example.healthlog.ui.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement
import com.example.healthlog.model.MeasurementType
import java.time.ZonedDateTime

// A tap anywhere on the screen, at a position relative to the whole screen.
// A new object for every tap, even at the same spot, so each one is noticed.
class ScreenTap(val position: Offset)

// The trend page of one type: its chart, and its stats below it.
// Without measurements in the period, a message instead of the chart and the period stats.
// The latest measurement is always shown, whatever the period.
// Scrolls up and down when it doesn't fit (small screens, landscape).
// The box of a tapped dot is closed by a tap anywhere on the screen outside the chart,
// and it never comes back by itself: not after swiping to another tab and back,
// nor after choosing another period.
@Composable
fun ChartPage(
    type: MeasurementType,
    // All the measurements of the type, newest first.
    measurements: List<Measurement>,
    period: ChartPeriod,
    // The latest tap anywhere on the screen, or null before the first one.
    screenTap: ScreenTap?
) {
    val inPeriod = remember(measurements, period) {
        measurements.inPeriod(period, ZonedDateTime.now())
    }

    // When any of these three changes, the chart is drawn from scratch, without the box
    // of a tapped dot:
    // - pageInstance is new every time this page is shown (e.g. after swiping back to it)
    // - inPeriod changes with the period and the data
    // - closeCount goes up on every tap outside the chart
    val pageInstance = remember { Any() }
    var closeCount by remember { mutableIntStateOf(0) }
    val chartPosition = remember { ChartPosition() }

    // Taps on the chart itself are handled by the chart.
    LaunchedEffect(screenTap) {
        if (screenTap != null && !chartPosition.contains(screenTap.position)) closeCount++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (inPeriod.isEmpty()) {
            Text(stringResource(R.string.chart_empty))
        } else {
            // Read aloud by screen readers, e.g. "Chart of Weight".
            val description = stringResource(R.string.chart_description, stringResource(type.label))

            key(pageInstance, inPeriod, closeCount) {
                MeasurementChart(
                    type = type,
                    measurements = inPeriod,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .onGloballyPositioned { chartPosition.coordinates = it }
                        .semantics { contentDescription = description }
                )
            }

            ChartStatsSection(type = type, inPeriod = inPeriod)
            HorizontalDivider()
        }

        // The list is newest first. A type without any measurement shows only the message.
        measurements.firstOrNull()?.let { latest ->
            LatestStatsSection(latest = latest)
        }
    }
}

// Where the chart is on the screen, to tell whether a tap was on it.
// A plain field, not state: it changes on every layout and nothing is redrawn from it.
private class ChartPosition {
    var coordinates: LayoutCoordinates? = null

    fun contains(position: Offset): Boolean {
        val chart = coordinates?.takeIf { it.isAttached } ?: return false
        return chart.boundsInRoot().contains(position)
    }
}
