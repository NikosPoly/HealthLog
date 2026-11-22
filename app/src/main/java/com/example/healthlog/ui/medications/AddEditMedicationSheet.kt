package com.example.healthlog.ui.medications

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import com.example.healthlog.database.MedicationWithReminders
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationSheet(
    editingItem: MedicationWithReminders?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        dosage: String,
        unit: String,
        frequency: String,
        instructions: String?,
        reminderTimes: List<Pair<Int, Int>>,
        saveToPrescriptions: Boolean,
        logDoseNow: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val initialName = editingItem?.medication?.name ?: ""
    val initialDosage = editingItem?.medication?.dosage ?: ""
    val initialUnit = editingItem?.medication?.unit ?: "mg"
    val initialFrequency = editingItem?.medication?.frequency ?: "DAILY"
    val initialInstructions = editingItem?.medication?.instructions ?: ""
    val initialReminders = editingItem?.reminders?.map { it.hour to it.minute }
        ?: if (initialFrequency == "DAILY") listOf(8 to 0) else emptyList()

    var name by rememberSaveable(editingItem?.medication?.id) { mutableStateOf(initialName) }
    var dosage by rememberSaveable(editingItem?.medication?.id) { mutableStateOf(initialDosage) }
    var selectedUnit by rememberSaveable(editingItem?.medication?.id) { mutableStateOf(initialUnit) }
    var frequency by rememberSaveable(editingItem?.medication?.id) { mutableStateOf(initialFrequency) }
    var instructions by rememberSaveable(editingItem?.medication?.id) { mutableStateOf(initialInstructions) }
    var saveToPrescriptions by rememberSaveable(editingItem?.medication?.id) {
        mutableStateOf(editingItem != null)
    }
    var logDoseNow by rememberSaveable(editingItem?.medication?.id) {
        mutableStateOf(true)
    }

    val reminderTimeListSaver = listSaver<List<Pair<Int, Int>>, String>(
        save = { list -> list.map { "${it.first}:${it.second}" } },
        restore = { savedList ->
            savedList.mapNotNull { str ->
                val parts = str.split(":")
                if (parts.size == 2) {
                    val h = parts[0].toIntOrNull()
                    val m = parts[1].toIntOrNull()
                    if (h != null && m != null) h to m else null
                } else null
            }
        }
    )

    val reminderTimesList = rememberSaveable(editingItem?.medication?.id, stateSaver = reminderTimeListSaver) {
        mutableStateOf(initialReminders)
    }

    var showPermissionPromptDialog by rememberSaveable { mutableStateOf(false) }
    var pendingSaveAfterPermission by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingSaveAfterPermission) {
                pendingSaveAfterPermission = false
                val isNowGranted = com.example.healthlog.notifications.MedicationNotificationHelper.areNotificationsEnabled(context)
                if (isNowGranted) {
                    onSave(
                        name,
                        dosage,
                        selectedUnit,
                        frequency,
                        instructions,
                        reminderTimesList.value,
                        saveToPrescriptions,
                        logDoseNow
                    )
                } else {
                    onDismiss()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onSave(
                name,
                dosage,
                selectedUnit,
                frequency,
                instructions,
                reminderTimesList.value,
                saveToPrescriptions,
                logDoseNow
            )
        } else {
            // User denied the permission on the system dialog -> close the saved medication window!
            onDismiss()
        }
    }

    var nameError by rememberSaveable { mutableStateOf(false) }
    var dosageError by rememberSaveable { mutableStateOf(false) }

    val units = listOf("mg", "tablet", "capsule", "ml", "drops", "puff")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = stringResource(
                    if (editingItem == null) R.string.med_form_title_add else R.string.med_form_title_edit
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Medication Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text(stringResource(R.string.med_form_name_label)) },
                placeholder = { Text(stringResource(R.string.med_form_name_hint)) },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text(stringResource(R.string.med_form_error_name_empty)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dosage and Unit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = dosage,
                    onValueChange = {
                        dosage = it
                        dosageError = false
                    },
                    label = { Text(stringResource(R.string.med_form_dosage_label)) },
                    placeholder = { Text(stringResource(R.string.med_form_dosage_hint)) },
                    isError = dosageError,
                    supportingText = if (dosageError) {
                        { Text(stringResource(R.string.med_form_error_dosage_empty)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )

                Column(modifier = Modifier.weight(1.8f)) {
                    Text(
                        text = stringResource(R.string.med_form_unit_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        units.take(3).forEach { unit ->
                            FilterChip(
                                selected = selectedUnit == unit,
                                onClick = { selectedUnit = unit },
                                label = { Text(unit, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        units.drop(3).forEach { unit ->
                            FilterChip(
                                selected = selectedUnit == unit,
                                onClick = { selectedUnit = unit },
                                label = { Text(unit, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency Selector (Recurring vs As-Needed / Once)
            Text(
                text = stringResource(R.string.med_form_frequency_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = frequency == "DAILY",
                    onClick = {
                        frequency = "DAILY"
                        if (reminderTimesList.value.isEmpty()) {
                            reminderTimesList.value = listOf(8 to 0)
                        }
                    },
                    label = { Text(stringResource(R.string.med_freq_daily)) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = frequency == "AS_NEEDED",
                    onClick = {
                        frequency = "AS_NEEDED"
                        reminderTimesList.value = emptyList() // No default alarms for as-needed
                    },
                    label = { Text(stringResource(R.string.med_freq_as_needed)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // As-Needed / One-Time Configuration Card
            if (frequency == "AS_NEEDED") {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.med_form_save_to_prescriptions),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.med_form_save_to_prescriptions_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = saveToPrescriptions,
                                onCheckedChange = { saveToPrescriptions = it }
                            )
                        }

                        if (saveToPrescriptions) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.med_form_log_now),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.med_form_log_now_desc),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = logDoseNow,
                                    onCheckedChange = { logDoseNow = it }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Reminder Timers Section (Optional)
            if (frequency == "DAILY") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.med_form_reminders_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedButton(
                        onClick = {
                            val timePicker = TimePickerDialog(
                                context,
                                { _, h, m ->
                                    reminderTimesList.value = reminderTimesList.value + (h to m)
                                },
                                12, 0, true
                            )
                            timePicker.show()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.med_form_add_reminder))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (reminderTimesList.value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.med_form_no_reminders),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        reminderTimesList.value.forEachIndexed { index, (h, m) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            TimePickerDialog(
                                                context,
                                                { _, newH, newM ->
                                                    val updated = reminderTimesList.value.toMutableList()
                                                    updated[index] = newH to newM
                                                    reminderTimesList.value = updated
                                                },
                                                h, m, true
                                            ).show()
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_nav_medications),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = String.format(Locale.US, "%02d:%02d", h, m),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val updated = reminderTimesList.value.toMutableList()
                                            updated.removeAt(index)
                                            reminderTimesList.value = updated
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_close),
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Instructions / Notes (Optional)
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text(stringResource(R.string.med_form_instructions_label)) },
                placeholder = { Text(stringResource(R.string.med_form_instructions_hint)) },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    val isNameValid = name.isNotBlank()
                    val isDosageValid = dosage.isNotBlank()

                    nameError = !isNameValid
                    dosageError = !isDosageValid

                    if (isNameValid && isDosageValid) {
                        val needsNotificationPermission = frequency == "DAILY" &&
                            reminderTimesList.value.isNotEmpty() &&
                            !com.example.healthlog.notifications.MedicationNotificationHelper.areNotificationsEnabled(context)

                        if (needsNotificationPermission) {
                            val activity = context as? ComponentActivity
                            val hasSystemPermissionPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            val isPermanentlyDenied = activity != null && hasSystemPermissionPrompt &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)

                            val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
                            val hasEverRequested = prefs.getBoolean("has_requested_notifications_startup", false)

                            if (!hasSystemPermissionPrompt || (isPermanentlyDenied && hasEverRequested)) {
                                showPermissionPromptDialog = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            onSave(
                                name,
                                dosage,
                                selectedUnit,
                                frequency,
                                instructions,
                                reminderTimesList.value,
                                saveToPrescriptions,
                                logDoseNow
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(
                        if (frequency == "AS_NEEDED" && !saveToPrescriptions) {
                            R.string.med_form_action_log_dose
                        } else {
                            R.string.med_form_save
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showPermissionPromptDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionPromptDialog = false
                onDismiss()
            },
            title = {
                Text(
                    text = stringResource(R.string.notification_permission_required_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.notification_permission_required_desc))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionPromptDialog = false
                        pendingSaveAfterPermission = true
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.notification_permission_enable))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionPromptDialog = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.notification_permission_dont_allow))
                }
            }
        )
    }
}
