package com.example.healthlog.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthlog.model.UserGender
import com.example.healthlog.model.calculateAgeYears
import com.example.healthlog.model.parseDateOfBirthOrNull
import com.example.healthlog.repository.UserProfileRepository
import com.example.healthlog.security.AndroidBiometricAuthenticator
import com.example.healthlog.security.AppAuthenticator
import com.example.healthlog.security.AppLockManager
import com.example.healthlog.security.BiometricAvailability
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProfileUiState(
    val name: String = "",
    val dateOfBirth: LocalDate? = null,
    val gender: UserGender? = null,
    val isAppLockEnabled: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.AVAILABLE,
    val isSaving: Boolean = false
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
    private val authenticator: AppAuthenticator = AndroidBiometricAuthenticator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<Int>()
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    private var isInitialLoaded = false

    init {
        viewModelScope.launch {
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
    }

    fun refreshBiometricAvailability(context: Context) {
        val availability = authenticator.checkAvailability(context)
        _uiState.update { it.copy(biometricAvailability = availability) }
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

    fun saveProfile(onSaved: () -> Unit = {}) {
        val current = _uiState.value
        if (!current.isInputValid) return

        viewModelScope.launch {
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
            _messages.emit(com.example.healthlog.R.string.profile_saved_message)
            onSaved()
        }
    }

    companion object {
        fun factory(
            repository: UserProfileRepository,
            appLockManager: AppLockManager,
            authenticator: AppAuthenticator = AndroidBiometricAuthenticator
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(repository, appLockManager, authenticator) as T
            }
        }
    }
}
