package com.example.furniturecloudy.util

/**
 * OTP Configuration Constants
 *
 * Two-Factor Authentication settings
 */
object OTPConfig {
    /**
     * DEBUG MODE - Set to true to bypass OTP verification during testing
     * ⚠️ MUST BE FALSE IN PRODUCTION!
     *
     * When true:
     * - OTP generation still works
     * - Email is still sent (if configured)
     * - But ANY 6-digit code will be accepted as valid
     */
    const val DEBUG_BYPASS_OTP = true  // TODO: Set to false before production release!

    /**
     * When DEBUG mode is enabled, this OTP always works
     * Use "123456" for quick testing
     */
    const val DEBUG_OTP_CODE = "123456"

    // OTP Settings
    const val OTP_LENGTH = 6
    const val OTP_EXPIRY_MINUTES = 5
    const val OTP_MAX_ATTEMPTS = 3
    const val RESEND_COOLDOWN_SECONDS = 60

    // 2FA Settings
    const val TWO_FACTOR_ENABLED_BY_DEFAULT = true  // Force 2FA for all users

    // Email Settings
    const val EMAIL_SERVICE_TYPE = "EMAILJS"  // or "FIREBASE_EXTENSION", "CUSTOM"

    // EmailJS Configuration (Get from https://www.emailjs.com/)
    const val EMAILJS_SERVICE_ID = "service_m1pcnmi"
    const val EMAILJS_TEMPLATE_ID = "template_j6qxk8f"
    const val EMAILJS_USER_ID = "AO8a042V7FEzfxa-K"

    /**
     * Get debug status message for logs
     */
    fun getDebugStatus(): String {
        return if (DEBUG_BYPASS_OTP) {
            "⚠️ OTP DEBUG MODE ENABLED - Any 6-digit code will be accepted!"
        } else {
            "🔒 OTP Protection Active - Real verification required"
        }
    }
}
