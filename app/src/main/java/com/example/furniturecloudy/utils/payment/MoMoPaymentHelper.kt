package com.example.furniturecloudy.utils.payment

import android.app.Activity
import android.content.Intent
import android.util.Log
import vn.momo.momo_partner.AppMoMoLib
import java.util.UUID

/**
 * Helper class for MoMo Payment integration
 *
 * Usage:
 * 1. Get merchant credentials from https://business.momo.vn
 * 2. Initialize in Activity: MoMoPaymentHelper.initialize(isDevelopment = true)
 * 3. Request payment: moMoHelper.requestPayment(activity, amount, orderId, description)
 * 4. Handle result in onActivityResult
 */
class MoMoPaymentHelper(
    private val merchantName: String,
    private val merchantCode: String,
    private val merchantNameLabel: String,
    private val description: String = "Payment for order"
) {

    companion object {
        private const val TAG = "MoMoPaymentHelper"
        const val PAYMENT_METHOD_MOMO = "MoMo"

        /**
         * Initialize MoMo SDK
         * @param isDevelopment true for testing, false for production
         */
        fun initialize(isDevelopment: Boolean = true) {
            val environment = if (isDevelopment) {
                AppMoMoLib.ENVIRONMENT.DEVELOPMENT
            } else {
                AppMoMoLib.ENVIRONMENT.PRODUCTION
            }
            AppMoMoLib.getInstance().setEnvironment(environment)
            Log.d(TAG, "MoMo SDK initialized with environment: $environment")
        }

        /**
         * Check if MoMo app is installed on device
         */
        fun isMoMoAppInstalled(activity: Activity): Boolean {
            return try {
                activity.packageManager.getPackageInfo("com.momo.platform", 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Request payment from MoMo
     *
     * @param activity Current activity
     * @param amount Payment amount in VND
     * @param orderId Unique order ID
     * @param description Payment description (optional)
     * @param fee Transaction fee (default 0)
     * @param extraData Additional data to send (optional)
     */
    fun requestPayment(
        activity: Activity,
        amount: Long,
        orderId: String,
        description: String = this.description,
        fee: Long = 0,
        extraData: String = ""
    ) {
        try {
            // Set action type
            AppMoMoLib.getInstance().setAction(AppMoMoLib.ACTION.PAYMENT)
            AppMoMoLib.getInstance().setActionType(AppMoMoLib.ACTION_TYPE.GET_TOKEN)

            // Prepare payment data
            val eventValue = HashMap<String, Any>().apply {
                put("merchantname", merchantName)
                put("merchantcode", merchantCode)
                put("amount", amount)
                put("orderId", orderId)
                put("orderLabel", orderId)

                put("merchantnamelabel", merchantNameLabel)
                put("fee", fee)
                put("description", description)
                put("requestId", UUID.randomUUID().toString())
                put("partnerCode", merchantCode)

                // Extra data (optional)
                if (extraData.isNotEmpty()) {
                    put("extra", extraData)
                }

                // Language setting
                put("language", "vi")
            }

            Log.d(TAG, "Requesting MoMo payment with data: $eventValue")

            // Request MoMo callback
            AppMoMoLib.getInstance().requestMoMoCallBack(activity, eventValue)

        } catch (e: Exception) {
            Log.e(TAG, "Error requesting MoMo payment: ${e.message}", e)
            throw MoMoPaymentException("Failed to request MoMo payment", e)
        }
    }

    /**
     * Handle MoMo payment result from onActivityResult
     *
     * @param requestCode Request code from onActivityResult
     * @param resultCode Result code from onActivityResult
     * @param data Intent data from onActivityResult
     * @return MoMoPaymentResult or null if not a MoMo payment result
     */
    fun handlePaymentResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): MoMoPaymentResult? {
        if (requestCode != AppMoMoLib.getInstance().REQUEST_CODE_MOMO) {
            return null
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            return MoMoPaymentResult(
                success = false,
                message = "Payment cancelled or failed",
                errorCode = -1
            )
        }

        val status = data.getIntExtra("status", -1)
        val message = data.getStringExtra("message") ?: ""
        val token = data.getStringExtra("data") ?: ""
        val phoneNumber = data.getStringExtra("phonenumber") ?: ""
        val env = data.getStringExtra("env") ?: ""
        val errorCode = data.getIntExtra("errorcode", 0)

        Log.d(TAG, "MoMo payment result - Status: $status, Message: $message, Token: $token")

        return MoMoPaymentResult(
            success = status == 0,
            token = token,
            message = message,
            phoneNumber = phoneNumber,
            environment = env,
            errorCode = errorCode,
            status = status
        )
    }
}

/**
 * Data class for MoMo payment result
 */
data class MoMoPaymentResult(
    val success: Boolean,
    val token: String = "",
    val message: String,
    val phoneNumber: String = "",
    val environment: String = "",
    val errorCode: Int = 0,
    val status: Int = -1
) {
    fun isSuccessful(): Boolean = success && status == 0

    fun getErrorMessage(): String {
        return when (errorCode) {
            1 -> "User cancelled payment"
            2 -> "Payment failed"
            3 -> "Payment timeout"
            4 -> "Invalid payment data"
            else -> message
        }
    }
}

/**
 * Exception for MoMo payment errors
 */
class MoMoPaymentException(message: String, cause: Throwable? = null) : Exception(message, cause)
