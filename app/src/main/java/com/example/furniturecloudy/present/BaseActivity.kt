package com.example.furniturecloudy.present

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.furniturecloudy.util.AppAuthManager
import com.example.furniturecloudy.util.SessionManager

abstract class BaseActivity : AppCompatActivity(), SessionManager.OnSessionExpiredListener {

    protected lateinit var sessionManager: SessionManager
    protected lateinit var appAuthManager: AppAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager.getInstance(this)
        appAuthManager = AppAuthManager(this)
        sessionManager.setOnSessionExpiredListener(this)
    }

    override fun onResume() {
        super.onResume()
        checkSessionAndAuthenticate()
    }

    override fun onPause() {
        super.onPause()
        // Cập nhật thời gian hoạt động cuối khi app vào background
        if (sessionManager.isSessionTimeoutEnabled()) {
            sessionManager.updateLastActivityTime()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager.setOnSessionExpiredListener(null)
    }

    /**
     * Bắt mọi touch event để cập nhật thời gian hoạt động
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            sessionManager.updateLastActivityTime()
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * Kiểm tra session và yêu cầu xác thực nếu cần
     */
    protected fun checkSessionAndAuthenticate() {
        // Chỉ kiểm tra nếu cả session timeout và app auth đều được bật
        if (!sessionManager.isSessionTimeoutEnabled() || !appAuthManager.isAuthEnabled()) {
            return
        }

        if (sessionManager.isSessionExpired() || sessionManager.isSessionLocked()) {
            sessionManager.lockSession()
            showAuthenticationScreen()
        } else {
            sessionManager.startSessionTimer()
        }
    }

    /**
     * Callback khi session hết hạn
     */
    override fun onSessionExpired() {
        runOnUiThread {
            showAuthenticationScreen()
        }
    }

    /**
     * Hiển thị màn hình xác thực
     * Override trong subclass nếu cần xử lý khác
     */
    protected open fun showAuthenticationScreen() {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LockScreenActivity.EXTRA_REASON, LockScreenActivity.REASON_SESSION_TIMEOUT)
        }
        startActivity(intent)
    }

    /**
     * Gọi sau khi xác thực thành công
     */
    protected fun onAuthenticationSuccess() {
        sessionManager.unlockSession()
    }
}