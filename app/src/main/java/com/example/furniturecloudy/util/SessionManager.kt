package com.example.furniturecloudy.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper

class SessionManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "session_prefs"
        private const val KEY_LAST_ACTIVITY_TIME = "last_activity_time"
        private const val KEY_SESSION_TIMEOUT_ENABLED = "session_timeout_enabled"
        private const val KEY_SESSION_TIMEOUT_DURATION = "session_timeout_duration"
        private const val KEY_SESSION_LOCKED = "session_locked"

        // Các mức timeout có sẵn (milliseconds)
        const val TIMEOUT_1_MINUTE = 1 * 60 * 1000L
        const val TIMEOUT_5_MINUTES = 5 * 60 * 1000L
        const val TIMEOUT_15_MINUTES = 15 * 60 * 1000L
        const val TIMEOUT_30_MINUTES = 30 * 60 * 1000L
        const val TIMEOUT_1_HOUR = 60 * 60 * 1000L

        // Default timeout: 5 phút
        const val DEFAULT_TIMEOUT = TIMEOUT_5_MINUTES

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var onSessionExpiredListener: OnSessionExpiredListener? = null

    /**
     * Interface để thông báo khi session hết hạn
     */
    interface OnSessionExpiredListener {
        fun onSessionExpired()
    }

    /**
     * Thiết lập listener khi session hết hạn
     */
    fun setOnSessionExpiredListener(listener: OnSessionExpiredListener?) {
        this.onSessionExpiredListener = listener
    }

    /**
     * Kiểm tra session timeout có được bật không
     */
    fun isSessionTimeoutEnabled(): Boolean {
        return prefs.getBoolean(KEY_SESSION_TIMEOUT_ENABLED, false)
    }

    /**
     * Bật/tắt tính năng session timeout
     */
    fun setSessionTimeoutEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SESSION_TIMEOUT_ENABLED, enabled).apply()
        if (enabled) {
            updateLastActivityTime()
            startSessionTimer()
        } else {
            stopSessionTimer()
            unlockSession()
        }
    }

    /**
     * Lấy thời gian timeout hiện tại (milliseconds)
     */
    fun getSessionTimeoutDuration(): Long {
        return prefs.getLong(KEY_SESSION_TIMEOUT_DURATION, DEFAULT_TIMEOUT)
    }

    /**
     * Thiết lập thời gian timeout
     */
    fun setSessionTimeoutDuration(durationMillis: Long) {
        prefs.edit().putLong(KEY_SESSION_TIMEOUT_DURATION, durationMillis).apply()
        if (isSessionTimeoutEnabled()) {
            restartSessionTimer()
        }
    }

    /**
     * Cập nhật thời gian hoạt động cuối cùng
     * Gọi method này mỗi khi user tương tác với app
     */
    fun updateLastActivityTime() {
        prefs.edit().putLong(KEY_LAST_ACTIVITY_TIME, System.currentTimeMillis()).apply()
        if (isSessionTimeoutEnabled()) {
            restartSessionTimer()
        }
    }

    /**
     * Lấy thời gian hoạt động cuối cùng
     */
    fun getLastActivityTime(): Long {
        return prefs.getLong(KEY_LAST_ACTIVITY_TIME, System.currentTimeMillis())
    }

    /**
     * Kiểm tra session đã hết hạn chưa
     */
    fun isSessionExpired(): Boolean {
        if (!isSessionTimeoutEnabled()) {
            return false
        }
        val lastActivity = getLastActivityTime()
        val timeout = getSessionTimeoutDuration()
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastActivity) >= timeout
    }

    /**
     * Kiểm tra session đang bị lock không
     */
    fun isSessionLocked(): Boolean {
        return prefs.getBoolean(KEY_SESSION_LOCKED, false)
    }

    /**
     * Lock session (yêu cầu xác thực lại)
     */
    fun lockSession() {
        prefs.edit().putBoolean(KEY_SESSION_LOCKED, true).apply()
    }

    /**
     * Unlock session sau khi xác thực thành công
     */
    fun unlockSession() {
        prefs.edit().putBoolean(KEY_SESSION_LOCKED, false).apply()
        updateLastActivityTime()
    }

    /**
     * Bắt đầu timer theo dõi session
     */
    fun startSessionTimer() {
        if (!isSessionTimeoutEnabled()) return

        stopSessionTimer()

        val timeout = getSessionTimeoutDuration()
        val lastActivity = getLastActivityTime()
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastActivity
        val remainingTime = (timeout - elapsed).coerceAtLeast(0)

        timeoutRunnable = Runnable {
            if (isSessionTimeoutEnabled()) {
                lockSession()
                onSessionExpiredListener?.onSessionExpired()
            }
        }

        handler.postDelayed(timeoutRunnable!!, remainingTime)
    }

    /**
     * Dừng timer
     */
    fun stopSessionTimer() {
        timeoutRunnable?.let {
            handler.removeCallbacks(it)
        }
        timeoutRunnable = null
    }

    /**
     * Khởi động lại timer
     */
    private fun restartSessionTimer() {
        stopSessionTimer()
        startSessionTimer()
    }

    /**
     * Gọi khi app vào foreground
     * Kiểm tra xem có cần yêu cầu xác thực lại không
     */
    fun onAppForeground(): Boolean {
        if (!isSessionTimeoutEnabled()) {
            return false
        }

        if (isSessionExpired() || isSessionLocked()) {
            lockSession()
            return true // Cần xác thực lại
        }

        startSessionTimer()
        return false
    }

    /**
     * Gọi khi app vào background
     */
    fun onAppBackground() {
        stopSessionTimer()
    }

    /**
     * Reset toàn bộ session (dùng khi logout)
     */
    fun clearSession() {
        stopSessionTimer()
        prefs.edit()
            .remove(KEY_LAST_ACTIVITY_TIME)
            .putBoolean(KEY_SESSION_LOCKED, false)
            .apply()
    }

    /**
     * Gọi khi app khởi động (fresh start)
     * Reset session state vì đã có luồng xác thực riêng khi mở app
     */
    fun onAppStart() {
        unlockSession()
        updateLastActivityTime()
    }

    /**
     * Lấy thời gian còn lại trước khi session hết hạn (milliseconds)
     */
    fun getRemainingTime(): Long {
        if (!isSessionTimeoutEnabled()) return Long.MAX_VALUE

        val lastActivity = getLastActivityTime()
        val timeout = getSessionTimeoutDuration()
        val currentTime = System.currentTimeMillis()
        val remaining = timeout - (currentTime - lastActivity)
        return remaining.coerceAtLeast(0)
    }

    /**
     * Chuyển đổi duration thành text hiển thị
     */
    fun getTimeoutDisplayText(durationMillis: Long): String {
        return when (durationMillis) {
            TIMEOUT_1_MINUTE -> "1 phút"
            TIMEOUT_5_MINUTES -> "5 phút"
            TIMEOUT_15_MINUTES -> "15 phút"
            TIMEOUT_30_MINUTES -> "30 phút"
            TIMEOUT_1_HOUR -> "1 giờ"
            else -> "${durationMillis / 60000} phút"
        }
    }

    /**
     * Lấy danh sách các timeout options
     */
    fun getTimeoutOptions(): List<Pair<String, Long>> {
        return listOf(
            "1 phút" to TIMEOUT_1_MINUTE,
            "5 phút" to TIMEOUT_5_MINUTES,
            "15 phút" to TIMEOUT_15_MINUTES,
            "30 phút" to TIMEOUT_30_MINUTES,
            "1 giờ" to TIMEOUT_1_HOUR
        )
    }
}