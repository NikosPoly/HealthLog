package com.example.healthlog.ui.medications

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlog.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    viewModel: MedicationsViewModel,
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val todayDoses by viewModel.todayDoses.collectAsState()
    val allMedications by viewModel.allMedications.collectAsState()
    val isAddSheetOpen by viewModel.isAddSheetOpen.collectAsState()
    val editingMedication by viewModel.editingMedication.collectAsState()
    val deletingMedication by viewModel.deletingMedication.collectAsState()
    var showTogglePermissionDialog by rememberSaveable { mutableStateOf(false) }
    var pendingToggleMedicationId by rememberSaveable { mutableStateOf<Long?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pendingId = pendingToggleMedicationId
                if (pendingId != null) {
                    pendingToggleMedicationId = null
                    val isNowGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                    if (isNowGranted) {
                        val med = allMedications.find { it.medication.id == pendingId }
                        if (med != null && !med.medication.isActive) {
                            viewModel.toggleMedicationActive(med)
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Tab Row: Today's Schedule vs My Medicines
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            text = stringResource(R.string.medications_tab_today),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text(
                            text = stringResource(R.string.medications_tab_prescriptions),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == 0) {
                    TodayScheduleView(
                        doses = todayDoses,
                        onMarkTaken = viewModel::markDoseTaken,
                        onMarkSkipped = viewModel::markDoseSkipped,
                        onUndo = viewModel::undoDoseAction,
                        onAddMedication = viewModel::openAddMedication
                    )
                } else {
                    PrescriptionsView(
                        medications = allMedications,
                        todayDoses = todayDoses,
                        onEdit = viewModel::openEditMedication,
                        onToggleActive = { medWithReminders ->
                            viewModel.toggleMedicationActive(medWithReminders) {
                                pendingToggleMedicationId = medWithReminders.medication.id
                                showTogglePermissionDialog = true
                            }
                        },
                        onDelete = viewModel::openDeleteConfirmation,
                        onAddMedication = viewModel::openAddMedication,
                        onQuickLog = viewModel::quickLogAsNeeded
                    )
                }
            }
        }
    }

    if (deletingMedication != null) {
        val item = deletingMedication!!
        AlertDialog(
            onDismissRequest = viewModel::closeDeleteConfirmation,
            title = { Text(stringResource(R.string.medications_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.medications_delete_confirm_desc,
                        item.medication.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDelete
                ) {
                    Text(
                        stringResource(R.string.medications_action_delete),
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
            onSave = { name, dosage, unit, frequency, instructions, reminders, saveToPrescriptions, logDoseNow ->
                viewModel.saveMedication(
                    name = name,
                    dosage = dosage,
                    unit = unit,
                    frequency = frequency,
                    instructions = instructions,
                    reminderTimes = reminders,
                    saveToPrescriptions = saveToPrescriptions,
                    logDoseNow = logDoseNow
                )
            }
        )
    }

    if (showTogglePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showTogglePermissionDialog = false },
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
                TextButton(onClick = {
                    showTogglePermissionDialog = false
                    pendingToggleMedicationId = null
                }) {
                    Text(stringResource(R.string.notification_permission_dont_allow))
                }
            }
        )
    }
}
