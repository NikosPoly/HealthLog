package com.example.healthlog.ui.medications

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.healthlog.R
import com.example.healthlog.notifications.MedicationNotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    viewModel: MedicationsViewModel,
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val todayDoses by viewModel.todayDoses.collectAsState()
    val allMedications by viewModel.allMedications.collectAsState()
    val isAddSheetOpen by viewModel.isAddSheetOpen.collectAsState()
    val editingMedication by viewModel.editingMedication.collectAsState()
    val deletingMedication by viewModel.deletingMedication.collectAsState()
    val deletingMedicationUnloggedCount by viewModel.deletingMedicationUnloggedCount.collectAsState()

    var notificationsEnabled by remember {
        mutableStateOf(MedicationNotificationHelper.areNotificationsEnabled(context))
    }
    var showReminderAlertsSheet by rememberSaveable { mutableStateOf(false) }
    var followUpEnabled by remember {
        mutableStateOf(MedicationNotificationHelper.isFollowUpEnabled(context))
    }
    var keepRingingEnabled by remember {
        mutableStateOf(MedicationNotificationHelper.isKeepRingingEnabled(context))
    }
    var showTogglePermissionDialog by rememberSaveable { mutableStateOf(false) }
    var pendingToggleMedicationId by rememberSaveable { mutableStateOf<Long?>(null) }

    val togglePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val isNowGranted = MedicationNotificationHelper.areNotificationsEnabled(context)
        notificationsEnabled = isNowGranted
        val pendingId = pendingToggleMedicationId
        pendingToggleMedicationId = null
        if (isNowGranted && pendingId != null) {
            viewModel.enableMedicationAndSyncAlarms(pendingId)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val isNowGranted = MedicationNotificationHelper.areNotificationsEnabled(context)
                notificationsEnabled = isNowGranted
                val pendingId = pendingToggleMedicationId
                if (pendingId != null) {
                    pendingToggleMedicationId = null
                    if (isNowGranted) {
                        viewModel.enableMedicationAndSyncAlarms(pendingId)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { medName ->
            Toast.makeText(
                context,
                resources.getString(R.string.dose_logged_toast, medName),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.medications_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            followUpEnabled = MedicationNotificationHelper.isFollowUpEnabled(context)
                            keepRingingEnabled = MedicationNotificationHelper.isKeepRingingEnabled(context)
                            showReminderAlertsSheet = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notifications),
                            contentDescription = stringResource(R.string.reminder_alerts_action)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddMedication() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_medications),
                    contentDescription = stringResource(R.string.medications_action_add)
                )
            }
        },
        bottomBar = bottomBar
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrescriptionsView(
                medications = allMedications,
                todayDoses = todayDoses,
                notificationsEnabled = notificationsEnabled,
                onEdit = viewModel::openEditMedication,
                onToggleActive = { medWithReminders ->
                    viewModel.toggleMedicationActive(medWithReminders) {
                        pendingToggleMedicationId = medWithReminders.medication.id
                        val activity = context as? ComponentActivity
                        val hasSystemPermissionPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        val isPermanentlyDenied = activity != null && hasSystemPermissionPrompt &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                        val hasEverRequested = prefs.getBoolean("has_requested_notifications_startup", false)

                        if (!hasSystemPermissionPrompt || (isPermanentlyDenied && hasEverRequested)) {
                            showTogglePermissionDialog = true
                        } else {
                            togglePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                },
                onDelete = viewModel::openDeleteConfirmation,
                onAddMedication = viewModel::openAddMedication,
                onQuickLog = viewModel::quickLogAsNeeded
            )
        }
    }

    if (deletingMedication != null) {
        val item = deletingMedication!!
        AlertDialog(
            onDismissRequest = viewModel::closeDeleteConfirmation,
            title = { Text(stringResource(R.string.medications_delete_confirm_title)) },
            text = {
                Column {
                    Text(
                        stringResource(
                            R.string.medications_delete_confirm_desc,
                            item.medication.name
                        )
                    )
                    if (deletingMedicationUnloggedCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = pluralStringResource(
                                R.plurals.medications_delete_unlogged_warning,
                                deletingMedicationUnloggedCount,
                                deletingMedicationUnloggedCount
                            ),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete
                ) {
                    Text(
                        stringResource(
                            if (deletingMedicationUnloggedCount > 0) {
                                R.string.medications_action_delete_anyway
                            } else {
                                R.string.medications_action_delete
                            }
                        ),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeDeleteConfirmation) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (isAddSheetOpen) {
        AddEditMedicationSheet(
            editingItem = editingMedication,
            onDismiss = viewModel::closeAddEditSheet,
            onSave = { name, dosage, unit, frequency, instructions, reminders, saveToPrescriptions, logDoseNow, targetDate, oneTimeHourMinute ->
                notificationsEnabled = MedicationNotificationHelper.areNotificationsEnabled(context)
                viewModel.saveMedication(
                    name = name,
                    dosage = dosage,
                    unit = unit,
                    frequency = frequency,
                    instructions = instructions,
                    reminderTimes = reminders,
                    saveToPrescriptions = saveToPrescriptions,
                    logDoseNow = logDoseNow,
                    targetDate = targetDate,
                    oneTimeHourMinute = oneTimeHourMinute
                )
            }
        )
    }

    if (showTogglePermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showTogglePermissionDialog = false
                pendingToggleMedicationId = null
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
                        showTogglePermissionDialog = false
                        MedicationNotificationHelper.openNotificationSettings(context)
                    }
                ) {
                    Text(stringResource(R.string.notification_permission_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTogglePermissionDialog = false
                    pendingToggleMedicationId = null
                }) {
                    Text(stringResource(R.string.notification_permission_dont_allow))
                }
            }
        )
    }

    if (showReminderAlertsSheet) {
        ReminderAlertsSheet(
            followUpEnabled = followUpEnabled,
            keepRingingEnabled = keepRingingEnabled,
            onFollowUpChanged = { enabled ->
                followUpEnabled = enabled
                MedicationNotificationHelper.setFollowUpEnabled(context, enabled)
            },
            onKeepRingingChanged = { enabled ->
                keepRingingEnabled = enabled
                MedicationNotificationHelper.setKeepRingingEnabled(context, enabled)
            },
            onOpenSoundSettings = {
                MedicationNotificationHelper.openChannelNotificationSettings(context)
            },
            onDismiss = { showReminderAlertsSheet = false }
        )
    }
}
