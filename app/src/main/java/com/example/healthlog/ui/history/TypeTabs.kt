package com.example.healthlog.ui.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.model.MeasurementType

// One tab per type, at the top of the history. The row scrolls sideways when the names
// don't fit. The selected type is bold, in the main color, with the underline below it;
// the others are gray.
@Composable
internal fun TypeTabs(
    types: List<MeasurementType>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        // No extra space before the first and after the last tab, so the row
        // starts at the left edge and stops at the first and last tab.
        edgePadding = 3.dp
    ) {
        types.forEachIndexed { index, type ->
            val selected = index == selectedIndex

            Tab(
                selected = selected,
                onClick = { onSelect(index) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Text(
                        text = stringResource(type.label),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}
