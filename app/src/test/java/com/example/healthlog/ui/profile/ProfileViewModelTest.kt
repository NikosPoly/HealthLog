package com.example.healthlog.ui.profile

import android.net.Uri
import com.example.healthlog.backup.BackupCrypto
import com.example.healthlog.backup.BackupData
import com.example.healthlog.backup.BackupMetadata
import com.example.healthlog.backup.BackupPayload
import com.example.healthlog.backup.BackupSchedule
import com.example.healthlog.backup.BackupSerializer
import com.example.healthlog.backup.FakeBackupStorage
import com.example.healthlog.database.Measurement
import com.example.healthlog.database.Medication
import com.example.healthlog.database.SymptomEntry
import com.example.healthlog.database.UserProfile
import com.example.healthlog.model.MeasurementType
import com.example.healthlog.model.UserGender
import com.example.healthlog.repository.FakeUserProfileDao
import com.example.healthlog.repository.UserProfileRepository
import com.example.healthlog.security.AppAuthenticator
import com.example.healthlog.security.AppLockManager
import com.example.healthlog.security.BiometricAvailability
import com.example.healthlog.security.FakeAppLockStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FakeAppAuthenticator(
    var availability: BiometricAvailability = BiometricAvailability.AVAILABLE
) : AppAuthenticator {
    override fun checkAvailability(context: android.content.Context): BiometricAvailability = availability
    override fun authenticate(
        activity: androidx.fragment.app.FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    ) {
        onSuccess()
    }
}

class ProfileViewModelTest {

    private lateinit var dao: FakeUserProfileDao
    private lateinit var repository: UserProfileRepository
    private lateinit var lockStorage: FakeAppLockStorage
    private lateinit var lockManager: AppLockManager
    private lateinit var authenticator: FakeAppAuthenticator
    private lateinit var fakeBackupStorage: FakeBackupStorage

