package com.example.furniturecloudy.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: String,
    val name: String,
    val category: String,
    val price: Float,
    val offerPercentage: Float? = null,
    val description: String? = null,
    val colors: List<Int>? = null,
    val sizes: List<String>? = null,
    val images: List<String>,
    val stock: Int = 0, // Available stock quantity
    val averageRating: Float = 0f, // Cached average rating
    val reviewCount: Int = 0 // Cached review count
): Parcelable{
    constructor(): this ("0","","",0f,images = emptyList(), stock = 0, averageRating = 0f, reviewCount = 0)
}