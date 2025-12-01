package com.example.furniturecloudy.present

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.furniturecloudy.databinding.ActivityLockScreenBinding
import com.example.furniturecloudy.util.AppAuthManager
import com.example.furniturecloudy.util.PinCodeDialog
import com.example.furniturecloudy.util.PinCodeManager
import com.example.furniturecloudy.util.SessionManager
import com.google.firebase.auth.FirebaseAuth

/**
 * LockScreenActivity - Màn hình khóa khi session timeout
 *
 * Tính năng bảo mật:
 * - Hiển thị khi session hết hạn hoặc app locked
 * - Hỗ trợ nhiều phương thức xác thực (Biometric, PIN thiết bị, PIN app)
 * - Không cho phép back để bypass
 * - Option đăng xuất nếu quên xác thực
 */
class LockScreenActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REASON = "lock_reason"
        const val REASON_SESSION_TIMEOUT = "session_timeout"
        const val REASON_APP_LAUNCH = "app_launch"
        const val REASON_MANUAL_LOCK = "manual_lock"

        // Biometric authenticators
        private const val AUTHENTICATORS_BIOMETRIC =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK

        private const val AUTHENTICATORS_DEVICE_CREDENTIAL =
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }

    private lateinit var binding: ActivityLockScreenBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var appAuthManager: AppAuthManager
    private lateinit var pinCodeManager: PinCodeManager
    private lateinit var firebaseAuth: FirebaseAuth

    private var lockReason: String = REASON_SESSION_TIMEOUT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize managers
        sessionManager = SessionManager.getInstance(this)
        appAuthManager = AppAuthManager(this)
        pinCodeManager = PinCodeManager(this)
        firebaseAuth = FirebaseAuth.getInstance()

        lockReason = intent.getStringExtra(EXTRA_REASON) ?: REASON_SESSION_TIMEOUT

        setupUI()
        setupClickListeners()

        // Tự động hiển thị prompt xác thực
        showAuthenticationPrompt()
    }

    private fun setupUI() {
        // Hiển thị lý do khóa
        val reasonText = when (lockReason) {
            REASON_SESSION_TIMEOUT -> "Phiên làm việc đã hết hạn\nVui lòng xác thực lại"
            REASON_APP_LAUNCH -> "Xác thực để tiếp tục"
            REASON_MANUAL_LOCK -> "Ứng dụng đã bị khóa"
            else -> "Vui lòng xác thực"
        }
        binding.tvLockReason.text = reasonText

        // Hiển thị email user (nếu có)
        firebaseAuth.currentUser?.email?.let { email ->
            binding.tvUserEmail.text = email
        }

        // Cập nhật text button theo phương thức xác thực
        updateAuthButtonText()
    }

    private fun updateAuthButtonText() {
        val buttonText = when (appAuthManager.getAuthMethod()) {
            AppAuthManager.AuthMethod.BIOMETRIC -> "Xác thực bằng vân tay"
            AppAuthManager.AuthMethod.DEVICE_CREDENTIAL -> "Xác thực bằng PIN thiết bị"
            AppAuthManager.AuthMethod.APP_PIN -> "Nhập PIN ứng dụng"
        }
        binding.btnAuthenticate.text = buttonText
    }

    private fun setupClickListeners() {
        binding.btnAuthenticate.setOnClickListener {
            showAuthenticationPrompt()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showAuthenticationPrompt() {
        when (appAuthManager.getAuthMethod()) {
            AppAuthManager.AuthMethod.BIOMETRIC -> {
                showBiometricPrompt()
            }
            AppAuthManager.AuthMethod.DEVICE_CREDENTIAL -> {
                showDeviceCredentialPrompt()
            }
            AppAuthManager.AuthMethod.APP_PIN -> {
                showAppPinDialog()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(
                            this@LockScreenActivity,
                            "Lỗi xác thực: $errString",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        this@LockScreenActivity,
                        "Xác thực thất bại",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực")
            .setSubtitle("Sử dụng vân tay hoặc khuôn mặt")
            .setNegativeButtonText("Hủy")
            .setAllowedAuthenticators(AUTHENTICATORS_BIOMETRIC)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showDeviceCredentialPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(
                            this@LockScreenActivity,
                            "Lỗi xác thực: $errString",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        this@LockScreenActivity,
                        "Xác thực thất bại",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        val promptInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Xác thực")
                .setSubtitle("Sử dụng PIN/Pattern/Password của thiết bị")
                .setAllowedAuthenticators(AUTHENTICATORS_DEVICE_CREDENTIAL)
                .build()
        } else {
            @Suppress("DEPRECATION")
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Xác thực")
                .setSubtitle("Sử dụng PIN/Pattern/Password của thiết bị")
                .setDeviceCredentialAllowed(true)
                .build()
        }

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showAppPinDialog() {
        if (!pinCodeManager.isPinSet()) {
            Toast.makeText(this, "PIN chưa được thiết lập", Toast.LENGTH_SHORT).show()
            return
        }

        PinCodeDialog(
            context = this,
            mode = PinCodeDialog.Mode.VERIFY,
            pinCodeManager = pinCodeManager,
            onSuccess = {
                onAuthSuccess()
            },
            onCancel = {
                // Do nothing, user can try again
            }
        ).show()
    }

    private fun onAuthSuccess() {
        sessionManager.unlockSession()
        Toast.makeText(this, "Xác thực thành công", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showLogoutConfirmation() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất? Mọi dữ liệu chưa lưu sẽ bị mất.")
            .setPositiveButton("Đăng xuất") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun performLogout() {
        // Clear session
        sessionManager.clearSession()
        appAuthManager.clearSettings()

        // Logout Firebase
        firebaseAuth.signOut()

        // Navigate to login
        val intent = Intent(this, LoginRegisterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * Không cho phép back để bypass màn hình khóa
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Không làm gì - không cho phép back
        Toast.makeText(this, "Vui lòng xác thực để tiếp tục", Toast.LENGTH_SHORT).show()
    }
}