package com.example.furniturecloudy.present.fragments.loginRegister

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.BottomSheetOtpVerificationBinding
import com.example.furniturecloudy.model.viewmodel.OTPViewModel
import com.example.furniturecloudy.util.EmailService
import com.example.furniturecloudy.util.OTPConfig
import com.example.furniturecloudy.util.OTPVerificationResult
import com.example.furniturecloudy.util.Resource
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * OTP Verification BottomSheet Fragment
 *
 * Shows beautiful 6-digit OTP input with:
 * - Auto-focus and auto-submit
 * - Countdown timer
 * - Resend button
 * - Error handling with shake animation
 * - Success animation
 */
@AndroidEntryPoint
class OTPBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetOtpVerificationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OTPViewModel by viewModels()
    private val emailService = EmailService()

    private var userId: String = ""
    private var userEmail: String = ""
    private var onVerificationSuccess: (() -> Unit)? = null

    companion object {
        private const val TAG = "OTPBottomSheet"
        private const val ARG_USER_ID = "user_id"
        private const val ARG_EMAIL = "email"

        /**
         * Create new instance
         */
        fun newInstance(
            userId: String,
            email: String,
            onSuccess: () -> Unit
        ): OTPBottomSheetFragment {
            return OTPBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                    putString(ARG_EMAIL, email)
                }
                this.onVerificationSuccess = onSuccess
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString(ARG_USER_ID) ?: ""
        userEmail = arguments?.getString(ARG_EMAIL) ?: ""

        Log.d(TAG, "OTP BottomSheet created for user: $userId, email: $userEmail")
        Log.d(TAG, OTPConfig.getDebugStatus())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetOtpVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        setupClickListeners()

        // Auto-generate and send OTP when sheet opens
        viewModel.generateAndSendOTP(userId, userEmail)
    }

    private fun setupUI() {
        // Show masked email
        binding.tvEmail.text = emailService.maskEmail(userEmail)

        // Show debug mode indicator if enabled
        binding.tvDebugMode.isVisible = OTPConfig.DEBUG_BYPASS_OTP

        // Set OTP complete listener
        binding.otpInputView.setOnOTPCompleteListener { otp ->
            Log.d(TAG, "OTP entered: $otp")
            // Auto-verify when 6 digits entered
            verifyOTP(otp)
        }

        // Focus on first box
        binding.otpInputView.requestOTPFocus()
    }

    private fun setupClickListeners() {
        // Verify button
        binding.btnVerify.setOnClickListener {
            val otp = binding.otpInputView.getOTP()
            if (otp.length == 6) {
                verifyOTP(otp)
            } else {
                showError("Vui lòng nhập đầy đủ 6 số")
            }
        }

        // Resend button
        binding.tvResend.setOnClickListener {
            resendOTP()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe OTP send state
                launch {
                    viewModel.otpSendState.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> {
                                showLoading()
                                Log.d(TAG, "Sending OTP...")
                            }
                            is Resource.Success -> {
                                hideLoading()
                                Log.d(TAG, "✅ OTP sent successfully: ${resource.data}")
                                if (OTPConfig.DEBUG_BYPASS_OTP) {
                                    showDebugInfo(resource.data ?: "")
                                }
                            }
                            is Resource.Error -> {
                                hideLoading()
                                Log.e(TAG, "❌ Failed to send OTP: ${resource.message}")
                                showError(resource.message ?: "Không thể gửi mã OTP")
                            }
                            else -> Unit
                        }
                    }
                }

                // Observe verification state
                launch {
                    viewModel.otpVerificationState.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> {
                                showLoading()
                                binding.otpInputView.setEnabled(false)
                                Log.d(TAG, "Verifying OTP...")
                            }
                            is Resource.Success -> {
                                hideLoading()
                                binding.otpInputView.setEnabled(true)
                                handleVerificationResult(resource.data!!)
                            }
                            is Resource.Error -> {
                                hideLoading()
                                binding.otpInputView.setEnabled(true)
                                showError(resource.message ?: "Lỗi xác thực")
                            }
                            else -> Unit
                        }
                    }
                }

                // Observe timer
                launch {
                    viewModel.timerState.collect { seconds ->
                        updateTimer(seconds)
                    }
                }

                // Observe can resend
                launch {
                    viewModel.canResend.collect { canResend ->
                        binding.tvResend.isEnabled = canResend
                        binding.tvResend.alpha = if (canResend) 1.0f else 0.5f
                    }
                }
            }
        }
    }

    private fun verifyOTP(otp: String) {
        hideError()
        viewModel.verifyOTP(userId, otp)
    }

    private fun handleVerificationResult(result: OTPVerificationResult) {
        when (result) {
            is OTPVerificationResult.Success -> {
                Log.d(TAG, "✅ OTP verification SUCCESS!")
                showSuccess()
                // Delay before dismissing
                view?.postDelayed({
                    onVerificationSuccess?.invoke()
                    dismiss()
                }, 1000)
            }
            else -> {
                Log.w(TAG, "❌ OTP verification failed: ${result.getErrorMessage()}")
                showError(result.getErrorMessage())
                binding.otpInputView.shake()
                binding.otpInputView.clear()
            }
        }
    }

    private fun resendOTP() {
        Log.d(TAG, "Resending OTP...")
        hideError()
        binding.otpInputView.clear()
        viewModel.resendOTP(userId, userEmail)
    }

    private fun updateTimer(seconds: Int) {
        val minutes = seconds / 60
        val secs = seconds % 60
        binding.tvTimer.text = String.format("Gửi lại sau %02d:%02d", minutes, secs)
    }

    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.btnVerify.isEnabled = false
        binding.btnVerify.alpha = 0.7f
    }

    private fun hideLoading() {
        binding.progressBar.isVisible = false
        binding.btnVerify.isEnabled = true
        binding.btnVerify.alpha = 1.0f
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.isVisible = true

        // Shake animation
        val shakeAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.shake_animation)
        binding.tvError.startAnimation(shakeAnim)
    }

    private fun hideError() {
        binding.tvError.isVisible = false
    }

    private fun showSuccess() {
        // Change title to success
        binding.tvTitle.text = "✅ Xác thực thành công!"
        binding.tvDescription.isVisible = false
        binding.tvEmail.isVisible = false
        binding.otpInputView.isVisible = false
        binding.layoutTimer.isVisible = false
        binding.btnVerify.isVisible = false
        binding.layoutResend.isVisible = false
        hideError()
    }

    private fun showDebugInfo(otp: String) {
        Log.d(TAG, "⚠️ DEBUG MODE - Real OTP: $otp")
        // Optionally show in UI for testing
        if (OTPConfig.DEBUG_BYPASS_OTP) {
            binding.tvDebugMode.text = "⚠️ DEBUG: Real OTP = $otp (or use ${OTPConfig.DEBUG_OTP_CODE})"
            binding.tvDebugMode.isVisible = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
