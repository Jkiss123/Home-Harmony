package com.example.furniturecloudy.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recently_viewed")
data class RecentlyViewed(
    @PrimaryKey
    val productId: String,
    val productName: String,
    val productPrice: Float,
    val productImage: String,
    val productCategory: String,
    val productStock: Int,
    val offerPercentage: Float?,
    val viewedAt: Long = System.currentTimeMillis()
)
