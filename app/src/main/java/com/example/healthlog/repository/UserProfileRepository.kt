package com.example.healthlog.repository

import com.example.healthlog.database.UserProfile
import com.example.healthlog.database.UserProfileDao
import com.example.healthlog.model.UserGender
import com.example.healthlog.model.parseDateOfBirthOrNull
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(
    private val dao: UserProfileDao
) {
    fun observeProfile(): Flow<UserProfile?> = dao.observeProfile()

    suspend fun getProfileOnce(): UserProfile? = dao.getProfileOnce()

    suspend fun saveProfile(
        name: String?,
        dateOfBirth: String?,
        gender: UserGender?,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        val normalizedName = name?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedDob = parseDateOfBirthOrNull(dateOfBirth)?.toString()
        val normalizedGender = gender?.name

        if (normalizedName == null && normalizedDob == null && normalizedGender == null) {
            dao.clear()
            return
        }

        dao.upsert(
            UserProfile(
                id = UserProfile.SINGLETON_ID,
                name = normalizedName,
                dateOfBirth = normalizedDob,
                gender = normalizedGender,
                updatedAt = updatedAt
            )
        )
    }

    suspend fun clearProfile() {
        dao.clear()
    }
}
