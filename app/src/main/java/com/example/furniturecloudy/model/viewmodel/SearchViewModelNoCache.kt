package com.example.furniturecloudy.model.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.data.ProductFilter
import com.example.furniturecloudy.data.SortOption
import com.example.furniturecloudy.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

/**
 * B1 DEMO - BEFORE: SearchViewModel WITHOUT CACHE
 *
 * This version demonstrates the problem of NOT caching data.
 * Every search operation calls Firestore, resulting in:
 * - Slow response time (~2500ms per search)
 * - Excessive network usage
 * - Poor user experience
 *
 * Compare with SearchViewModel (AFTER) which uses in-memory cache.
 */
@HiltViewModel
class SearchViewModelNoCache @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _searchResults = MutableStateFlow<Resource<List<Product>>>(Resource.UnSpecified())
    val searchResults = _searchResults.asStateFlow()

    private var currentSearchQuery = ""
    private var currentFilter = ProductFilter()

    // ❌ NO CACHE - Always fetch from Firestore

    /**
     * Search products WITHOUT cache
     * Every call fetches data from Firestore → SLOW
     */
    fun searchProducts(query: String) {
        currentSearchQuery = query.trim()
        filterAndSortProducts()
    }

    fun applyFilter(filter: ProductFilter) {
        currentFilter = filter
        filterAndSortProducts()
    }

    fun getCurrentFilter(): ProductFilter = currentFilter

    fun isFiltering(): Boolean {
        return currentSearchQuery.isNotEmpty() || currentFilter.isActive()
    }

    /**
     * ❌ BEFORE: Fetch from Firestore EVERY TIME
     * Problem:
     * - Network call on every search
     * - ~2500ms response time
     * - Wastes bandwidth
     */
    private fun filterAndSortProducts() {
        viewModelScope.launch {
            _searchResults.emit(Resource.Loading())

            try {
                val startTime = System.currentTimeMillis()

                // ❌ Call Firestore EVERY TIME - SLOW!
                Log.d("B1_NoCache", "Calling Firestore for query: '$currentSearchQuery'...")

                val snapshot = firestore.collection("Products")
                    .get()
                    .await()

                val allProducts = snapshot.toObjects(Product::class.java)

                val networkTime = System.currentTimeMillis() - startTime
                Log.d("B1_NoCache", "Firestore fetch took: ${networkTime}ms")

                // Filter and sort locally
                val filterStartTime = System.currentTimeMillis()

                var filteredSequence = allProducts.asSequence()

                // Apply search query
                if (currentSearchQuery.isNotEmpty()) {
                    val queryLowerCase = currentSearchQuery.lowercase(Locale.getDefault())
                    filteredSequence = filteredSequence.filter { product ->
                        product.name.lowercase(Locale.getDefault()).contains(queryLowerCase) ||
                                product.category.lowercase(Locale.getDefault()).contains(queryLowerCase)
                    }
                }

                // Apply price filter
                filteredSequence = filteredSequence.filter { product ->
                    val offerPercentage = product.offerPercentage
                    val finalPrice = if (offerPercentage != null) {
                        product.price * (1 - offerPercentage)
                    } else {
                        product.price
                    }
                    finalPrice >= currentFilter.minPrice && finalPrice <= currentFilter.maxPrice
                }

                // Apply stock filter
                if (currentFilter.inStockOnly) {
                    filteredSequence = filteredSequence.filter { it.stock > 0 }
                }

                // Apply sale filter
                if (currentFilter.onSaleOnly) {
                    filteredSequence = filteredSequence.filter {
                        val offer = it.offerPercentage
                        offer != null && offer > 0
                    }
                }

                // Apply sorting
                val filteredList = when (currentFilter.sortBy) {
                    SortOption.PRICE_LOW_TO_HIGH -> filteredSequence.sortedBy {
                        val offer = it.offerPercentage
                        if (offer != null) it.price * (1 - offer) else it.price
                    }.toList()
                    SortOption.PRICE_HIGH_TO_LOW -> filteredSequence.sortedByDescending {
                        val offer = it.offerPercentage
                        if (offer != null) it.price * (1 - offer) else it.price
                    }.toList()
                    SortOption.RATING_HIGH_TO_LOW -> filteredSequence.sortedByDescending { it.averageRating }.toList()
                    SortOption.NAME_A_TO_Z -> filteredSequence.sortedBy { it.name.lowercase() }.toList()
                    SortOption.NAME_Z_TO_A -> filteredSequence.sortedByDescending { it.name.lowercase() }.toList()
                    SortOption.NEWEST, SortOption.NONE -> filteredSequence.toList()
                }

                val filterTime = System.currentTimeMillis() - filterStartTime
                val totalTime = System.currentTimeMillis() - startTime

                Log.d("B1_NoCache", "Filter/sort took: ${filterTime}ms")
                Log.d("B1_NoCache", "❌ TOTAL TIME (NO CACHE): ${totalTime}ms")
                Log.d("B1_NoCache", "Found ${filteredList.size} products")

                _searchResults.emit(Resource.Success(filteredList))

            } catch (e: Exception) {
                Log.e("B1_NoCache", "Error fetching products", e)
                _searchResults.emit(Resource.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun resetSearch() {
        currentSearchQuery = ""
        viewModelScope.launch {
            _searchResults.emit(Resource.UnSpecified())
        }
    }
}
