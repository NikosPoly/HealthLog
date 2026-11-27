package com.example.healthlog.ui.profile

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import com.example.healthlog.R
import com.example.healthlog.backup.BackupSchedule
import com.example.healthlog.model.UserGender
import com.example.healthlog.security.AndroidBiometricAuthenticator
import com.example.healthlog.security.AppAuthenticator
import com.example.healthlog.security.BiometricAvailability
import com.example.healthlog.ui.format.formatFullDate
import com.example.healthlog.ui.format.formatTimestamp
import com.example.healthlog.ui.format.rememberDateTimeFormatter
import com.example.healthlog.ui.theme.HealthLogTheme
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    authenticator: AppAuthenticator = AndroidBiometricAuthenticator
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.refreshBiometricAvailability(context)
        viewModel.refreshBackupStatus()
        viewModel.messages.collect { messageRes ->
            snackbarHostState.showSnackbar(resources.getString(messageRes))
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showManualBackupDialog by remember { mutableStateOf(false) }
    var showAutoPasswordDialog by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf<String?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportManualBackup(context, uri, password = null)
        }
        pendingExportPassword = null
    }

    val exportEncryptedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val password = pendingExportPassword
        if (uri != null) {
            viewModel.exportManualBackup(context, uri, password = password)
        }
        pendingExportPassword = null
    }

    val restoreDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.inspectBackupUri(context, uri)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.onCustomFolderSelected(uri)
        }
    }

    fun launchManualExport(password: String?) {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        if (!password.isNullOrBlank()) {
            pendingExportPassword = password
            exportEncryptedLauncher.launch("healthlog_backup_$dateStr.hlbackup")
        } else {
            pendingExportPassword = null
            exportJsonLauncher.launch("healthlog_backup_$dateStr.json")
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_screen_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_left),
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Personal Information Section
            PersonalInfoCard(
                name = uiState.name,
                nameError = uiState.usernameError,
                onNameChange = viewModel::onNameChange,
                dateOfBirth = uiState.dateOfBirth,
                ageYears = uiState.ageYears,
                onOpenDatePicker = { showDatePicker = true },
                onClearDateOfBirth = { viewModel.onDateOfBirthChange(null) },
                gender = uiState.gender,
                onGenderChange = viewModel::onGenderChange,
                canSave = !uiState.isSaving && uiState.hasChangesFromLoaded && uiState.isInputValid,
                onSave = { viewModel.saveProfile() }
            )

            // Security & Privacy Section
            SecurityCard(
                isAppLockEnabled = uiState.isAppLockEnabled,
                availability = uiState.biometricAvailability,
                onToggleAppLock = { targetEnabled ->
                    val activity = context.findFragmentActivity()
                    if (activity == null) {
                        viewModel.onAppLockToggled(targetEnabled)
                        return@SecurityCard
                    }

                    val title = if (targetEnabled) {
                        resources.getString(R.string.profile_app_lock_enable_prompt_title)
                    } else {
                        resources.getString(R.string.profile_app_lock_disable_prompt_title)
                    }
                    val subtitle = if (targetEnabled) {
                        resources.getString(R.string.profile_app_lock_enable_prompt_subtitle)
                    } else {
                        resources.getString(R.string.profile_app_lock_disable_prompt_subtitle)
                    }

                    authenticator.authenticate(
                        activity = activity,
                        title = title,
                        subtitle = subtitle,
                        onSuccess = {
                            viewModel.onAppLockToggled(targetEnabled)
                        },
                        onError = {
                            // Cancelled or failed: do not alter the toggle.
                        }
                    )
                }
            )

            // Data & Backup Section
            DataBackupCard(
                schedule = uiState.backupSchedule,
                lastBackupTimestamp = uiState.lastBackupTimestamp,
                customFolderUri = uiState.customBackupFolderUri,
                isAutoBackupPasswordEnabled = uiState.isAutoBackupPasswordEnabled,
                isBackupOverdue = uiState.isBackupOverdue,
                isBackupInProgress = uiState.isBackupInProgress,
                onScheduleSelected = { schedule ->
                    viewModel.onBackupScheduleSelected(context, schedule)
                },
                onPickFolder = { folderPickerLauncher.launch(null) },
                onResetFolder = { viewModel.onCustomFolderSelected(null) },
                onOpenAutoPasswordDialog = { showAutoPasswordDialog = true },
                onExportClick = { showManualBackupDialog = true },
                onRestoreClick = {
                    restoreDocumentLauncher.launch(
                        arrayOf("application/json", "application/octet-stream", "*/*")
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showDatePicker) {
        val initialMillis = uiState.dateOfBirth
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli()
            ?: System.currentTimeMillis()

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selectedDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            viewModel.onDateOfBirthChange(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
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

    if (showManualBackupDialog) {
        ManualBackupDialog(
            onDismiss = { showManualBackupDialog = false },
            onConfirmExport = { password ->
                showManualBackupDialog = false
                launchManualExport(password)
            }
        )
    }

    if (showAutoPasswordDialog) {
        AutoBackupPasswordDialog(
            hasExistingPassword = uiState.isAutoBackupPasswordEnabled,
            onDismiss = { showAutoPasswordDialog = false },
            onSavePassword = { password ->
                showAutoPasswordDialog = false
                viewModel.onAutoBackupPasswordChanged(password)
            }
        )
    }

    when (val restoreState = uiState.pendingRestore) {
        is PendingRestoreState.NeedsPassword -> {
            RestorePasswordDialog(
                metadata = restoreState.metadata,
                isWrongPassword = restoreState.isWrongPassword,
                isBusy = uiState.isBackupInProgress,
                onDismiss = viewModel::dismissRestoreDialog,
                onSubmitPassword = viewModel::submitRestorePassword
            )
        }

        is PendingRestoreState.ReadyToConfirm -> {
            RestoreConfirmDialog(
                payload = restoreState.payload,
                isBusy = uiState.isBackupInProgress,
                onDismiss = viewModel::dismissRestoreDialog,
                onConfirmRestore = { viewModel.confirmRestore(context) }
            )
        }

        null -> Unit
    }
}

@Composable
private fun PersonalInfoCard(
    name: String,
    nameError: com.example.healthlog.model.UsernameValidationError?,
    onNameChange: (String) -> Unit,
    dateOfBirth: LocalDate?,
    ageYears: Int?,
    onOpenDatePicker: () -> Unit,
    onClearDateOfBirth: () -> Unit,
    gender: UserGender?,
    onGenderChange: (UserGender?) -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_section_personal_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Name / Username: displays as clean read-only by default; tapping the pencil enables editing
            var isNameEditing by remember { mutableStateOf(false) }
            val nameFocusRequester = remember { FocusRequester() }

            LaunchedEffect(isNameEditing) {
                if (isNameEditing) {
                    nameFocusRequester.requestFocus()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.profile_name_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isNameEditing) {
                    // Read-only presentation card with pencil action
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isNameEditing = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = name.ifBlank { stringResource(R.string.profile_name_placeholder) },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (name.isNotBlank()) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                },
                                fontWeight = if (name.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { isNameEditing = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit),
                                    contentDescription = stringResource(R.string.profile_name_label),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Active edit field
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        placeholder = { Text(stringResource(R.string.profile_name_placeholder)) },
                        isError = nameError != null,
                        trailingIcon = {
                            IconButton(onClick = { isNameEditing = false }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.action_cancel),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        supportingText = {
                            if (nameError != null) {
                                val errorText = when (nameError) {
                                    com.example.healthlog.model.UsernameValidationError.MustStartWithLetter ->
                                        stringResource(R.string.username_error_start_letter)
                                    com.example.healthlog.model.UsernameValidationError.TooShort ->
                                        stringResource(R.string.username_error_too_short)
                                    com.example.healthlog.model.UsernameValidationError.TooLong ->
                                        stringResource(R.string.username_error_too_long)
                                    com.example.healthlog.model.UsernameValidationError.InvalidCharacters ->
                                        stringResource(R.string.username_error_invalid_chars)
                                }
                                Text(
                                    text = errorText,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester)
                    )
                }
            }

            // Date of Birth & Age
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.profile_dob_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenDatePicker),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = if (dateOfBirth != null) {
                            val formattedDate = formatFullDate(dateOfBirth)
                            if (ageYears != null) {
                                "$formattedDate (${stringResource(R.string.profile_age_format, ageYears)})"
                            } else {
                                formattedDate
                            }
                        } else {
                            ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text(stringResource(R.string.profile_dob_placeholder)) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (dateOfBirth != null) {
                                    IconButton(onClick = onClearDateOfBirth) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_close),
                                            contentDescription = stringResource(R.string.profile_clear_dob),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = onOpenDatePicker) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_edit),
                                        contentDescription = stringResource(R.string.profile_dob_placeholder),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Biological Sex / Gender with male and female icons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.profile_gender_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = gender == UserGender.MALE,
                        onClick = { onGenderChange(UserGender.MALE) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_gender_male),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.profile_gender_male)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD0E4FF),
                            selectedLabelColor = Color(0xFF001D36)
                        )
                    )

                    FilterChip(
                        selected = gender == UserGender.FEMALE,
                        onClick = { onGenderChange(UserGender.FEMALE) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_gender_female),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.profile_gender_female)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFB3C6),
                            selectedLabelColor = Color(0xFF5C001F)
                        )
                    )
                }
            }

            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.profile_action_save),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SecurityCard(
    isAppLockEnabled: Boolean,
    availability: BiometricAvailability,
    onToggleAppLock: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_section_security),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.profile_app_lock_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.profile_app_lock_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                val isToggleEnabled = availability != BiometricAvailability.UNAVAILABLE
                Switch(
                    checked = isAppLockEnabled,
                    onCheckedChange = onToggleAppLock,
                    enabled = isToggleEnabled
                )
            }

            when (availability) {
                BiometricAvailability.NO_CREDENTIAL_ENROLLED -> {
                    Text(
                        text = stringResource(R.string.profile_app_lock_no_credential_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                BiometricAvailability.UNAVAILABLE -> {
                    Text(
                        text = stringResource(R.string.profile_app_lock_unavailable_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                BiometricAvailability.AVAILABLE -> Unit
            }
        }
    }
}

@Composable
private fun DataBackupCard(
    schedule: BackupSchedule,
    lastBackupTimestamp: Long,
    customFolderUri: Uri?,
    isAutoBackupPasswordEnabled: Boolean,
    isBackupOverdue: Boolean,
    isBackupInProgress: Boolean,
    onScheduleSelected: (BackupSchedule) -> Unit,
    onPickFolder: () -> Unit,
    onResetFolder: () -> Unit,
    onOpenAutoPasswordDialog: () -> Unit,
    onExportClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateTimeFormatter = rememberDateTimeFormatter()
    var scheduleMenuExpanded by remember { mutableStateOf(false) }

    val customFolderName = remember(customFolderUri) {
        if (customFolderUri == null) {
            null
        } else {
            runCatching { DocumentFile.fromTreeUri(context, customFolderUri)?.name }.getOrNull()
                ?: customFolderUri.lastPathSegment?.substringAfterLast(':')
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.backup_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (isBackupInProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (isBackupOverdue) {
                BackupWarningBanner(
                    schedule = schedule,
                    lastBackupTimestamp = lastBackupTimestamp,
                    enabled = !isBackupInProgress,
                    onBackUpNow = onExportClick
                )
            }

            // Last Backup Status
            val lastBackupText = if (lastBackupTimestamp > 0L) {
                formatTimestamp(lastBackupTimestamp, dateTimeFormatter)
            } else {
                stringResource(R.string.backup_last_backup_never)
            }
            Text(
                text = stringResource(R.string.backup_last_backup_label, lastBackupText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Automatic Backup Interval Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isBackupInProgress) { scheduleMenuExpanded = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_backup),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.backup_schedule_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = scheduleLabel(schedule),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = scheduleMenuExpanded,
                    onDismissRequest = { scheduleMenuExpanded = false }
                ) {
                    BackupSchedule.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = scheduleLabel(option),
                                    fontWeight = if (option == schedule) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == schedule) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            onClick = {
                                scheduleMenuExpanded = false
                                onScheduleSelected(option)
                            }
                        )
                    }
                }
            }

            // Auto-Backup Folder Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isBackupInProgress) { onPickFolder() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.backup_folder_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (customFolderUri == null) {
                                stringResource(R.string.backup_folder_default)
                            } else {
                                customFolderName ?: stringResource(R.string.backup_folder_custom_fallback)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (customFolderUri != null) {
                    IconButton(
                        onClick = onResetFolder,
                        enabled = !isBackupInProgress,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.backup_folder_reset_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Auto-Backup Password
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isBackupInProgress) { onOpenAutoPasswordDialog() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_key),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.backup_auto_password_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isAutoBackupPasswordEnabled) {
                                stringResource(R.string.backup_auto_password_enabled)
                            } else {
                                stringResource(R.string.backup_auto_password_disabled)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Manual Export & Restore Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onExportClick,
                    enabled = !isBackupInProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_backup),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.backup_action_export),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onRestoreClick,
                    enabled = !isBackupInProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_restore),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.backup_action_restore),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupWarningBanner(
    schedule: BackupSchedule,
    lastBackupTimestamp: Long,
    enabled: Boolean,
    onBackUpNow: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF3D2900) else Color(0xFFFFF4E0)
    val accentColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFD97706)
    val textColor = if (isDark) Color(0xFFFFE0B2) else Color(0xFF5C3400)

    val messageRes = when {
        !schedule.isEnabled -> R.string.backup_warning_off
        lastBackupTimestamp <= 0L -> R.string.backup_warning_never
        else -> R.string.backup_warning_overdue
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onBackUpNow,
                    enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                ) {
                    Text(
                        text = stringResource(R.string.backup_action_back_up_now),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun scheduleLabel(schedule: BackupSchedule): String {
    return when (schedule) {
        BackupSchedule.DAILY -> stringResource(R.string.backup_schedule_daily)
        BackupSchedule.THREE_DAYS -> stringResource(R.string.backup_schedule_three_days)
        BackupSchedule.WEEKLY -> stringResource(R.string.backup_schedule_weekly)
        BackupSchedule.TWO_WEEKS -> stringResource(R.string.backup_schedule_two_weeks)
        BackupSchedule.MONTHLY -> stringResource(R.string.backup_schedule_monthly)
        BackupSchedule.OFF -> stringResource(R.string.backup_schedule_off)
    }
}

@Preview(showBackground = true)
@Composable
private fun PersonalInfoCardPreview() {
    HealthLogTheme {
        PersonalInfoCard(
            name = "Maria",
            nameError = null,
            onNameChange = {},
            dateOfBirth = LocalDate.of(1990, 5, 15),
            ageYears = 34,
            onOpenDatePicker = {},
            onClearDateOfBirth = {},
            gender = UserGender.FEMALE,
            onGenderChange = {},
            canSave = true,
            onSave = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DataBackupCardPreview() {
    HealthLogTheme {
        DataBackupCard(
            schedule = BackupSchedule.WEEKLY,
            lastBackupTimestamp = 0L,
            customFolderUri = null,
            isAutoBackupPasswordEnabled = false,
            isBackupOverdue = true,
            isBackupInProgress = false,
            onScheduleSelected = {},
            onPickFolder = {},
            onResetFolder = {},
            onOpenAutoPasswordDialog = {},
            onExportClick = {},
            onRestoreClick = {}
        )
    }
}
