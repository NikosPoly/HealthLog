package com.example.healthlog.security

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAppLockStorage(
    private var enabled: Boolean = false
) : AppLockStorage {
    override fun isLockEnabled(): Boolean = enabled
    override fun setLockEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}

class AppLockManagerTest {

    private var currentTimeMs: Long = 100_000L

    private fun createManager(
        initialEnabled: Boolean,
        timeoutMs: Long = 30_000L
    ): Pair<AppLockManager, FakeAppLockStorage> {
        val storage = FakeAppLockStorage(initialEnabled)
        val manager = AppLockManager(
            storage = storage,
            lockTimeoutMs = timeoutMs,
            clock = { currentTimeMs }
        )
        return Pair(manager, storage)
    }

    @Test
    fun coldStart_whenLockDisabled_isUnlocked() = runBlocking {
        val (manager, _) = createManager(initialEnabled = false)
        assertFalse(manager.isLockEnabled.first())
        assertFalse(manager.isLocked.first())
    }

    @Test
    fun coldStart_whenLockEnabled_isLocked() = runBlocking {
        val (manager, _) = createManager(initialEnabled = true)
        assertTrue(manager.isLockEnabled.first())
        assertTrue(manager.isLocked.first())
    }

    @Test
    fun setLockEnabled_persistsAndUpdatesState() = runBlocking {
        val (manager, storage) = createManager(initialEnabled = false)
        assertFalse(storage.isLockEnabled())

        manager.setLockEnabled(true)
        assertTrue(storage.isLockEnabled())
        assertTrue(manager.isLockEnabled.first())
        assertFalse(manager.isLocked.first())

        manager.setLockEnabled(false)
        assertFalse(storage.isLockEnabled())
        assertFalse(manager.isLockEnabled.first())
        assertFalse(manager.isLocked.first())
    }

    @Test
    fun successfulAuthentication_unlocksApp() = runBlocking {
        val (manager, _) = createManager(initialEnabled = true)
        assertTrue(manager.isLocked.first())

        manager.beginAuthentication()
        manager.endAuthentication(succeeded = true)
        assertFalse(manager.isLocked.first())
    }

    @Test
    fun failedAuthentication_keepsAppLocked() = runBlocking {
        val (manager, _) = createManager(initialEnabled = true)
        assertTrue(manager.isLocked.first())

        manager.beginAuthentication()
        manager.endAuthentication(succeeded = false)
        assertTrue(manager.isLocked.first())
    }

    @Test
    fun backgroundResume_withinTimeout_doesNotLock() = runBlocking {
        val (manager, _) = createManager(initialEnabled = true, timeoutMs = 30_000L)
        manager.beginAuthentication()
        manager.endAuthentication(succeeded = true)
        assertFalse(manager.isLocked.first())

        // App goes to background
        manager.onAppBackgrounded()

        // 10 seconds pass (less than 30s)
        currentTimeMs += 10_000L
        manager.onAppForegrounded()

        assertFalse(manager.isLocked.first())
    }

    @Test
    fun backgroundResume_afterTimeout_locksApp() = runBlocking {
        val (manager, _) = createManager(initialEnabled = true, timeoutMs = 30_000L)
        manager.beginAuthentication()
        manager.endAuthentication(succeeded = true)
        assertFalse(manager.isLocked.first())

        // App goes to background
        manager.onAppBackgrounded()

        // 35 seconds pass (exceeds 30s timeout)
        currentTimeMs += 35_000L
        manager.onAppForegrounded()

        assertTrue(manager.isLocked.first())
    }

    @Test
    fun backgroundTransition_duringAuthentication_isIgnored() = runBlocking {
        val (manager, _) = createManager(initialEnabled = true, timeoutMs = 30_000L)
        manager.beginAuthentication()
        manager.endAuthentication(succeeded = true)
        assertFalse(manager.isLocked.first())

        // OS biometric prompt pops up: triggers beginAuthentication
        manager.beginAuthentication()

        // System prompt covers activity, triggering onAppBackgrounded
        manager.onAppBackgrounded()

        // 45 seconds pass
        currentTimeMs += 45_000L

        // Authentication succeeds
        manager.endAuthentication(succeeded = true)
        manager.onAppForegrounded()

        // Should NOT be locked
        assertFalse(manager.isLocked.first())
    }
}
