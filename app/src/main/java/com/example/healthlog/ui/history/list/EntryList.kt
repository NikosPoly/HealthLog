package com.example.healthlog.ui.history.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.database.Measurement

// The list mode of one history page: the cards of one type (EntryCard),
// newest first, or the empty text in the middle.
@Composable
internal fun EntryList(
    measurements: List<Measurement>,
    onOpenDetails: (Long) -> Unit,
    onRequestDelete: (Measurement) -> Unit
) {
    if (measurements.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(stringResource(R.string.history_empty))
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 16.dp
        )
    ) {
        items(
            items = measurements,
            key = { it.id }
        ) { measurement ->
            EntryCard(
                measurement = measurement,
                onViewDetails = {
                    onOpenDetails(measurement.id)
                },
                onDelete = {
                    onRequestDelete(measurement)
                }
            )
        }
    }
}
