package com.example.healthlog.ui.profile

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.healthlog.R
import com.example.healthlog.model.UserGender
import com.example.healthlog.security.AndroidBiometricAuthenticator
import com.example.healthlog.security.AppAuthenticator
import com.example.healthlog.security.BiometricAvailability
import com.example.healthlog.ui.format.formatFullDate
import com.example.healthlog.ui.theme.HealthLogTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
        viewModel.messages.collect { messageRes ->
            snackbarHostState.showSnackbar(resources.getString(messageRes))
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

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
                onNameChange = viewModel::onNameChange,
                dateOfBirth = uiState.dateOfBirth,
                ageYears = uiState.ageYears,
                onOpenDatePicker = { showDatePicker = true },
                onClearDateOfBirth = { viewModel.onDateOfBirthChange(null) },
                gender = uiState.gender,
                onGenderChange = viewModel::onGenderChange
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
                        context.getString(R.string.profile_app_lock_enable_prompt_title)
                    } else {
                        context.getString(R.string.profile_app_lock_disable_prompt_title)
                    }
                    val subtitle = if (targetEnabled) {
                        context.getString(R.string.profile_app_lock_enable_prompt_subtitle)
                    } else {
                        context.getString(R.string.profile_app_lock_disable_prompt_subtitle)
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

            Spacer(modifier = Modifier.weight(1f, fill = false))

            // Save Action Button
            Button(
                onClick = { viewModel.saveProfile() },
                enabled = !uiState.isSaving && uiState.hasChangesFromLoaded,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_action_save),
                    fontWeight = FontWeight.SemiBold
                )
            }
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
}

@Composable
private fun PersonalInfoCard(
    name: String,
    onNameChange: (String) -> Unit,
    dateOfBirth: LocalDate?,
    ageYears: Int?,
    onOpenDatePicker: () -> Unit,
    onClearDateOfBirth: () -> Unit,
    gender: UserGender?,
    onGenderChange: (UserGender?) -> Unit,
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

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.profile_name_label)) },
                placeholder = { Text(stringResource(R.string.profile_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

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
                            if (dateOfBirth != null) {
                                IconButton(onClick = onClearDateOfBirth) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = stringResource(R.string.profile_clear_dob),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                IconButton(onClick = onOpenDatePicker) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_calendar),
                                        contentDescription = stringResource(R.string.profile_dob_placeholder),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Biological Sex / Gender
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
                        label = { Text(stringResource(R.string.profile_gender_male)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = gender == UserGender.FEMALE,
                        onClick = { onGenderChange(UserGender.FEMALE) },
                        label = { Text(stringResource(R.string.profile_gender_female)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
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

@Preview(showBackground = true)
@Composable
private fun PersonalInfoCardPreview() {
    HealthLogTheme {
        PersonalInfoCard(
            name = "Maria",
            onNameChange = {},
            dateOfBirth = LocalDate.of(1990, 5, 15),
            ageYears = 34,
            onOpenDatePicker = {},
            onClearDateOfBirth = {},
            gender = UserGender.FEMALE,
            onGenderChange = {}
        )
    }
}
