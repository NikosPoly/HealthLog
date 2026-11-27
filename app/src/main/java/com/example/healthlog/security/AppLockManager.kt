package com.example.healthlog.security

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AppLockStorage {
    fun isLockEnabled(): Boolean
    fun setLockEnabled(enabled: Boolean)
}

class SharedPreferencesAppLockStorage(
    private val prefs: SharedPreferences
) : AppLockStorage {
    override fun isLockEnabled(): Boolean =
        prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    override fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    }
}

/**
 * Process-scoped state machine managing device-local Biometric App Lock:
 * - Reads the persisted lock flag synchronously on cold launch so Frame 0 starts locked (zero flash).
 * - Survives configuration changes (e.g. screen rotation) without re-locking.
 * - Re-locks when returning from background after [lockTimeoutMs] (default 30 seconds).
 * - Ignores transient background transitions caused by the OS Biometric / Device Credential prompt.
 */
class AppLockManager(
    private val storage: AppLockStorage,
    private val lockTimeoutMs: Long = DEFAULT_LOCK_TIMEOUT_MS,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val _isLockEnabled = MutableStateFlow(storage.isLockEnabled())
    val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    // Cold start begins locked whenever App Lock is enabled.
    private val _isLocked = MutableStateFlow(_isLockEnabled.value)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundedAt: Long? = null
    private var isAuthenticationInProgress: Boolean = false

    fun setLockEnabled(enabled: Boolean) {
        storage.setLockEnabled(enabled)
        _isLockEnabled.value = enabled
        lastBackgroundedAt = null
        // Turning lock on or off happens inside an already-unlocked session.
        _isLocked.value = false
    }

    fun beginAuthentication() {
        isAuthenticationInProgress = true
    }

    fun endAuthentication(succeeded: Boolean) {
        isAuthenticationInProgress = false
        lastBackgroundedAt = null
        if (succeeded) {
            _isLocked.value = false
        }
    }

    fun onAppBackgrounded() {
        if (!_isLockEnabled.value || _isLocked.value || isAuthenticationInProgress) {
            return
        }
        if (lastBackgroundedAt == null) {
            lastBackgroundedAt = clock()
        }
    }

    fun onAppForegrounded() {
        if (!_isLockEnabled.value) {
            _isLocked.value = false
            lastBackgroundedAt = null
            return
        }
        if (isAuthenticationInProgress) {
            return
        }
        val backgroundedAt = lastBackgroundedAt
        lastBackgroundedAt = null
        if (backgroundedAt != null) {
            val elapsedMs = clock() - backgroundedAt
            if (elapsedMs >= lockTimeoutMs) {
                _isLocked.value = true
            }
        }
    }

    companion object {
        const val DEFAULT_LOCK_TIMEOUT_MS: Long = 30_000L

        @Volatile
        private var INSTANCE: AppLockManager? = null

        fun getInstance(context: Context): AppLockManager {
            return INSTANCE ?: synchronized(this) {
                val prefs = context.applicationContext.getSharedPreferences(
                    SharedPreferencesAppLockStorage.PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                val instance = AppLockManager(SharedPreferencesAppLockStorage(prefs))
                INSTANCE = instance
                instance
            }
        }
    }
}
