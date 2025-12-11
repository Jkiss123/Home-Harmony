package com.example.furniturecloudy.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Email Service for sending OTP codes
 *
 * Uses EmailJS (https://www.emailjs.com/) - No backend required!
 *
 * Setup instructions:
 * 1. Go to https://www.emailjs.com/
 * 2. Create free account
 * 3. Add email service (Gmail, Outlook, etc.)
 * 4. Create email template with variables: {{user_email}}, {{otp_code}}, {{user_name}}
 * 5. Copy Service ID, Template ID, User ID to OTPConfig.kt
 */
class EmailService {

    companion object {
        private const val TAG = "EmailService"
        private const val EMAILJS_API_URL = "https://api.emailjs.com/api/v1.0/email/send"
    }

    /**
     * Send OTP email to user
     *
     * @param toEmail Recipient email
     * @param toName Recipient name
     * @param otpCode 6-digit OTP code
     * @return Result with success/failure
     */
    suspend fun sendOTPEmail(
        toEmail: String,
        toName: String,
        otpCode: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check if EmailJS is configured
            if (OTPConfig.EMAILJS_SERVICE_ID == "YOUR_SERVICE_ID") {
                Log.w(TAG, "⚠️ EmailJS not configured! Please set credentials in OTPConfig.kt")
                Log.w(TAG, "📧 Simulating email send: OTP=$otpCode to $toEmail")

                if (OTPConfig.DEBUG_BYPASS_OTP) {
                    // In debug mode, just log the OTP
                    Log.d(TAG, "✅ DEBUG MODE: Email simulation successful. OTP=$otpCode")
                    return@withContext Result.success(Unit)
                }

                return@withContext Result.failure(
                    Exception("EmailJS not configured. Please update OTPConfig.kt")
                )
            }

            // ⚠️ EmailJS blocks direct API calls from Android apps
            // For production, use a proxy server (see email-proxy/README.md)
            if (OTPConfig.DEBUG_BYPASS_OTP) {
                // Debug mode: Just log and return success (OTP shown in UI)
                Log.d(TAG, "✅ DEBUG MODE: OTP=$otpCode (shown in UI)")
                return@withContext Result.success(Unit)
            }

            // Build JSON payload for EmailJS
            val payload = JSONObject().apply {
                put("service_id", OTPConfig.EMAILJS_SERVICE_ID)
                put("template_id", OTPConfig.EMAILJS_TEMPLATE_ID)
                put("user_id", OTPConfig.EMAILJS_USER_ID)
                put("template_params", JSONObject().apply {
                    put("user_email", toEmail)
                    put("user_name", toName)
                    put("otp_code", otpCode)
                    put("app_name", "Home Harmony")
                    put("expiry_minutes", OTPConfig.OTP_EXPIRY_MINUTES)
                })
            }

            // Send HTTP POST request
            val url = URL(EMAILJS_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            // Write payload
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            // Check response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "✅ Email sent successfully to $toEmail")
                Result.success(Unit)
            } else {
                val errorMessage = connection.errorStream?.bufferedReader()?.readText()
                    ?: "HTTP $responseCode"
                Log.e(TAG, "❌ Failed to send email: $errorMessage")
                Result.failure(Exception("Failed to send email: $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending email", e)
            Result.failure(e)
        }
    }

    /**
     * Send OTP email with default template
     */
    suspend fun sendOTP(email: String, otpCode: String): Result<Unit> {
        val name = email.substringBefore('@')  // Use email prefix as name
        return sendOTPEmail(email, name, otpCode)
    }

    /**
     * Mask email for display (e.g., "m***y@gmail.com")
     */
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email

        val username = parts[0]
        val domain = parts[1]

        return when {
            username.length <= 2 -> "${username.first()}***@$domain"
            username.length <= 4 -> "${username.first()}***${username.last()}@$domain"
            else -> "${username.take(2)}***${username.takeLast(2)}@$domain"
        }
    }
}

/**
 * EmailJS Template Example:
 *
 * Subject: Your Home Harmony OTP Code
 *
 * Body:
 * ```
 * Hello {{user_name}},
 *
 * Your OTP code for Home Harmony is:
 *
 * {{otp_code}}
 *
 * This code will expire in {{expiry_minutes}} minutes.
 *
 * If you didn't request this code, please ignore this email.
 *
 * Best regards,
 * Home Harmony Team
 * ```
 */
