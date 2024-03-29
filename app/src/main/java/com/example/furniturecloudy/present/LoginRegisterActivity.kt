package com.example.furniturecloudy.present

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.furniturecloudy.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginRegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_register)
    }
}