package com.example.healthlog.ui.home.newentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// An item in a single-choice option group.
data class SelectorOption<T>(
    val value: T,
    val label: String,
    val iconRes: Int? = null
)

// A labeled group of single-choice chips for picking clinical context (e.g. Arm, Meal, Arrhythmia).
// Starts with no chip selected if selectedValue is null.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> OptionSelectorGroup(
    title: String,
    options: List<SelectorOption<T>>,
    selectedValue: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$title *",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = option.value == selectedValue
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(option.value) },
                    label = { Text(option.label) },
                    leadingIcon = option.iconRes?.let { icon ->
                        {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    }
                )
            }
        }
    }
}
