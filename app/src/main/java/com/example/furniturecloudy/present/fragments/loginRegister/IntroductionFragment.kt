package com.example.furniturecloudy.present.fragments.loginRegister

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentIntroductionBinding
import com.example.furniturecloudy.model.viewmodel.IntroductionViewmodel
import com.example.furniturecloudy.model.viewmodel.IntroductionViewmodel.Companion.ACCOUNT_OPTION_FRAGMENT
import com.example.furniturecloudy.model.viewmodel.IntroductionViewmodel.Companion.SHOPPING_ACTIVITY
import com.example.furniturecloudy.present.ShoppingActivity
import com.example.furniturecloudy.util.AppAuthManager
import com.example.furniturecloudy.util.BiometricHelper
import com.example.furniturecloudy.util.PinCodeDialog
import com.example.furniturecloudy.util.PinCodeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IntroductionFragment : Fragment() {
    private lateinit var binding: FragmentIntroductionBinding
    private val viewmodel: IntroductionViewmodel by viewModels()
    private lateinit var appAuthManager: AppAuthManager
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var pinCodeManager: PinCodeManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentIntroductionBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appAuthManager = AppAuthManager(requireContext())
        biometricHelper = BiometricHelper(this)
        pinCodeManager = PinCodeManager(requireContext())

        binding.btnIntroducStart.setOnClickListener {
            findNavController().navigate(R.id.action_introductionFragment_to_accountOptionFragment)
            viewmodel.startButtonClicked()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.navigate.collect {
                    when (it) {
                        SHOPPING_ACTIVITY -> {
                            handleAuthenticationFlow()
                        }
                        ACCOUNT_OPTION_FRAGMENT -> {
                            findNavController().navigate(R.id.action_introductionFragment_to_accountOptionFragment)
                        }
                    }
                }
            }
        }
    }

    /**
     * Main authentication flow based on user settings
     */
    private fun handleAuthenticationFlow() {
        // Check if authentication is enabled in settings
        if (!appAuthManager.isAuthEnabled()) {
            // Auth not enabled -> go directly to shopping
            navigateToShopping()
            return
        }

        // Auth enabled -> use selected method
        when (appAuthManager.getAuthMethod()) {
            AppAuthManager.AuthMethod.BIOMETRIC -> {
                authenticateWithBiometric()
            }
            AppAuthManager.AuthMethod.DEVICE_CREDENTIAL -> {
                authenticateWithDeviceCredential()
            }
            AppAuthManager.AuthMethod.APP_PIN -> {
                authenticateWithAppPin()
            }
        }
    }

    /**
     * Authenticate using biometric (fingerprint/face)
     */
    private fun authenticateWithBiometric() {
        if (biometricHelper.isBiometricAvailable() != BiometricHelper.BiometricStatus.AVAILABLE) {
            Toast.makeText(requireContext(), "Xác thực sinh trắc học không khả dụng", Toast.LENGTH_SHORT).show()
            fallbackAuthentication()
            return
        }

        biometricHelper.showBiometricPrompt(
            title = "Xác thực sinh trắc học",
            subtitle = "Sử dụng vân tay hoặc khuôn mặt",
            negativeButtonText = "Sử dụng phương thức khác",
            onSuccess = {
                navigateToShopping()
            },
            onError = { errorCode, _ ->
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        // User chose alternative method
                        showAlternativeAuthMethods()
                    }
                    BiometricPrompt.ERROR_USER_CANCELED -> {
                        showLogoutConfirmation()
                    }
                    else -> {
                        showLogoutConfirmation()
                    }
                }
            },
            onFailed = {
                Toast.makeText(requireContext(), "Xác thực thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun authenticateWithDeviceCredential() {
        if (!biometricHelper.isDeviceCredentialAvailable()) {
            Toast.makeText(requireContext(), "PIN thiết bị không khả dụng", Toast.LENGTH_SHORT).show()
            fallbackAuthentication()
            return
        }

        biometricHelper.showDeviceCredentialPrompt(
            title = "Nhập mã PIN thiết bị",
            subtitle = "Sử dụng PIN/Pattern/Password của thiết bị",
            onSuccess = {
                navigateToShopping()
            },
            onError = { errorCode, _ ->
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED -> {
                        showLogoutConfirmation()
                    }
                    else -> {
                        showLogoutConfirmation()
                    }
                }
            },
            onFailed = {
                Toast.makeText(requireContext(), "Xác thực thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    /**
     * Authenticate using app's local PIN
     */
    private fun authenticateWithAppPin() {
        if (!pinCodeManager.isPinSet()) {
            Toast.makeText(requireContext(), "PIN ứng dụng chưa được thiết lập", Toast.LENGTH_SHORT).show()
            fallbackAuthentication()
            return
        }

        PinCodeDialog(
            context = requireContext(),
            mode = PinCodeDialog.Mode.VERIFY,
            pinCodeManager = pinCodeManager,
            onSuccess = {
                navigateToShopping()
            },
            onCancel = {
                showLogoutConfirmation()
            }
        ).show()
    }

    /**
     * Fallback when selected method is not available
     */
    private fun fallbackAuthentication() {
        // Try alternatives in order: Biometric -> Device Credential -> App PIN -> No auth
        when {
            biometricHelper.isBiometricAvailable() == BiometricHelper.BiometricStatus.AVAILABLE -> {
                authenticateWithBiometric()
            }
            biometricHelper.isDeviceCredentialAvailable() -> {
                authenticateWithDeviceCredential()
            }
            pinCodeManager.isPinSet() -> {
                authenticateWithAppPin()
            }
            else -> {
                // No authentication available, just go to shopping
                Toast.makeText(
                    requireContext(),
                    "Không có phương thức xác thực khả dụng",
                    Toast.LENGTH_SHORT
                ).show()
                navigateToShopping()
            }
        }
    }

    /**
     * Show dialog to pick alternative authentication method
     */
    private fun showAlternativeAuthMethods() {
        val methods = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        if (biometricHelper.isDeviceCredentialAvailable()) {
            methods.add("PIN / Pattern thiết bị")
            actions.add { authenticateWithDeviceCredential() }
        }

        if (pinCodeManager.isPinSet()) {
            methods.add("PIN ứng dụng")
            actions.add { authenticateWithAppPin() }
        }

        if (methods.isEmpty()) {
            Toast.makeText(requireContext(), "Không có phương thức thay thế", Toast.LENGTH_SHORT).show()
            showLogoutConfirmation()
            return
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Chọn phương thức xác thực")
            .setItems(methods.toTypedArray()) { _, which ->
                actions[which].invoke()
            }
            .setNegativeButton("Hủy") { _, _ ->
                showLogoutConfirmation()
            }
            .setCancelable(false)
            .show()
    }

    private fun showLogoutConfirmation() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Xác thực bị hủy")
            .setMessage("Bạn cần xác thực để tiếp tục sử dụng ứng dụng. Bạn muốn làm gì?")
            .setPositiveButton("Thử lại") { _, _ ->
                handleAuthenticationFlow()
            }
            .setNegativeButton("Đăng xuất") { _, _ ->
                viewmodel.signOut()
                Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_introductionFragment_to_accountOptionFragment)
            }
            .setNeutralButton("Thoát app") { _, _ ->
                requireActivity().finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToShopping() {
        Intent(requireActivity(), ShoppingActivity::class.java).also { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}