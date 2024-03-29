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
import javax.inject.Inject

@HiltViewModel
class SearchViewmodel @Inject constructor(private val firestore: FirebaseFirestore
):ViewModel() {
    private val _products = MutableStateFlow<Resource<List<Product>>>(Resource.UnSpecified())
    val products = _products.asStateFlow()
    private val pagingInfo  = PagingInfo()

    init {
        getProducts()
    }

    fun getProducts(){
        if (!pagingInfo.isPagingEnd){
            viewModelScope.launch {
                _products.emit(Resource.Loading())
            }
            firestore.collection("Products").limit(pagingInfo.page * 10).get()
                .addOnSuccessListener {
                    val productList = it.toObjects(Product::class.java)
                    pagingInfo.isPagingEnd = productList == pagingInfo.oldProduct
                    pagingInfo.oldProduct = productList
                    viewModelScope.launch {
                        _products.emit(Resource.Success(productList))
                    }
                    pagingInfo.page++
                }.addOnFailureListener {
                    viewModelScope.launch {
                        _products.emit(Resource.Error(it.message.toString()))
                    }
                }
        }
    }

}