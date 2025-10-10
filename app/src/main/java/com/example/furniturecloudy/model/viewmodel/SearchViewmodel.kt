package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.PagingInfo
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.data.ProductFilter
import com.example.furniturecloudy.data.SortOption
import com.example.furniturecloudy.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SearchViewmodel @Inject constructor(private val firestore: FirebaseFirestore
):ViewModel() {
    private val _products = MutableStateFlow<Resource<List<Product>>>(Resource.UnSpecified())
    val products = _products.asStateFlow()

    private val _searchResults = MutableStateFlow<Resource<List<Product>>>(Resource.UnSpecified())
    val searchResults = _searchResults.asStateFlow()

    private val pagingInfo  = PagingInfo()
    private var allProducts = mutableListOf<Product>()
    private var currentSearchQuery = ""
    private var currentFilter = ProductFilter()

    init {
        getProducts()
    }

    fun getProducts(){
        if (!pagingInfo.isPagingEnd){
            viewModelScope.launch {
                _products.emit(Resource.Loading())
            }

            var query = firestore.collection("Products")
                .limit(10)

            // Use startAfter for pagination
            pagingInfo.lastVisibleDocument?.let {
                query = query.startAfter(it)
            }

            query.get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        pagingInfo.isPagingEnd = true
                        viewModelScope.launch {
                            _products.emit(Resource.Success(allProducts))
                        }
                        return@addOnSuccessListener
                    }

                    val productList = querySnapshot.toObjects(Product::class.java)
                    allProducts.addAll(productList)

                    // Store last visible document for pagination
                    pagingInfo.lastVisibleDocument = querySnapshot.documents.lastOrNull()

                    // Check if we've reached the end
                    if (productList.size < 10) {
                        pagingInfo.isPagingEnd = true
                    }

                    viewModelScope.launch {
                        _products.emit(Resource.Success(allProducts.toList()))

                        // If there's an active search or filter, reapply them
                        if (currentSearchQuery.isNotEmpty() || currentFilter.isActive()) {
                            filterAndSortProducts()
                        }
                    }

                    pagingInfo.page++
                }.addOnFailureListener {
                    viewModelScope.launch {
                        _products.emit(Resource.Error(it.message.toString()))
                    }
                }
        }
    }

    fun searchProducts(query: String) {
        currentSearchQuery = query.trim()

        if (currentSearchQuery.isEmpty()) {
            // If query is empty, show all products
            viewModelScope.launch {
                _searchResults.emit(Resource.Success(allProducts.toList()))
            }
            return
        }

        viewModelScope.launch {
            _searchResults.emit(Resource.Loading())
        }

        // Filter locally for better UX
        filterProducts(currentSearchQuery)
    }

    private fun filterProducts(query: String) {
        filterAndSortProducts()
    }

    fun resetSearch() {
        currentSearchQuery = ""
        viewModelScope.launch {
            _searchResults.emit(Resource.UnSpecified())
        }
    }

    fun resetPagination() {
        pagingInfo.page = 1
        pagingInfo.isPagingEnd = false
        pagingInfo.lastVisibleDocument = null
        pagingInfo.oldProduct = emptyList()
        allProducts.clear()
    }

    fun applyFilter(filter: ProductFilter) {
        currentFilter = filter
        filterAndSortProducts()
    }

    fun getCurrentFilter(): ProductFilter = currentFilter

    fun isFiltering(): Boolean {
        return currentSearchQuery.isNotEmpty() || currentFilter.isActive()
    }

    private fun filterAndSortProducts() {
        viewModelScope.launch {
            _searchResults.emit(Resource.Loading())
        }

        var filteredList = allProducts.toList()

        // Apply search query
        if (currentSearchQuery.isNotEmpty()) {
            val queryLowerCase = currentSearchQuery.lowercase(Locale.getDefault())
            filteredList = filteredList.filter { product ->
                product.name.lowercase(Locale.getDefault()).contains(queryLowerCase) ||
                        product.category.lowercase(Locale.getDefault()).contains(queryLowerCase)
            }
        }

        // Apply price filter
        filteredList = filteredList.filter { product ->
            val finalPrice = if (product.offerPercentage != null) {
                product.price * (1 - product.offerPercentage)
            } else {
                product.price
            }
            finalPrice >= currentFilter.minPrice && finalPrice <= currentFilter.maxPrice
        }

        // Apply stock filter
        if (currentFilter.inStockOnly) {
            filteredList = filteredList.filter { it.stock > 0 }
        }

        // Apply sale filter
        if (currentFilter.onSaleOnly) {
            filteredList = filteredList.filter { it.offerPercentage != null && it.offerPercentage > 0 }
        }

        // Apply sorting
        filteredList = when (currentFilter.sortBy) {
            SortOption.PRICE_LOW_TO_HIGH -> filteredList.sortedBy {
                if (it.offerPercentage != null) it.price * (1 - it.offerPercentage) else it.price
            }
            SortOption.PRICE_HIGH_TO_LOW -> filteredList.sortedByDescending {
                if (it.offerPercentage != null) it.price * (1 - it.offerPercentage) else it.price
            }
            SortOption.RATING_HIGH_TO_LOW -> filteredList.sortedByDescending { it.averageRating }
            SortOption.NAME_A_TO_Z -> filteredList.sortedBy { it.name.lowercase() }
            SortOption.NAME_Z_TO_A -> filteredList.sortedByDescending { it.name.lowercase() }
            SortOption.NEWEST, SortOption.NONE -> filteredList
        }

        viewModelScope.launch {
            _searchResults.emit(Resource.Success(filteredList))
        }
    }

}