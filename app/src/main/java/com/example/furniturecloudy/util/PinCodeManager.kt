package com.example.furniturecloudy.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages local PIN code for app authentication
 * Uses Android Keystore for secure encryption
 */
class PinCodeManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "pin_code_prefs"
        private const val KEY_PIN_HASH = "encrypted_pin"
        private const val KEY_PIN_IV = "pin_iv"
        private const val KEY_PIN_SET = "is_pin_set"
        private const val KEYSTORE_ALIAS = "HomeHarmonyPinKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        const val PIN_LENGTH = 4
        const val MAX_ATTEMPTS = 5
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_TIME = "lockout_time"
        private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if PIN has been set up
     */
    fun isPinSet(): Boolean {
        return prefs.getBoolean(KEY_PIN_SET, false)
    }

    /**
     * Set up a new PIN
     * @param pin The PIN to set (should be 4-6 digits)
     * @return true if PIN was set successfully
     */
    fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val encryptedPin = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv

            prefs.edit()
                .putString(KEY_PIN_HASH, Base64.encodeToString(encryptedPin, Base64.NO_WRAP))
                .putString(KEY_PIN_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .putBoolean(KEY_PIN_SET, true)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .apply()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Verify the entered PIN
     * @param pin The PIN to verify
     * @return PinVerificationResult indicating success, failure, or lockout
     */
    fun verifyPin(pin: String): PinVerificationResult {
        if (isLockedOut()) {
            val remainingTime = getRemainingLockoutTime()
            return PinVerificationResult.LockedOut(remainingTime)
        }

        if (!isPinSet()) {
            return PinVerificationResult.NoPinSet
        }

        return try {
            val encryptedPinBase64 = prefs.getString(KEY_PIN_HASH, null)
            val ivBase64 = prefs.getString(KEY_PIN_IV, null)

            if (encryptedPinBase64 == null || ivBase64 == null) {
                return PinVerificationResult.Error("PIN data corrupted")
            }

            val encryptedPin = Base64.decode(encryptedPinBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedPin = String(cipher.doFinal(encryptedPin), Charsets.UTF_8)

            if (pin == decryptedPin) {
                resetFailedAttempts()
                PinVerificationResult.Success
            } else {
                incrementFailedAttempts()
                val remainingAttempts = MAX_ATTEMPTS - getFailedAttempts()
                if (remainingAttempts <= 0) {
                    startLockout()
                    PinVerificationResult.LockedOut(LOCKOUT_DURATION_MS)
                } else {
                    PinVerificationResult.WrongPin(remainingAttempts)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PinVerificationResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Remove the PIN
     */
    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_IV)
            .putBoolean(KEY_PIN_SET, false)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_LOCKOUT_TIME)
            .apply()
    }

    /**
     * Validate PIN format
     */
    fun isValidPin(pin: String): Boolean {
        return pin.length == PIN_LENGTH && pin.all { it.isDigit() }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        keyStore.getKey(KEYSTORE_ALIAS, null)?.let {
            return it as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun getFailedAttempts(): Int {
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    private fun incrementFailedAttempts() {
        val current = getFailedAttempts()
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, current + 1).apply()
    }

    private fun resetFailedAttempts() {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply()
    }

    private fun isLockedOut(): Boolean {
        val lockoutTime = prefs.getLong(KEY_LOCKOUT_TIME, 0)
        if (lockoutTime == 0L) return false

        return System.currentTimeMillis() < lockoutTime + LOCKOUT_DURATION_MS
    }

    private fun getRemainingLockoutTime(): Long {
        val lockoutTime = prefs.getLong(KEY_LOCKOUT_TIME, 0)
        val remaining = (lockoutTime + LOCKOUT_DURATION_MS) - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }

    private fun startLockout() {
        prefs.edit()
            .putLong(KEY_LOCKOUT_TIME, System.currentTimeMillis())
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .apply()
    }

    /**
     * Result of PIN verification
     */
    sealed class PinVerificationResult {
        object Success : PinVerificationResult()
        object NoPinSet : PinVerificationResult()
        data class WrongPin(val remainingAttempts: Int) : PinVerificationResult()
        data class LockedOut(val remainingTimeMs: Long) : PinVerificationResult()
        data class Error(val message: String) : PinVerificationResult()
    }
}