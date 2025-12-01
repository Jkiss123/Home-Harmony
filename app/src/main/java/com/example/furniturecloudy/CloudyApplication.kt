package com.example.furniturecloudy

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.furniturecloudy.util.SessionManager
import dagger.hilt.android.HiltAndroidApp
//11/3/2024

/**
 * CloudyApplication - Application class với Session Management
 *
 * Tính năng bảo mật:
 * - Theo dõi app lifecycle (foreground/background)
 * - Tự động kiểm tra session timeout khi app trở lại foreground
 * - Quản lý session timer
 */
@HiltAndroidApp
class CloudyApplication : Application(), DefaultLifecycleObserver {

    private lateinit var sessionManager: SessionManager
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super<Application>.onCreate()

        sessionManager = SessionManager.getInstance(this)

        // Reset session khi app khởi động (vì đã có xác thực riêng ở IntroductionFragment)
        sessionManager.onAppStart()

        // Đăng ký lifecycle observer để theo dõi app foreground/background
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Đăng ký activity lifecycle callbacks
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    /**
     * Khi app vào foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        sessionManager.onAppForeground()
    }

    /**
     * Khi app vào background
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        sessionManager.onAppBackground()
    }

    /**
     * Activity lifecycle callbacks để theo dõi activity hiện tại
     */
    private val activityLifecycleCallbacks = object : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {
            currentActivity = activity
        }

        override fun onActivityPaused(activity: Activity) {
            if (currentActivity == activity) {
                currentActivity = null
            }
        }

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }

    /**
     * Lấy activity hiện tại
     */
    fun getCurrentActivity(): Activity? = currentActivity
}