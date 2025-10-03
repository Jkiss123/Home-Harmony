package com.example.furniturecloudy.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Address(
    val fullName : String,
    val phone : String,
    val wards : String,
    val district : String,
    val city: String,
    val addressFull : String,
    val id: String = ""
) : Parcelable{
    constructor() : this("","","","","","","")
}
