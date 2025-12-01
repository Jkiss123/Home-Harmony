package com.example.furniturecloudy.present.fragments.shopping

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentProfileBinding
import com.example.furniturecloudy.model.viewmodel.ProfileViewmodel
import com.example.furniturecloudy.present.LoginRegisterActivity
import com.example.furniturecloudy.util.AppAuthManager
import com.example.furniturecloudy.util.BiometricHelper
import com.example.furniturecloudy.util.PinCodeDialog
import com.example.furniturecloudy.util.PinCodeManager
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.SessionManager
import com.example.furniturecloudy.util.showBottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val viewModel : ProfileViewmodel by viewModels()

    private lateinit var appAuthManager: AppAuthManager
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var pinCodeManager: PinCodeManager
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize auth managers
        appAuthManager = AppAuthManager(requireContext())
        biometricHelper = BiometricHelper(this)
        pinCodeManager = PinCodeManager(requireContext())
        sessionManager = SessionManager.getInstance(requireContext())

        setupSecuritySettings()
        setupSessionTimeoutSettings()

        binding.constraintProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_userAccountFragment)
        }

        binding.linearWishlist.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_wishlistFragment)
        }

        binding.linearAllOrders.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_ordersFragment)
        }

        binding.linearLogOut.setOnClickListener {
            viewModel.logOut()
            appAuthManager.clearSettings() // Clear auth settings on logout
            val intent = Intent(requireContext(),LoginRegisterActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.user.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            binding.progressbarSettings.visibility = View.GONE
                        }
                        is Resource.Loading -> {
                            binding.progressbarSettings.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressbarSettings.visibility = View.GONE
                            Glide.with(requireView()).load(it.data!!.imagePath).error(ColorDrawable(
                                Color.BLACK)).into(binding.imageUser)
                            binding.tvUserName.text = it.data.firstName
                        }
                        else -> Unit
                    }
                }
            }
        }

    }

    private fun setupSecuritySettings() {
        // Load current auth state
        val isAuthEnabled = appAuthManager.isAuthEnabled()
        binding.switchAuth.isChecked = isAuthEnabled
        updateSecurityOptionsVisibility(isAuthEnabled)
        updateSelectedAuthMethodText()
        updateAppPinLabel()

        // Toggle auth on/off
        binding.switchAuth.setOnCheckedChangeListener { _, isChecked ->
            appAuthManager.setAuthEnabled(isChecked)
            updateSecurityOptionsVisibility(isChecked)
        }

        // Choose auth method
        binding.linearAuthMethod.setOnClickListener {
            showAuthMethodPicker()
        }

        // Setup App PIN
        binding.linearSetupAppPin.setOnClickListener {
            showAppPinSetup()
        }
    }

    private fun updateSecurityOptionsVisibility(authEnabled: Boolean) {
        val visibility = if (authEnabled) View.VISIBLE else View.GONE
        binding.dividerAuthMethod.visibility = visibility
        binding.linearAuthMethod.visibility = visibility
        binding.dividerAppPin.visibility = visibility
        binding.linearSetupAppPin.visibility = visibility
    }

    private fun updateSelectedAuthMethodText() {
        val methodText = when (appAuthManager.getAuthMethod()) {
            AppAuthManager.AuthMethod.BIOMETRIC -> "Vân tay / Khuôn mặt"
            AppAuthManager.AuthMethod.DEVICE_CREDENTIAL -> "PIN thiết bị"
            AppAuthManager.AuthMethod.APP_PIN -> "PIN ứng dụng"
        }
        binding.tvSelectedAuthMethod.text = methodText
    }

    private fun updateAppPinLabel() {
        binding.tvSetupAppPinLabel.text = if (pinCodeManager.isPinSet()) {
            "Đổi PIN ứng dụng"
        } else {
            "Thiết lập PIN ứng dụng"
        }
    }

    private fun showAuthMethodPicker() {
        val methods = mutableListOf<String>()
        val methodEnums = mutableListOf<AppAuthManager.AuthMethod>()

        // Check biometric availability
        when (biometricHelper.isBiometricAvailable()) {
            BiometricHelper.BiometricStatus.AVAILABLE -> {
                methods.add("Vân tay / Khuôn mặt")
                methodEnums.add(AppAuthManager.AuthMethod.BIOMETRIC)
            }
            else -> { /* Biometric not available */ }
        }

        // Check device credential availability
        if (biometricHelper.isDeviceCredentialAvailable()) {
            methods.add("PIN / Pattern thiết bị")
            methodEnums.add(AppAuthManager.AuthMethod.DEVICE_CREDENTIAL)
        }

        // App PIN is always available
        methods.add("PIN ứng dụng")
        methodEnums.add(AppAuthManager.AuthMethod.APP_PIN)

        val currentMethod = appAuthManager.getAuthMethod()
        val currentIndex = methodEnums.indexOf(currentMethod).coerceAtLeast(0)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Chọn phương thức xác thực")
            .setSingleChoiceItems(methods.toTypedArray(), currentIndex) { dialog, which ->
                val selectedMethod = methodEnums[which]

                // If selecting APP_PIN but no PIN set yet, prompt to set it first
                if (selectedMethod == AppAuthManager.AuthMethod.APP_PIN && !pinCodeManager.isPinSet()) {
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Vui lòng thiết lập PIN ứng dụng trước", Toast.LENGTH_SHORT).show()
                    showAppPinSetup {
                        // After PIN setup success, set the method
                        appAuthManager.setAuthMethod(selectedMethod)
                        updateSelectedAuthMethodText()
                    }
                } else {
                    appAuthManager.setAuthMethod(selectedMethod)
                    updateSelectedAuthMethodText()
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showAppPinSetup(onSuccess: (() -> Unit)? = null) {
        if (pinCodeManager.isPinSet()) {
            // Need to verify current PIN first
            PinCodeDialog(
                context = requireContext(),
                mode = PinCodeDialog.Mode.VERIFY,
                pinCodeManager = pinCodeManager,
                onSuccess = {
                    // After verification, clear old PIN and setup new one
                    pinCodeManager.clearPin()
                    showNewPinSetupDialog(onSuccess)
                },
                onCancel = {
                    Toast.makeText(requireContext(), "Đã hủy", Toast.LENGTH_SHORT).show()
                }
            ).show()
        } else {
            showNewPinSetupDialog(onSuccess)
        }
    }

    private fun showNewPinSetupDialog(onSuccess: (() -> Unit)? = null) {
        PinCodeDialog(
            context = requireContext(),
            mode = PinCodeDialog.Mode.SETUP,
            pinCodeManager = pinCodeManager,
            onSuccess = {
                Toast.makeText(requireContext(), "Đã thiết lập PIN thành công", Toast.LENGTH_SHORT).show()
                updateAppPinLabel()
                onSuccess?.invoke()
            },
            onCancel = {
                Toast.makeText(requireContext(), "Đã hủy", Toast.LENGTH_SHORT).show()
            }
        ).show()
    }

    override fun onResume() {
        super.onResume()
        showBottomNavigationView()
    }

    // ==================== SESSION TIMEOUT SETTINGS ====================

    private fun setupSessionTimeoutSettings() {
        // Load current session timeout state
        val isSessionTimeoutEnabled = sessionManager.isSessionTimeoutEnabled()
        binding.switchSessionTimeout.isChecked = isSessionTimeoutEnabled
        updateSessionTimeoutOptionsVisibility(isSessionTimeoutEnabled)
        updateSelectedTimeoutText()

        // Toggle session timeout on/off
        binding.switchSessionTimeout.setOnCheckedChangeListener { _, isChecked ->
            // Chỉ cho phép bật session timeout nếu authentication đã được bật
            if (isChecked && !appAuthManager.isAuthEnabled()) {
                binding.switchSessionTimeout.isChecked = false
                Toast.makeText(
                    requireContext(),
                    "Vui lòng bật 'Xác thực khi mở app' trước",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnCheckedChangeListener
            }

            sessionManager.setSessionTimeoutEnabled(isChecked)
            updateSessionTimeoutOptionsVisibility(isChecked)

            if (isChecked) {
                Toast.makeText(
                    requireContext(),
                    "Session Timeout đã được bật",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Choose timeout duration
        binding.linearTimeoutDuration.setOnClickListener {
            showTimeoutDurationPicker()
        }
    }

    private fun updateSessionTimeoutOptionsVisibility(enabled: Boolean) {
        val visibility = if (enabled) View.VISIBLE else View.GONE
        binding.dividerTimeoutDuration.visibility = visibility
        binding.linearTimeoutDuration.visibility = visibility
    }

    private fun updateSelectedTimeoutText() {
        val duration = sessionManager.getSessionTimeoutDuration()
        binding.tvSelectedTimeout.text = sessionManager.getTimeoutDisplayText(duration)
    }

    private fun showTimeoutDurationPicker() {
        val options = sessionManager.getTimeoutOptions()
        val labels = options.map { it.first }.toTypedArray()
        val currentDuration = sessionManager.getSessionTimeoutDuration()
        val currentIndex = options.indexOfFirst { it.second == currentDuration }.coerceAtLeast(0)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Chọn thời gian timeout")
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                val selectedDuration = options[which].second
                sessionManager.setSessionTimeoutDuration(selectedDuration)
                updateSelectedTimeoutText()
                Toast.makeText(
                    requireContext(),
                    "Đã đặt timeout: ${options[which].first}",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}