package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.PagingInfo
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainCategoryViewmodel @Inject constructor(private val firestore: FirebaseFirestore
):ViewModel(){
    private val _specialProducts = MutableStateFlow<Resource<List<Product>>>(Resource.UnSpecified())
    val specialProducts : StateFlow<Resource<List<Product>>> = _specialProducts

    private val _bestProducts = MutableStateFlow<Resource<List<Product>>>(Resource.UnSpecified())
    val bestProducts : StateFlow<Resource<List<Product>>> = _bestProducts

    private val _bestDeals = MutableStateFlow<Resource<List<Product>>>(Resource.UnSpecified())
    val bestDeals : StateFlow<Resource<List<Product>>> = _bestDeals

    private val pagingInfo  = PagingInfo()
    init {
        fetchSpecialProductsI()
        fetchBestDeals()
        fetchBestProducts()

    }

    fun fetchSpecialProductsI(){
            viewModelScope.launch {
                _specialProducts.emit(Resource.Loading())
            }

            firestore.collection("Products").whereEqualTo("category","Special Products").get().addOnSuccessListener { result ->
                val specialProductsList = result.toObjects(Product::class.java)
                viewModelScope.launch {
                    _specialProducts.emit(Resource.Success(specialProductsList))
                }

            }.addOnFailureListener {
                viewModelScope.launch {
                    _specialProducts.emit(Resource.Error(it.message.toString()))
                }
            }

    }
    fun fetchBestProducts() {
        if (!pagingInfo.isPagingEnd) {
            viewModelScope.launch {
                _bestProducts.emit(Resource.Loading())
            }

            firestore.collection("Products").limit(pagingInfo.page * 10).get()
                .addOnSuccessListener { result ->
                    val bestProdutsList = result.toObjects(Product::class.java)
                    pagingInfo.isPagingEnd = bestProdutsList == pagingInfo.oldProduct
                    pagingInfo.oldProduct = bestProdutsList
                    viewModelScope.launch {
                        _bestProducts.emit(Resource.Success(bestProdutsList))
                    }
                    pagingInfo.page++
                }.addOnFailureListener {
                viewModelScope.launch {
                    _bestProducts.emit(Resource.Error(it.message.toString()))
                }
            }
        }
    }

    fun fetchBestDeals(){
        viewModelScope.launch {
            _bestDeals.emit(Resource.Loading())
        }

        firestore.collection("Products").whereEqualTo("category","Best Deals").get().addOnSuccessListener { result ->
            val bestDealsList = result.toObjects(Product::class.java)
            viewModelScope.launch {
                _bestDeals.emit(Resource.Success(bestDealsList))
            }

        }.addOnFailureListener {
            viewModelScope.launch {
                _bestDeals.emit(Resource.Error(it.message.toString()))
            }
        }
    }
}