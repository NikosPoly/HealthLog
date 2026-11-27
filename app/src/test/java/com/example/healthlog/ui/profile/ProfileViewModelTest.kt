package com.example.healthlog.ui.profile

import com.example.healthlog.database.UserProfile
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

    @Before
    fun setUp() {
        dao = FakeUserProfileDao()
        repository = UserProfileRepository(dao)
        lockStorage = FakeAppLockStorage(enabled = false)
        lockManager = AppLockManager(lockStorage)
        authenticator = FakeAppAuthenticator()
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(
            repository = repository,
            appLockManager = lockManager,
            authenticator = authenticator,
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
}
