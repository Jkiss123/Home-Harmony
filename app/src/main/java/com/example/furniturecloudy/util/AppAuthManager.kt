package com.example.furniturecloudy.util

import android.content.Context

/**
 * Manages app authentication settings
 * Stores user preferences for authentication on app launch
 */
class AppAuthManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "app_auth_prefs"
        private const val KEY_AUTH_ENABLED = "auth_enabled"
        private const val KEY_AUTH_METHOD = "auth_method"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Authentication methods available
     */
    enum class AuthMethod {
        BIOMETRIC,          // Vân tay / Khuôn mặt
        DEVICE_CREDENTIAL,  // PIN / Pattern của thiết bị
        APP_PIN             // PIN riêng của app
    }

    /**
     * Check if authentication is enabled
     */
    fun isAuthEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTH_ENABLED, false)
    }

    /**
     * Enable or disable authentication
     */
    fun setAuthEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTH_ENABLED, enabled).apply()
    }

    /**
     * Get selected authentication method
     */
    fun getAuthMethod(): AuthMethod {
        val methodName = prefs.getString(KEY_AUTH_METHOD, AuthMethod.BIOMETRIC.name)
        return try {
            AuthMethod.valueOf(methodName ?: AuthMethod.BIOMETRIC.name)
        } catch (e: Exception) {
            AuthMethod.BIOMETRIC
        }
    }

    /**
     * Set authentication method
     */
    fun setAuthMethod(method: AuthMethod) {
        prefs.edit().putString(KEY_AUTH_METHOD, method.name).apply()
    }

    /**
     * Clear all auth settings (on logout)
     */
    fun clearSettings() {
        prefs.edit()
            .putBoolean(KEY_AUTH_ENABLED, false)
            .remove(KEY_AUTH_METHOD)
            .apply()
    }
}