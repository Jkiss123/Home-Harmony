package com.example.furniturecloudy.util

import android.util.Patterns
import java.util.regex.Pattern

fun validateEmail(email:String) : RegisterValidation{
    if (email.isEmpty())
            return RegisterValidation.Failed("Email không được để trống")
    if(!Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return RegisterValidation.Failed("Email không đúng format")

    return  RegisterValidation.Success
}

fun validatePassword(password:String): RegisterValidation{
    if (password.isEmpty())
        return RegisterValidation.Failed("Password không được để trống")
    if (password.length <6)
            return RegisterValidation.Failed("Password phải lớn hơn 6 ký tự")
    return RegisterValidation.Success
}
