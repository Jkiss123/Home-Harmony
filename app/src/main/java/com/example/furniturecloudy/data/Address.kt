package com.example.furniturecloudy.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Address(
    val fullName : String,
    val phone : String, // vì có số 0 ở đầu set string cho tiện
    val wards : String,
    val district : String,
    val city: String,
    val addressFull : String
) : Parcelable{
    constructor() : this("","","","","","")
}
