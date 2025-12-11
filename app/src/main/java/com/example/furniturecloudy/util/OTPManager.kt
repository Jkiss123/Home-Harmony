package com.example.furniturecloudy.util

import android.util.Log
import com.example.furniturecloudy.data.OTPCode
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * OTP Manager - Core logic for Two-Factor Authentication
 *
 * Handles OTP generation, storage, verification
 */
class OTPManager(private val firestore: FirebaseFirestore) {

    companion object {
        private const val TAG = "OTPManager"
        private const val COLLECTION_OTP_CODES = "otp_codes"
    }

    init {
        Log.d(TAG, OTPConfig.getDebugStatus())
    }

    /**
     * Generate random 6-digit OTP code
     */
    fun generateOTP(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    /**
     * Save OTP to Firestore with expiry time
     *
     * @param userId User ID
     * @param email User email
     * @return Generated OTP code
     */
    suspend fun createAndSaveOTP(userId: String, email: String): Result<String> {
        return try {
            val otp = generateOTP()
            val now = Timestamp.now()
            val expirySeconds = now.seconds + (OTPConfig.OTP_EXPIRY_MINUTES * 60)
            val expiresAt = Timestamp(expirySeconds, 0)

            val otpCode = OTPCode(
                userId = userId,
                otp = otp,
                email = email,
                createdAt = now,
                expiresAt = expiresAt,
                verified = false,
                attempts = 0,
                maxAttempts = OTPConfig.OTP_MAX_ATTEMPTS
            )

            // Save to Firestore
            firestore.collection(COLLECTION_OTP_CODES)
                .document(userId)
                .set(otpCode)
                .await()

            Log.d(TAG, "OTP created for user $userId: $otp (expires in ${OTPConfig.OTP_EXPIRY_MINUTES} min)")

            if (OTPConfig.DEBUG_BYPASS_OTP) {
                Log.w(TAG, "⚠️ DEBUG MODE: OTP=$otp OR use ${OTPConfig.DEBUG_OTP_CODE}")
            }

            Result.success(otp)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating OTP", e)
            Result.failure(e)
        }
    }

    /**
     * Verify OTP code entered by user
     *
     * @param userId User ID
     * @param inputOtp OTP entered by user
     * @return Result with success/failure
     */
    suspend fun verifyOTP(userId: String, inputOtp: String): OTPVerificationResult {
        return try {
            // 🔓 DEBUG MODE: Accept debug OTP or any 6-digit code
            if (OTPConfig.DEBUG_BYPASS_OTP) {
                Log.w(TAG, "⚠️ DEBUG MODE: Accepting OTP=$inputOtp (bypass enabled)")
                if (inputOtp == OTPConfig.DEBUG_OTP_CODE || inputOtp.length == 6) {
                    // Still mark as verified in Firestore if document exists
                    try {
                        firestore.collection(COLLECTION_OTP_CODES)
                            .document(userId)
                            .update("verified", true)
                            .await()
                    } catch (e: Exception) {
                        // Ignore if document doesn't exist in debug mode
                        Log.d(TAG, "Debug mode: OTP document not found, ignoring")
                    }
                    return OTPVerificationResult.Success
                }
            }

            // Get OTP document from Firestore
            val docSnapshot = firestore.collection(COLLECTION_OTP_CODES)
                .document(userId)
                .get()
                .await()

            if (!docSnapshot.exists()) {
                Log.w(TAG, "OTP not found for user $userId")
                return OTPVerificationResult.NotFound
            }

            val otpCode = docSnapshot.toObject(OTPCode::class.java)
                ?: return OTPVerificationResult.Error("Failed to parse OTP data")

            // Check if already verified
            if (otpCode.verified) {
                Log.w(TAG, "OTP already used for user $userId")
                return OTPVerificationResult.AlreadyUsed
            }

            // Check if expired
            if (otpCode.isExpired()) {
                Log.w(TAG, "OTP expired for user $userId")
                return OTPVerificationResult.Expired
            }

            // Check if locked due to too many attempts
            if (otpCode.isLocked()) {
                Log.w(TAG, "OTP locked for user $userId (too many attempts)")
                return OTPVerificationResult.Locked
            }

            // Verify OTP
            if (inputOtp == otpCode.otp) {
                // ✅ Success - Mark as verified
                firestore.collection(COLLECTION_OTP_CODES)
                    .document(userId)
                    .update(
                        mapOf(
                            "verified" to true,
                            "lastAttemptAt" to Timestamp.now()
                        )
                    )
                    .await()

                Log.d(TAG, "✅ OTP verified successfully for user $userId")
                OTPVerificationResult.Success
            } else {
                // ❌ Wrong OTP - Increment attempts
                val newAttempts = otpCode.attempts + 1
                firestore.collection(COLLECTION_OTP_CODES)
                    .document(userId)
                    .update(
                        mapOf(
                            "attempts" to newAttempts,
                            "lastAttemptAt" to Timestamp.now()
                        )
                    )
                    .await()

                val remaining = otpCode.maxAttempts - newAttempts
                Log.w(TAG, "❌ Wrong OTP for user $userId. Attempts: $newAttempts/${otpCode.maxAttempts}")

                if (remaining <= 0) {
                    OTPVerificationResult.Locked
                } else {
                    OTPVerificationResult.WrongCode(remaining)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying OTP", e)
            OTPVerificationResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Get OTP code for a user (for checking status)
     */
    suspend fun getOTPCode(userId: String): OTPCode? {
        return try {
            val docSnapshot = firestore.collection(COLLECTION_OTP_CODES)
                .document(userId)
                .get()
                .await()

            docSnapshot.toObject(OTPCode::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting OTP code", e)
            null
        }
    }

    /**
     * Invalidate OTP after successful login or on logout
     */
    suspend fun invalidateOTP(userId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_OTP_CODES)
                .document(userId)
                .delete()
                .await()

            Log.d(TAG, "OTP invalidated for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error invalidating OTP", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user can resend OTP (cooldown period)
     */
    suspend fun canResendOTP(userId: String): Boolean {
        val otpCode = getOTPCode(userId) ?: return true

        val now = Timestamp.now().seconds
        val created = otpCode.createdAt.seconds
        val elapsed = now - created

        return elapsed >= OTPConfig.RESEND_COOLDOWN_SECONDS
    }

    /**
     * Get remaining cooldown time before resend is allowed
     */
    suspend fun getResendCooldownSeconds(userId: String): Long {
        val otpCode = getOTPCode(userId) ?: return 0

        val now = Timestamp.now().seconds
        val created = otpCode.createdAt.seconds
        val elapsed = now - created
        val remaining = OTPConfig.RESEND_COOLDOWN_SECONDS - elapsed

        return if (remaining > 0) remaining else 0
    }
}

/**
 * OTP Verification Result
 */
sealed class OTPVerificationResult {
    object Success : OTPVerificationResult()
    object NotFound : OTPVerificationResult()
    object Expired : OTPVerificationResult()
    object AlreadyUsed : OTPVerificationResult()
    object Locked : OTPVerificationResult()
    data class WrongCode(val remainingAttempts: Int) : OTPVerificationResult()
    data class Error(val message: String) : OTPVerificationResult()

    fun isSuccess() = this is Success

    fun getErrorMessage(): String {
        return when (this) {
            is Success -> ""
            is NotFound -> "Mã OTP không tồn tại. Vui lòng thử lại."
            is Expired -> "Mã OTP đã hết hạn. Vui lòng gửi lại mã mới."
            is AlreadyUsed -> "Mã OTP đã được sử dụng."
            is Locked -> "Bạn đã nhập sai quá nhiều lần. Vui lòng gửi lại mã mới."
            is WrongCode -> "Mã OTP không đúng. Còn $remainingAttempts lần thử."
            is Error -> "Lỗi: $message"
        }
    }
}
