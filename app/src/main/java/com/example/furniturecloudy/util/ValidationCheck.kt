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

fun validatePassword(password: String): RegisterValidation {
    if (password.isEmpty())
        return RegisterValidation.Failed("Password không được để trống")

    if (password.length < 8)
        return RegisterValidation.Failed("Password phải có ít nhất 8 ký tự")

    if (!password.contains(Regex("[A-Z]")))
        return RegisterValidation.Failed("Password phải có ít nhất 1 chữ HOA")

    if (!password.contains(Regex("[a-z]")))
        return RegisterValidation.Failed("Password phải có ít nhất 1 chữ thường")

    if (!password.contains(Regex("[0-9]")))
        return RegisterValidation.Failed("Password phải có ít nhất 1 chữ SỐ")

    if (!password.contains(Regex("[!@#\$%^&*(),.?\":{}|<>]")))
        return RegisterValidation.Failed("Password phải có ít nhất 1 ký tự đặc biệt (!@#\$%^&*...)")

    return RegisterValidation.Success
}
