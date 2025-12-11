package com.example.furniturecloudy.data

import com.google.firebase.Timestamp

/**
 * OTP Code data model for Two-Factor Authentication
 *
 * Stored in Firestore collection: "otp_codes"
 * Document ID: userId
 */
data class OTPCode(
    val userId: String = "",
    val otp: String = "",
    val email: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now(),
    val verified: Boolean = false,
    val attempts: Int = 0,
    val maxAttempts: Int = 3,
    val lastAttemptAt: Timestamp? = null
) {
    /**
     * Check if OTP is expired
     */
    fun isExpired(): Boolean {
        return Timestamp.now().seconds > expiresAt.seconds
    }

    /**
     * Check if OTP is locked due to too many attempts
     */
    fun isLocked(): Boolean {
        return attempts >= maxAttempts
    }

    /**
     * Check if OTP is still valid
     */
    fun isValid(): Boolean {
        return !isExpired() && !isLocked() && !verified
    }

    /**
     * Get remaining attempts
     */
    fun getRemainingAttempts(): Int {
        return maxAttempts - attempts
    }

    /**
     * Get remaining time in seconds
     */
    fun getRemainingSeconds(): Long {
        val remaining = expiresAt.seconds - Timestamp.now().seconds
        return if (remaining > 0) remaining else 0
    }
}
