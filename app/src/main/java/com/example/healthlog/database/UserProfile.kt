package com.example.healthlog.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.healthlog.model.UserGender
import com.example.healthlog.model.parseDateOfBirthOrNull

@Entity(tableName = "user_profile")
data class UserProfile(
    // Single-row personal profile table; always uses SINGLETON_ID = 1L.
    @PrimaryKey
    val id: Long = SINGLETON_ID,

    // Optional display name used on the Profile screen and exported PDF doctor reports.
    val name: String? = null,

    // Optional ISO-8601 date string ("YYYY-MM-DD"), e.g. "1985-04-12".
    val dateOfBirth: String? = null,

    // Optional UserGender enum name ("MALE" or "FEMALE").
    val gender: String? = null,

    // Epoch milliseconds when the profile was last updated.
    val updatedAt: Long = System.currentTimeMillis()
) {
    val hasAnyDetails: Boolean
        get() = !name.isNullOrBlank() ||
            parseDateOfBirthOrNull(dateOfBirth) != null ||
            UserGender.fromNameOrNull(gender) != null

    companion object {
        const val SINGLETON_ID: Long = 1L
    }
}
