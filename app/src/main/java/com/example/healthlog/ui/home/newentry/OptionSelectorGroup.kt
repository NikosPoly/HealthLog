package com.example.healthlog.ui.home.newentry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// An item in a single-choice option group.
data class SelectorOption<T>(
    val value: T,
    val label: String,
    val iconRes: Int? = null
)

// A group of single-choice chips with icon only (or compact label) for picking clinical context.
// Title is placed on the left, options flow to the right, followed by a red asterisk.
// When isError is true, the title turns red and a subtle red border wraps the group.
@Composable
fun <T> OptionSelectorGroup(
    title: String,
    options: List<SelectorOption<T>>,
    selectedValue: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val borderColor = if (isError) MaterialTheme.colorScheme.error else Color.Transparent
    val backgroundColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.06f) else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (isError) {
                    this.error("Selection required")
                }
            }
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.small
            )
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option.value == selectedValue
                val shape = MaterialTheme.shapes.small
                val borderColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                }
                val containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = shape
                        )
                        .background(
                            color = containerColor,
                            shape = shape
                        )
                        .clickable { onSelect(option.value) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    option.iconRes?.let { icon ->
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = option.label,
                            modifier = Modifier.size(27.dp),
                            tint = Color.Unspecified
                        )
                    }
                }
            }
            Text(
                text = "*",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 3.dp)
            )
        }
    }
}
