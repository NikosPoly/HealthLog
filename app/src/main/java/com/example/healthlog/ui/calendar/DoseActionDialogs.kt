package com.example.healthlog.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.model.MedicationScheduleResolver
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.ui.medications.MedicationActions.PendingDoseMove
import com.example.healthlog.ui.medications.MedicationActions.RecurringDoseScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseTimePickerDialog(
    dose: ScheduledDoseItem,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val medName = dose.medicationName.ifBlank { stringResource(R.string.nav_medications) }
    val parts = dose.scheduledTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dose_move_picker_title, medName),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            ) {
                Text(
                    text = stringResource(android.R.string.ok),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun DoseMoveScopeDialog(
    pendingMove: PendingDoseMove,
    onDismiss: () -> Unit,
    onConfirmScope: (RecurringDoseScope) -> Unit
) {
    val medName = pendingMove.dose.medicationName.ifBlank { stringResource(R.string.nav_medications) }
    val formattedTime = MedicationScheduleResolver.formatTime(pendingMove.newHour, pendingMove.newMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dose_move_scope_title, formattedTime),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = stringResource(R.string.dose_move_scope_desc, medName))
                OutlinedButton(
                    onClick = { onConfirmScope(RecurringDoseScope.THIS_DAY_ONLY) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.dose_scope_this_day),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = { onConfirmScope(RecurringDoseScope.THIS_AND_FUTURE) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.dose_scope_this_and_future),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun DoseDeleteDialog(
    dose: ScheduledDoseItem,
    onDismiss: () -> Unit,
    onConfirmScope: (RecurringDoseScope) -> Unit
) {
    val medName = dose.medicationName.ifBlank { stringResource(R.string.nav_medications) }
    val isRecurring = (dose.isRecurring && dose.reminderId != null)

    if (isRecurring) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.dose_delete_scope_title, medName),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = stringResource(R.string.dose_delete_scope_desc))
                    OutlinedButton(
                        onClick = { onConfirmScope(RecurringDoseScope.THIS_DAY_ONLY) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.dose_scope_this_day),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = { onConfirmScope(RecurringDoseScope.THIS_AND_FUTURE) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.dose_scope_this_and_future),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.dose_delete_scope_title, medName),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.dose_delete_single_desc, medName))
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmScope(RecurringDoseScope.THIS_DAY_ONLY) }
                ) {
                    Text(
                        text = stringResource(R.string.medications_action_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
