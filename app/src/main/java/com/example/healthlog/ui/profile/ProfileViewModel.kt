package com.example.healthlog.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthlog.R
import com.example.healthlog.backup.AutoBackupWorker
import com.example.healthlog.backup.BackupCrypto
import com.example.healthlog.backup.BackupMetadata
import com.example.healthlog.backup.BackupPayload
import com.example.healthlog.backup.BackupSchedule
import com.example.healthlog.backup.BackupSerializer
import com.example.healthlog.backup.BackupStorage
import com.example.healthlog.model.UserGender
import com.example.healthlog.model.calculateAgeYears
import com.example.healthlog.model.parseDateOfBirthOrNull
import com.example.healthlog.repository.BackupRepository
import com.example.healthlog.repository.UserProfileRepository
import com.example.healthlog.security.AndroidBiometricAuthenticator
import com.example.healthlog.security.AppAuthenticator
import com.example.healthlog.security.AppLockManager
import com.example.healthlog.security.BiometricAvailability
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed interface PendingRestoreState {
    data class NeedsPassword(
        val rawContent: String,
        val metadata: BackupMetadata,
        val isWrongPassword: Boolean = false
    ) : PendingRestoreState

    data class ReadyToConfirm(
        val payload: BackupPayload
    ) : PendingRestoreState
}

data class ProfileUiState(
    val name: String = "",
    val dateOfBirth: LocalDate? = null,
    val gender: UserGender? = null,
    val isAppLockEnabled: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.AVAILABLE,
    val isSaving: Boolean = false,
    val backupSchedule: BackupSchedule = BackupSchedule.DEFAULT,
    val lastBackupTimestamp: Long = 0L,
    val customBackupFolderUri: Uri? = null,
    val isAutoBackupPasswordEnabled: Boolean = false,
    val isBackupOverdue: Boolean = false,
    val isBackupInProgress: Boolean = false,
    val pendingRestore: PendingRestoreState? = null
) {
    val usernameError: com.example.healthlog.model.UsernameValidationError?
        get() = com.example.healthlog.model.UsernameValidationRules.validate(name)

    val isInputValid: Boolean
        get() = usernameError == null

    val ageYears: Int?
        get() = calculateAgeYears(dateOfBirth)

    val hasChangesFromLoaded: Boolean
        get() = name.trim() != loadedName.trim() || dateOfBirth != loadedDateOfBirth || gender != loadedGender

    internal var loadedName: String = ""
    internal var loadedDateOfBirth: LocalDate? = null
    internal var loadedGender: UserGender? = null
}

