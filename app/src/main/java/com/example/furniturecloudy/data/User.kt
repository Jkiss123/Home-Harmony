package com.example.furniturecloudy.data

data class User(
    val firstName:String,
    val lastName:String,
    val email:String,
    var imagePath:String
){
    constructor() : this("","","","")
}
