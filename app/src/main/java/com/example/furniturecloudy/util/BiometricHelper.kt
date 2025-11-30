package com.example.furniturecloudy.util

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Helper class for Biometric Authentication
 * Handles fingerprint, face recognition, and device credential (PIN/Pattern/Password) authentication
 */
class BiometricHelper(private val fragment: Fragment) {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    companion object {
        // Biometric only (fingerprint, face)
        private const val AUTHENTICATORS_BIOMETRIC =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

        // Biometric + Device Credential (PIN/Pattern/Password)
        private const val AUTHENTICATORS_ALL =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        // Device Credential only (PIN/Pattern/Password)
        private const val AUTHENTICATORS_DEVICE_CREDENTIAL =
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }

    /**
     * Check if biometric authentication is available on the device
     */
    fun isBiometricAvailable(): BiometricStatus {
        val biometricManager = BiometricManager.from(fragment.requireContext())

        return when (biometricManager.canAuthenticate(AUTHENTICATORS_BIOMETRIC)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.UNKNOWN_ERROR
        }
    }

    /**
     * Check if device credential (PIN/Pattern/Password) is available
     */
    fun isDeviceCredentialAvailable(): Boolean {
        val biometricManager = BiometricManager.from(fragment.requireContext())
        return biometricManager.canAuthenticate(AUTHENTICATORS_DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Check if any authentication method is available (biometric OR device credential)
     */
    fun isAnyAuthenticationAvailable(): AuthenticationAvailability {
        val biometricStatus = isBiometricAvailable()
        val deviceCredentialAvailable = isDeviceCredentialAvailable()

        return when {
            biometricStatus == BiometricStatus.AVAILABLE -> AuthenticationAvailability.BIOMETRIC_AVAILABLE
            deviceCredentialAvailable -> AuthenticationAvailability.DEVICE_CREDENTIAL_AVAILABLE
            else -> AuthenticationAvailability.NONE_AVAILABLE
        }
    }

    /**
     * Show biometric authentication prompt (biometric only)
     */
    fun showBiometricPrompt(
        title: String = "Xác thực sinh trắc học",
        subtitle: String = "Sử dụng vân tay hoặc khuôn mặt để xác thực",
        negativeButtonText: String = "Hủy",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errorMessage: String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())

        biometricPrompt = BiometricPrompt(fragment, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(AUTHENTICATORS_BIOMETRIC)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Show authentication prompt with fallback to device credential (PIN/Pattern/Password)
     * This allows users to use biometric OR PIN/Pattern/Password
     */
    fun showBiometricOrCredentialPrompt(
        title: String = "Xác thực để tiếp tục",
        subtitle: String = "Sử dụng vân tay, khuôn mặt hoặc mã PIN/Pattern",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errorMessage: String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())

        biometricPrompt = BiometricPrompt(fragment, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            })

        // API 30+ supports combining biometric with device credential
        // Below API 30, we need different handling
        promptInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(AUTHENTICATORS_ALL)
                .build()
        } else {
            // For API < 30, use device credential as fallback
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDeviceCredentialAllowed(true)
                .build()
        }

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Show device credential only prompt (PIN/Pattern/Password)
     */
    fun showDeviceCredentialPrompt(
        title: String = "Nhập mã PIN/Pattern",
        subtitle: String = "Xác thực bằng mã khóa màn hình của bạn",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errorMessage: String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())

        biometricPrompt = BiometricPrompt(fragment, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            })

        promptInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(AUTHENTICATORS_DEVICE_CREDENTIAL)
                .build()
        } else {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDeviceCredentialAllowed(true)
                .build()
        }

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Biometric availability status
     */
    enum class BiometricStatus {
        AVAILABLE,           // Biometric is available and enrolled
        NO_HARDWARE,         // Device doesn't have biometric hardware
        HARDWARE_UNAVAILABLE,// Hardware is currently unavailable
        NOT_ENROLLED,        // No biometric is enrolled (no fingerprint registered)
        UNKNOWN_ERROR        // Unknown error
    }

    /**
     * Overall authentication availability
     */
    enum class AuthenticationAvailability {
        BIOMETRIC_AVAILABLE,         // Biometric (fingerprint/face) is available
        DEVICE_CREDENTIAL_AVAILABLE, // Only PIN/Pattern/Password is available
        NONE_AVAILABLE               // No authentication method available
    }
}
