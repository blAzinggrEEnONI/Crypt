package com.example.crypt.domain.model

/**
 * Represents the current lock state of the application.
 */
sealed class AppLockState {
    object Unlocked : AppLockState()
    object Locked : AppLockState()
    data class AutoLocking(val remainingSeconds: Int) : AppLockState()
}