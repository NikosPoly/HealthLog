package com.example.healthlog.repository

import com.example.healthlog.database.UserProfile
import com.example.healthlog.database.UserProfileDao
import com.example.healthlog.model.UserGender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FakeUserProfileDao : UserProfileDao {
    private val profileFlow = MutableStateFlow<UserProfile?>(null)

    override fun observeProfile(): Flow<UserProfile?> = profileFlow

    override suspend fun getProfileOnce(): UserProfile? = profileFlow.value

    override suspend fun upsert(profile: UserProfile) {
        profileFlow.value = profile
    }

    override suspend fun clear() {
        profileFlow.value = null
    }
}

class UserProfileRepositoryTest {

    @Test
    fun saveProfile_normalizesAndUpserts() = runBlocking {
        val dao = FakeUserProfileDao()
        val repo = UserProfileRepository(dao)

        assertNull(repo.observeProfile().first())

        repo.saveProfile(
            name = "  Nikos  ",
            dateOfBirth = " 1990-05-15 ",
            gender = UserGender.MALE,
            updatedAt = 5000L
        )

        val saved = repo.observeProfile().first()
        assertNotNull(saved)
        assertEquals("Nikos", saved?.name)
        assertEquals("1990-05-15", saved?.dateOfBirth)
        assertEquals("MALE", saved?.gender)
        assertEquals(5000L, saved?.updatedAt)
    }

    @Test
    fun saveProfile_whenAllFieldsBlank_clearsProfile() = runBlocking {
        val dao = FakeUserProfileDao()
        val repo = UserProfileRepository(dao)

        repo.saveProfile(
            name = "Nikos",
            dateOfBirth = "1990-05-15",
            gender = UserGender.MALE
        )
        assertNotNull(repo.observeProfile().first())

        // Save with all blanks/nulls
        repo.saveProfile(
            name = "   ",
            dateOfBirth = null,
            gender = null
        )

        assertNull(repo.observeProfile().first())
    }

    @Test
    fun clearProfile_deletesRow() = runBlocking {
        val dao = FakeUserProfileDao()
        val repo = UserProfileRepository(dao)

        repo.saveProfile(
            name = "Nikos",
            dateOfBirth = "1990-05-15",
            gender = UserGender.MALE
        )
        assertNotNull(repo.observeProfile().first())

        repo.clearProfile()
        assertNull(repo.observeProfile().first())
    }
}
