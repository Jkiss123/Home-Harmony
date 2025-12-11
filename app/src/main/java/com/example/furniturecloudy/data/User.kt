package com.example.furniturecloudy.data

data class User(
    val firstName:String,
    val lastName:String,
    val email:String,
    var imagePath:String,
    // Two-Factor Authentication fields
    val twoFactorEnabled: Boolean = true,  // Force 2FA for all users
    val twoFactorMethod: String = "email"  // Currently only email OTP supported
){
    constructor() : this("","","","", true, "email")
}