    @Before
    fun setUp() {
        dao = FakeUserProfileDao()
        repository = UserProfileRepository(dao)
        lockStorage = FakeAppLockStorage(enabled = false)
        lockManager = AppLockManager(lockStorage)
        authenticator = FakeAppAuthenticator()
        fakeBackupStorage = FakeBackupStorage(
            initialSchedule = BackupSchedule.WEEKLY,
            initialLastBackup = 0L
        )
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(
            repository = repository,
            appLockManager = lockManager,
            authenticator = authenticator,
            backupRepository = null,
            backupStorage = fakeBackupStorage,
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun initialLoad_loadsExistingProfileAndLockState() = runBlocking {
        dao.upsert(
            UserProfile(
                name = "Nikos",
                dateOfBirth = "1990-05-15",
                gender = "MALE"
            )
        )
        lockManager.setLockEnabled(true)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals("Nikos", state.name)
        assertEquals(LocalDate.of(1990, 5, 15), state.dateOfBirth)
        assertEquals(UserGender.MALE, state.gender)
        assertTrue(state.isAppLockEnabled)
        assertFalse(state.hasChangesFromLoaded)
        assertTrue(state.isInputValid)
    }

    @Test
    fun onNameChange_updatesStateAndValidation() = runBlocking {
        val viewModel = createViewModel()

        viewModel.onNameChange("123invalid")
        assertFalse(viewModel.uiState.value.isInputValid)
        assertEquals(
            com.example.healthlog.model.UsernameValidationError.MustStartWithLetter,
            viewModel.uiState.value.usernameError
        )
        assertTrue(viewModel.uiState.value.hasChangesFromLoaded)

        viewModel.onNameChange("Nikos_99")
        assertTrue(viewModel.uiState.value.isInputValid)
        assertNull(viewModel.uiState.value.usernameError)
    }

    @Test
    fun onGenderChange_togglesSelection() = runBlocking {
        val viewModel = createViewModel()

        viewModel.onGenderChange(UserGender.MALE)
        assertEquals(UserGender.MALE, viewModel.uiState.value.gender)

        // Tapping MALE again clears it
        viewModel.onGenderChange(UserGender.MALE)
        assertNull(viewModel.uiState.value.gender)

        // Selecting FEMALE
        viewModel.onGenderChange(UserGender.FEMALE)
        assertEquals(UserGender.FEMALE, viewModel.uiState.value.gender)
    }

    @Test
    fun onAppLockToggled_updatesLockManagerAndState() = runBlocking {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isAppLockEnabled)
        assertFalse(lockManager.isLockEnabled.first())

        viewModel.onAppLockToggled(true)
        assertTrue(viewModel.uiState.value.isAppLockEnabled)
        assertTrue(lockManager.isLockEnabled.first())

        viewModel.onAppLockToggled(false)
        assertFalse(viewModel.uiState.value.isAppLockEnabled)
        assertFalse(lockManager.isLockEnabled.first())
    }

    @Test
    fun saveProfile_persistsDataAndEmitsMessage() = runBlocking {
        val viewModel = createViewModel()

        viewModel.onNameChange("Maria")
        viewModel.onDateOfBirthChange(LocalDate.of(1995, 3, 20))
        viewModel.onGenderChange(UserGender.FEMALE)

        var savedCalled = false
        viewModel.saveProfile { savedCalled = true }

        assertTrue(savedCalled)
        assertFalse(viewModel.uiState.value.hasChangesFromLoaded)

        val persisted = repository.observeProfile().first()
        assertEquals("Maria", persisted?.name)
        assertEquals("1995-03-20", persisted?.dateOfBirth)
        assertEquals("FEMALE", persisted?.gender)
    }

    // --- Milestone 4.2 Backup & Restore UI state tests ---

    @Test
    fun backupSettings_initializesFromStorage() = runBlocking {
        fakeBackupStorage.setSchedule(BackupSchedule.DAILY)
        fakeBackupStorage.setLastBackupTimestamp(123456789L)
        fakeBackupStorage.setAutoBackupPassword("secret123")

        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertEquals(BackupSchedule.DAILY, state.backupSchedule)
        assertEquals(123456789L, state.lastBackupTimestamp)
        assertTrue(state.isAutoBackupPasswordEnabled)
        assertNull(state.pendingRestore)
    }

    @Test
    fun onBackupScheduleSelected_updatesStorageAndState() = runBlocking {
        var updatedSchedule: BackupSchedule? = null

        val viewModel = ProfileViewModel(
            repository = repository,
            appLockManager = lockManager,
            authenticator = authenticator,
            backupStorage = fakeBackupStorage,
            scheduleUpdater = { _, sched ->
                updatedSchedule = sched
            },
            ioDispatcher = Dispatchers.Unconfined,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val dummyContext = android.content.ContextWrapper(null)

        viewModel.onBackupScheduleSelected(dummyContext, BackupSchedule.MONTHLY)

        assertEquals(BackupSchedule.MONTHLY, fakeBackupStorage.schedule.value)
        assertEquals(BackupSchedule.MONTHLY, viewModel.uiState.value.backupSchedule)
        assertEquals(BackupSchedule.MONTHLY, updatedSchedule)
    }

    @Test
    fun onCustomFolderSelected_updatesStorage() = runBlocking {
        val viewModel = createViewModel()
        val dummyUri = Uri.parse("content://dummy/tree")

        viewModel.onCustomFolderSelected(dummyUri)
        assertEquals(dummyUri, fakeBackupStorage.customFolderUri.value)
        assertEquals(dummyUri, viewModel.uiState.value.customBackupFolderUri)

        viewModel.onCustomFolderSelected(null)
        assertNull(fakeBackupStorage.customFolderUri.value)
        assertNull(viewModel.uiState.value.customBackupFolderUri)
    }

    @Test
    fun onAutoBackupPasswordChanged_updatesStorage() = runBlocking {
        val viewModel = createViewModel()

        viewModel.onAutoBackupPasswordChanged("newPassword123")
        assertEquals("newPassword123", fakeBackupStorage.getAutoBackupPassword())
        assertTrue(viewModel.uiState.value.isAutoBackupPasswordEnabled)

        viewModel.onAutoBackupPasswordChanged("")
        assertNull(fakeBackupStorage.getAutoBackupPassword())
        assertFalse(viewModel.uiState.value.isAutoBackupPasswordEnabled)
    }

    @Test
    fun submitRestorePassword_withCorrectPassword_transitionsToReadyToConfirm() = runBlocking {
        val viewModel = createViewModel()

        val sampleData = BackupData(
            profile = UserProfile(name = "RestoredUser"),
            measurements = listOf(Measurement(id = 1L, type = MeasurementType.PULSE, value = 72.0))
        )
        val samplePayload = BackupPayload(
            metadata = BackupMetadata(measurementsCount = 1, isEncrypted = true),
            data = sampleData
        )
        val plainJson = BackupSerializer.toJson(samplePayload)
        val password = "StrongPassword456"
        val encryptedContent = BackupCrypto.encrypt(plainJson, password.toCharArray(), samplePayload.metadata)

        // Manually place in NeedsPassword state as if inspected from a file
        val metadata = BackupCrypto.peekMetadata(encryptedContent)!!
        val needsPasswordState = PendingRestoreState.NeedsPassword(
            rawContent = encryptedContent,
            metadata = metadata
        )

        // Submit wrong password first
        val field = ProfileViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<ProfileUiState>
        stateFlow.value = stateFlow.value.copy(pendingRestore = needsPasswordState)

        viewModel.submitRestorePassword("WrongPassword")
        val afterWrongState = viewModel.uiState.value.pendingRestore
        assertTrue(afterWrongState is PendingRestoreState.NeedsPassword)
        assertTrue((afterWrongState as PendingRestoreState.NeedsPassword).isWrongPassword)

        // Submit correct password
        viewModel.submitRestorePassword(password)
        val afterCorrectState = viewModel.uiState.value.pendingRestore
        assertTrue(afterCorrectState is PendingRestoreState.ReadyToConfirm)
        val ready = afterCorrectState as PendingRestoreState.ReadyToConfirm
        assertEquals("RestoredUser", ready.payload.data.profile?.name)
        assertEquals(1, ready.payload.data.measurements.size)
        assertEquals(72.0, ready.payload.data.measurements[0].value, 0.0)

        // Dismiss dialog
        viewModel.dismissRestoreDialog()
        assertNull(viewModel.uiState.value.pendingRestore)
    }
}
