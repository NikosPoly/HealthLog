package com.example.healthlog.ui.medications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthlog.R
import com.example.healthlog.database.Medication
import com.example.healthlog.database.MedicationWithReminders
import com.example.healthlog.model.ScheduledDoseItem
import com.example.healthlog.model.ScheduledDoseStatus
import java.util.Locale

@Composable
fun PrescriptionsView(
    medications: List<MedicationWithReminders>,
    todayDoses: List<ScheduledDoseItem> = emptyList(),
    notificationsEnabled: Boolean = true,
    onEdit: (MedicationWithReminders) -> Unit,
    onToggleActive: (MedicationWithReminders) -> Unit,
    onDelete: (MedicationWithReminders) -> Unit,
    onAddMedication: () -> Unit,
    onQuickLog: (Medication) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visibleMedications = medications.filter {
        !it.medication.isDeleted && it.medication.frequency != "ONE_TIME"
    }

    if (visibleMedications.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_medications),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.medications_empty_prescriptions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.medications_empty_prescriptions_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAddMedication,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.medications_action_add))
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(visibleMedications) { medWithReminders ->
                PrescriptionCard(
                    item = medWithReminders,
                    todayDoses = todayDoses,
                    notificationsEnabled = notificationsEnabled,
                    onEdit = { onEdit(medWithReminders) },
                    onToggleActive = { onToggleActive(medWithReminders) },
                    onDelete = { onDelete(medWithReminders) },
                    onQuickLog = { onQuickLog(medWithReminders.medication) }
                )
            }
        }
    }
}

@Composable
fun PrescriptionCard(
    item: MedicationWithReminders,
    todayDoses: List<ScheduledDoseItem> = emptyList(),
    notificationsEnabled: Boolean = true,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onQuickLog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val med = item.medication
    val isActive = med.isActive
    val isShadedForPermission = !notificationsEnabled && med.frequency == "DAILY" && item.reminders.isNotEmpty()
    val isVisuallyActive = isActive && !isShadedForPermission

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isVisuallyActive) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(
            1.dp,
            if (isVisuallyActive) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .alpha(if (isVisuallyActive) 1f else 0.6f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_medications),
                        contentDescription = null,
                        tint = if (isVisuallyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = med.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${med.dosage} ${med.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (med.frequency == "DAILY") {
                    Switch(
                        checked = isActive,
                        onCheckedChange = { onToggleActive() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                } else {
                    FilledTonalButton(
                        onClick = onQuickLog,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.dose_action_mark_taken),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.dose_action_mark_taken),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reminders / Frequency Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (med.frequency == "DAILY") {
                            stringResource(R.string.med_freq_daily)
                        } else {
                            stringResource(R.string.med_freq_as_needed)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (item.reminders.isNotEmpty()) {
                    item.reminders.forEach { reminder ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🔔 " + String.format(Locale.US, "%02d:%02d", reminder.hour, reminder.minute),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (isActive && med.frequency == "DAILY" && item.reminders.any { it.isEnabled }) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "⏱ " + getNextDoseText(item, todayDoses),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!med.instructions.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = med.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card Bottom Actions (Edit, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_medications),
                        contentDescription = stringResource(R.string.medications_action_edit),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.medications_action_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun getNextDoseText(
    item: MedicationWithReminders,
    todayDoses: List<ScheduledDoseItem>
): String {
    val med = item.medication
    if (med.frequency != "DAILY" || item.reminders.none { it.isEnabled }) {
        return ""
    }

    // Look for the earliest scheduled dose today for this medicine that is still PENDING
    val pendingToday = todayDoses.firstOrNull {
        it.medicationId == med.id && it.status == ScheduledDoseStatus.PENDING
    }

    return if (pendingToday != null) {
        stringResource(R.string.med_next_dose_today, pendingToday.scheduledTime)
    } else {
        // All of today's doses have been taken or skipped (or none left today).
        // Advance to the earliest reminder tomorrow.
        val enabledReminders = item.reminders
            .filter { it.isEnabled }
            .sortedWith(compareBy({ it.hour }, { it.minute }))

        if (enabledReminders.isNotEmpty()) {
            val firstTomorrow = enabledReminders.first()
            val timeStr = String.format(Locale.US, "%02d:%02d", firstTomorrow.hour, firstTomorrow.minute)
            stringResource(R.string.med_next_dose_tomorrow, timeStr)
        } else {
            stringResource(R.string.med_next_dose_none)
        }
    }
}
