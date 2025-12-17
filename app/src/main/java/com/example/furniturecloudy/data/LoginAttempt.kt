package com.example.furniturecloudy.data

import com.google.firebase.Timestamp

/**
 * Data class for tracking failed login attempts
 * Used to implement account lockout after multiple failed attempts
 */
data class LoginAttempt(
    val email: String = "",
    val failedCount: Int = 0,
    val lastFailedAt: Timestamp = Timestamp.now(),
    val lockedUntil: Timestamp? = null,
    val isLocked: Boolean = false
) {
    companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 15L
    }

    /**
     * Check if account is currently locked
     */
    fun isCurrentlyLocked(): Boolean {
        if (!isLocked || lockedUntil == null) return false
        return Timestamp.now().seconds < lockedUntil.seconds
    }

    /**
     * Get remaining lockout time in seconds
     */
    fun getRemainingLockoutSeconds(): Long {
        if (!isCurrentlyLocked()) return 0
        return (lockedUntil!!.seconds - Timestamp.now().seconds).coerceAtLeast(0)
    }
}