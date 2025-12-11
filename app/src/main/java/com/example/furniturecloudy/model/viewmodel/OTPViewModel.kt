package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.util.EmailService
import com.example.furniturecloudy.util.OTPManager
import com.example.furniturecloudy.util.OTPVerificationResult
import com.example.furniturecloudy.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * OTP ViewModel - Manages OTP generation, sending, and verification
 */
@HiltViewModel
class OTPViewModel @Inject constructor(
    firestore: FirebaseFirestore
) : ViewModel() {

    private val otpManager = OTPManager(firestore)
    private val emailService = EmailService()

    // OTP generation and sending state
    private val _otpSendState = MutableSharedFlow<Resource<String>>()
    val otpSendState = _otpSendState.asSharedFlow()

    // OTP verification state
    private val _otpVerificationState = MutableSharedFlow<Resource<OTPVerificationResult>>()
    val otpVerificationState = _otpVerificationState.asSharedFlow()

    // Timer state (countdown for resend)
    private val _timerState = MutableStateFlow(60)
    val timerState = _timerState.asStateFlow()

    // Can resend state
    private val _canResend = MutableStateFlow(false)
    val canResend = _canResend.asStateFlow()

    /**
     * Generate and send OTP to user's email
     *
     * @param userId User ID
     * @param email User email
     */
    fun generateAndSendOTP(userId: String, email: String) {
        viewModelScope.launch {
            try {
                _otpSendState.emit(Resource.Loading())

                // Generate and save OTP
                val result = otpManager.createAndSaveOTP(userId, email)

                if (result.isSuccess) {
                    val otp = result.getOrNull()!!

                    // Send OTP email
                    val emailResult = emailService.sendOTP(email, otp)

                    if (emailResult.isSuccess) {
                        _otpSendState.emit(Resource.Success(otp))
                        startResendTimer()
                    } else {
                        _otpSendState.emit(
                            Resource.Error(
                                emailResult.exceptionOrNull()?.message ?: "Failed to send email"
                            )
                        )
                    }
                } else {
                    _otpSendState.emit(
                        Resource.Error(
                            result.exceptionOrNull()?.message ?: "Failed to generate OTP"
                        )
                    )
                }
            } catch (e: Exception) {
                _otpSendState.emit(Resource.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Verify OTP entered by user
     *
     * @param userId User ID
     * @param otp OTP code entered
     */
    fun verifyOTP(userId: String, otp: String) {
        viewModelScope.launch {
            try {
                _otpVerificationState.emit(Resource.Loading())

                val result = otpManager.verifyOTP(userId, otp)

                _otpVerificationState.emit(Resource.Success(result))
            } catch (e: Exception) {
                _otpVerificationState.emit(Resource.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Resend OTP
     */
    fun resendOTP(userId: String, email: String) {
        viewModelScope.launch {
            val canResend = otpManager.canResendOTP(userId)

            if (canResend) {
                generateAndSendOTP(userId, email)
            } else {
                val remaining = otpManager.getResendCooldownSeconds(userId)
                _otpSendState.emit(
                    Resource.Error("Vui lòng đợi $remaining giây để gửi lại mã")
                )
            }
        }
    }

    /**
     * Start countdown timer for resend button
     */
    private fun startResendTimer() {
        viewModelScope.launch {
            _canResend.value = false
            _timerState.value = 60

            repeat(60) { second ->
                kotlinx.coroutines.delay(1000)
                val remaining = 60 - second - 1
                _timerState.value = remaining

                if (remaining <= 0) {
                    _canResend.value = true
                }
            }
        }
    }

    /**
     * Invalidate OTP (after successful verification or logout)
     */
    fun invalidateOTP(userId: String) {
        viewModelScope.launch {
            otpManager.invalidateOTP(userId)
        }
    }
}
