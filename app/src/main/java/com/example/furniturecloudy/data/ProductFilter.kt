package com.example.furniturecloudy.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductFilter(
    val minPrice: Float = 0f,
    val maxPrice: Float = Float.MAX_VALUE,
    val categories: List<String> = emptyList(),
    val colors: List<Int> = emptyList(),
    val sizes: List<String> = emptyList(),
    val inStockOnly: Boolean = false,
    val onSaleOnly: Boolean = false,
    val sortBy: SortOption = SortOption.NONE
) : Parcelable {
    fun isActive(): Boolean {
        return minPrice > 0f ||
                maxPrice < Float.MAX_VALUE ||
                categories.isNotEmpty() ||
                colors.isNotEmpty() ||
                sizes.isNotEmpty() ||
                inStockOnly ||
                onSaleOnly ||
                sortBy != SortOption.NONE
    }

    fun getActiveFilterCount(): Int {
        var count = 0
        if (minPrice > 0f || maxPrice < Float.MAX_VALUE) count++
        if (categories.isNotEmpty()) count++
        if (colors.isNotEmpty()) count++
        if (sizes.isNotEmpty()) count++
        if (inStockOnly) count++
        if (onSaleOnly) count++
        if (sortBy != SortOption.NONE) count++
        return count
    }
}

@Parcelize
enum class SortOption : Parcelable {
    NONE,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    RATING_HIGH_TO_LOW,
    NEWEST,
    NAME_A_TO_Z,
    NAME_Z_TO_A;

    fun getDisplayName(): String {
        return when (this) {
            NONE -> "Mặc định"
            PRICE_LOW_TO_HIGH -> "Giá: Thấp đến cao"
            PRICE_HIGH_TO_LOW -> "Giá: Cao đến thấp"
            RATING_HIGH_TO_LOW -> "Đánh giá cao nhất"
            NEWEST -> "Mới nhất"
            NAME_A_TO_Z -> "Tên: A-Z"
            NAME_Z_TO_A -> "Tên: Z-A"
        }
    }
}