class ProfileViewModel(
    private val repository: UserProfileRepository,
    private val appLockManager: AppLockManager,
    private val authenticator: AppAuthenticator = AndroidBiometricAuthenticator,
    private val backupRepository: BackupRepository? = null,
    private val backupStorage: BackupStorage? = null,
    private val scheduleUpdater: (Context, BackupSchedule) -> Unit = AutoBackupWorker::updateSchedule,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<Int>()
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    private var isInitialLoaded = false

    init {
        scope.launch {
            repository.observeProfile().collect { profile ->
                val parsedDob = parseDateOfBirthOrNull(profile?.dateOfBirth)
                val parsedGender = UserGender.fromNameOrNull(profile?.gender)
                val loadedName = profile?.name.orEmpty()

                if (!isInitialLoaded) {
                    isInitialLoaded = true
                    _uiState.update { current ->
                        current.copy(
                            name = loadedName,
                            dateOfBirth = parsedDob,
                            gender = parsedGender,
                            isAppLockEnabled = appLockManager.isLockEnabled.value
                        ).apply {
                            this.loadedName = loadedName
                            this.loadedDateOfBirth = parsedDob
                            this.loadedGender = parsedGender
                        }
                    }
                } else {
                    _uiState.update { current ->
                        current.copy().apply {
                            this.loadedName = loadedName
                            this.loadedDateOfBirth = parsedDob
                            this.loadedGender = parsedGender
                        }
                    }
                }
            }
        }

        if (backupStorage != null) {
            scope.launch {
                combine(
                    backupStorage.schedule,
                    backupStorage.lastBackupTimestamp,
                    backupStorage.customFolderUri,
                    backupStorage.isAutoBackupPasswordEnabled
                ) { schedule, lastTimestamp, folderUri, hasPassword ->
                    BackupSettingsSnapshot(
                        schedule = schedule,
                        lastBackupTimestamp = lastTimestamp,
                        customFolderUri = folderUri,
                        isPasswordEnabled = hasPassword,
                        isOverdue = backupStorage.isBackupOverdue()
                    )
                }.collect { snapshot ->
                    _uiState.update { current ->
                        current.copy(
                            backupSchedule = snapshot.schedule,
                            lastBackupTimestamp = snapshot.lastBackupTimestamp,
                            customBackupFolderUri = snapshot.customFolderUri,
                            isAutoBackupPasswordEnabled = snapshot.isPasswordEnabled,
                            isBackupOverdue = snapshot.isOverdue
                        ).apply {
                            this.loadedName = current.loadedName
                            this.loadedDateOfBirth = current.loadedDateOfBirth
                            this.loadedGender = current.loadedGender
                        }
                    }
                }
            }
        }
    }

    private data class BackupSettingsSnapshot(
        val schedule: BackupSchedule,
        val lastBackupTimestamp: Long,
        val customFolderUri: Uri?,
        val isPasswordEnabled: Boolean,
        val isOverdue: Boolean
    )

    fun refreshBiometricAvailability(context: Context) {
        val availability = authenticator.checkAvailability(context)
        _uiState.update { it.copy(biometricAvailability = availability) }
    }

    fun refreshBackupStatus() {
        val storage = backupStorage ?: return
        _uiState.update { it.copy(isBackupOverdue = storage.isBackupOverdue()) }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun onDateOfBirthChange(newDate: LocalDate?) {
        _uiState.update { it.copy(dateOfBirth = newDate) }
    }

    fun onGenderChange(newGender: UserGender?) {
        // Tapping the currently selected gender clears it (optional field).
        _uiState.update { current ->
            current.copy(gender = if (current.gender == newGender) null else newGender)
        }
    }

    fun onAppLockToggled(enabled: Boolean) {
        appLockManager.setLockEnabled(enabled)
        _uiState.update { it.copy(isAppLockEnabled = enabled) }
    }

    fun onBackupScheduleSelected(context: Context, schedule: BackupSchedule) {
        backupStorage?.setSchedule(schedule)
        scheduleUpdater(context.applicationContext ?: context, schedule)
        refreshBackupStatus()
    }

    fun onCustomFolderSelected(uri: Uri?) {
        backupStorage?.setCustomFolderUri(uri)
        scope.launch {
            _messages.emit(
                if (uri != null) R.string.backup_folder_updated else R.string.backup_folder_reset
            )
        }
    }

    fun onAutoBackupPasswordChanged(password: String?) {
        val cleaned = password?.takeIf { it.isNotBlank() }
        backupStorage?.setAutoBackupPassword(cleaned)
        scope.launch {
            _messages.emit(
                if (cleaned != null) R.string.backup_auto_password_saved
                else R.string.backup_auto_password_cleared
            )
        }
    }

    fun exportManualBackup(context: Context, destinationUri: Uri, password: String?) {
        val repo = backupRepository ?: return
        scope.launch {
            _uiState.update { it.copy(isBackupInProgress = true) }
            val succeeded = try {
                withContext(ioDispatcher) {
                    val isEncrypted = !password.isNullOrBlank()
                    val payload = repo.createBackupPayload(isEncrypted = isEncrypted)
                    val plainJson = BackupSerializer.toJson(payload)
                    val finalContent = if (isEncrypted) {
                        BackupCrypto.encrypt(plainJson, password.toCharArray(), payload.metadata)
                    } else {
                        plainJson
                    }
                    context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                        out.write(finalContent.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } ?: return@withContext false
                    true
                }
            } catch (_: Exception) {
                false
            } finally {
                _uiState.update { it.copy(isBackupInProgress = false) }
            }

            if (succeeded) {
                backupStorage?.setLastBackupTimestamp(System.currentTimeMillis())
                refreshBackupStatus()
                _messages.emit(R.string.backup_export_success)
            } else {
                _messages.emit(R.string.backup_export_failed)
            }
        }
    }

    fun inspectBackupUri(context: Context, sourceUri: Uri) {
        scope.launch {
            _uiState.update { it.copy(isBackupInProgress = true) }
            val nextState: PendingRestoreState? = try {
                withContext(ioDispatcher) {
                    val rawContent = context.contentResolver.openInputStream(sourceUri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: return@withContext null

                    val metadata = BackupCrypto.peekMetadata(rawContent)
                        ?: return@withContext null

                    if (metadata.isEncrypted) {
                        PendingRestoreState.NeedsPassword(
                            rawContent = rawContent,
                            metadata = metadata,
                            isWrongPassword = false
                        )
                    } else {
                        val payload = BackupSerializer.fromJson(rawContent)
                        PendingRestoreState.ReadyToConfirm(payload)
                    }
                }
            } catch (_: Exception) {
                null
            } finally {
                _uiState.update { it.copy(isBackupInProgress = false) }
            }

            if (nextState != null) {
                _uiState.update { it.copy(pendingRestore = nextState) }
            } else {
                _messages.emit(R.string.backup_invalid_file)
            }
        }
    }

    fun submitRestorePassword(password: String) {
        val current = _uiState.value.pendingRestore as? PendingRestoreState.NeedsPassword ?: return
        if (password.isBlank()) {
            _uiState.update {
                it.copy(pendingRestore = current.copy(isWrongPassword = true))
            }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isBackupInProgress = true) }
            val decryptedPayload: BackupPayload? = try {
                withContext(ioDispatcher) {
                    val plainJson = BackupCrypto.decrypt(current.rawContent, password.toCharArray())
                    BackupSerializer.fromJson(plainJson)
                }
            } catch (_: Exception) {
                null
            } finally {
                _uiState.update { it.copy(isBackupInProgress = false) }
            }

            if (decryptedPayload != null) {
                _uiState.update {
                    it.copy(pendingRestore = PendingRestoreState.ReadyToConfirm(decryptedPayload))
                }
            } else {
                _uiState.update {
                    it.copy(pendingRestore = current.copy(isWrongPassword = true))
                }
            }
        }
    }

    fun confirmRestore(context: Context) {
        val current = _uiState.value.pendingRestore as? PendingRestoreState.ReadyToConfirm ?: return
        val repo = backupRepository ?: return

        scope.launch {
            _uiState.update { it.copy(isBackupInProgress = true) }
            val succeeded = try {
                withContext(ioDispatcher) {
                    repo.restoreBackup(context.applicationContext, current.payload.data)
                }
                true
            } catch (_: Exception) {
                false
            } finally {
                _uiState.update {
                    it.copy(
                        isBackupInProgress = false,
                        pendingRestore = null
                    )
                }
            }

            if (succeeded) {
                val restoredProfile = current.payload.data.profile
                val restoredName = restoredProfile?.name.orEmpty()
                val restoredDob = parseDateOfBirthOrNull(restoredProfile?.dateOfBirth)
                val restoredGender = UserGender.fromNameOrNull(restoredProfile?.gender)
                _uiState.update { state ->
                    state.copy(
                        name = restoredName,
                        dateOfBirth = restoredDob,
                        gender = restoredGender
                    ).apply {
                        this.loadedName = restoredName
                        this.loadedDateOfBirth = restoredDob
                        this.loadedGender = restoredGender
                    }
                }
                _messages.emit(R.string.backup_restore_success)
            } else {
                _messages.emit(R.string.backup_restore_failed)
            }
        }
    }

    fun dismissRestoreDialog() {
        _uiState.update { it.copy(pendingRestore = null) }
    }

    fun saveProfile(onSaved: () -> Unit = {}) {
        val current = _uiState.value
        if (!current.isInputValid) return

        scope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.saveProfile(
                name = current.name,
                dateOfBirth = current.dateOfBirth?.toString(),
                gender = current.gender
            )
            _uiState.update {
                it.copy(isSaving = false).apply {
                    this.loadedName = current.name.trim()
                    this.loadedDateOfBirth = current.dateOfBirth
                    this.loadedGender = current.gender
                }
            }
            _messages.emit(R.string.profile_saved_message)
            onSaved()
        }
    }

    companion object {
        fun factory(
            repository: UserProfileRepository,
            appLockManager: AppLockManager,
            authenticator: AppAuthenticator = AndroidBiometricAuthenticator,
            backupRepository: BackupRepository? = null,
            backupStorage: BackupStorage? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(
                    repository = repository,
                    appLockManager = appLockManager,
                    authenticator = authenticator,
                    backupRepository = backupRepository,
                    backupStorage = backupStorage
                ) as T
            }
        }
    }
}
