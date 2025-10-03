package com.example.furniturecloudy.database.repository

import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.database.dao.RecentlyViewedDao
import com.example.furniturecloudy.database.entity.RecentlyViewed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentlyViewedRepository @Inject constructor(
    private val recentlyViewedDao: RecentlyViewedDao
) {

    fun getRecentlyViewedProducts(): Flow<List<Product>> {
        return recentlyViewedDao.getRecentlyViewed().map { viewedList ->
            viewedList.map { it.toProduct() }
        }
    }

    suspend fun addRecentlyViewed(product: Product) {
        val recentlyViewed = RecentlyViewed(
            productId = product.id,
            productName = product.name,
            productPrice = product.price,
            productImage = product.images.firstOrNull() ?: "",
            productCategory = product.category,
            productStock = product.stock,
            offerPercentage = product.offerPercentage
        )
        recentlyViewedDao.insertViewed(recentlyViewed)

        // Keep only last 10 items
        val count = recentlyViewedDao.getCount()
        if (count > 10) {
            // Delete items older than 30 days
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            recentlyViewedDao.deleteOldViews(thirtyDaysAgo)
        }
    }

    suspend fun clearAll() {
        recentlyViewedDao.clearAll()
    }

    private fun RecentlyViewed.toProduct(): Product {
        return Product(
            id = productId,
            name = productName,
            category = productCategory,
            price = productPrice,
            offerPercentage = offerPercentage,
            description = null,
            colors = null,
            sizes = null,
            images = listOf(productImage),
            stock = productStock
        )
    }
}
