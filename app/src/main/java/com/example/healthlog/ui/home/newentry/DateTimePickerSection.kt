package com.example.healthlog.ui.home.newentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.ui.format.formatLocale
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Section in the entry dialog allowing the user to either use the current date/time
// (checked by default) or pick a custom date and time via a combined sequential picker.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerSection(
    useCurrentDateTime: Boolean,
    timestamp: Long,
    onUseCurrentChange: (Boolean) -> Unit,
    onTimestampChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    val zoneId = ZoneId.systemDefault()
    val localDateTime = remember(timestamp) {
        Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDateTime()
    }

    val locale = formatLocale()
    val dateTimeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Toggle for automatic timestamp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = useCurrentDateTime,
                    role = Role.Checkbox,
                    onValueChange = onUseCurrentChange
                )
        ) {
            Checkbox(
                checked = useCurrentDateTime,
                onCheckedChange = null
            )
            Text(
                text = stringResource(R.string.entry_use_current_datetime),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // When unticked, display a single button to choose custom date & time
        if (!useCurrentDateTime) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = "  " + localDateTime.format(dateTimeFormatter),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateTime.toLocalDate()
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val pickedLocalDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            pendingDate = pickedLocalDate
                            showDatePicker = false
                            // Sequentially open the time picker right after date is picked!
                            showTimePicker = true
                        } else {
                            showDatePicker = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = localDateTime.hour,
            initialMinute = localDateTime.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
        )
        DatePickerDialog(
            onDismissRequest = {
                showTimePicker = false
                pendingDate = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = pendingDate ?: localDateTime.toLocalDate()
                        val pickedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val updatedDateTime = LocalDateTime.of(date, pickedTime)
                        onTimestampChange(updatedDateTime.atZone(zoneId).toInstant().toEpochMilli())
                        showTimePicker = false
                        pendingDate = null
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        pendingDate = null
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
            }
        }
    }
}
