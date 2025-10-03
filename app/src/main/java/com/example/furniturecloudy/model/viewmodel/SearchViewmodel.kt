package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.PagingInfo
import com.example.furniturecloudy.data.Product
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

                        // If there's an active search, filter the results
                        if (currentSearchQuery.isNotEmpty()) {
                            filterProducts(currentSearchQuery)
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
        val queryLowerCase = query.lowercase(Locale.getDefault())

        val filteredList = allProducts.filter { product ->
            product.name?.lowercase(Locale.getDefault())?.contains(queryLowerCase) == true ||
            product.category?.lowercase(Locale.getDefault())?.contains(queryLowerCase) == true
        }

        viewModelScope.launch {
            _searchResults.emit(Resource.Success(filteredList))
        }
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

}