package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.Category
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BaseCategoryViewmodel @AssistedInject constructor(
    private val firestore: FirebaseFirestore,
    @Assisted private val category: Category
):ViewModel() {

    private val _offerProducts = MutableStateFlow<Resource<List<Product>>>(Resource.UnSpecified())
    val offerProducts : StateFlow<Resource<List<Product>>> = _offerProducts
    private val _bestProducts = MutableStateFlow<Resource<List<Product>>>(Resource.UnSpecified())
    val bestProducts : StateFlow<Resource<List<Product>>> = _bestProducts

    init {
        fetchOfferProducts()
        fetchBestProducts()
    }
    fun fetchOfferProducts(){
        viewModelScope.launch {
            _offerProducts.emit(Resource.Loading())
        }
        firestore.collection("Products").whereEqualTo("category",category.category).whereNotEqualTo("offerPercentage",null).get()
            .addOnSuccessListener {results->
                val offerProductsList = results.toObjects(Product::class.java)
                viewModelScope.launch {
                    _offerProducts.emit(Resource.Success(offerProductsList))
                }
            }
            .addOnFailureListener {
                viewModelScope.launch {
                    _offerProducts.emit(Resource.Error(it.message.toString()))
                }
            }
    }

    fun fetchBestProducts(){
        viewModelScope.launch {
            _bestProducts.emit(Resource.Loading())
        }
        firestore.collection("Products").whereEqualTo("category",category.category).get()
            .addOnSuccessListener {result->
                val bestProductsList = result.toObjects(Product::class.java)
                viewModelScope.launch {
                    _bestProducts.emit(Resource.Success(bestProductsList))
                }
            }.addOnFailureListener {
                viewModelScope.launch {
                    _bestProducts.emit(Resource.Error(it.message.toString()))
                }
            }
    }


    @AssistedFactory
    interface Factory{
        fun create(category: Category) : BaseCategoryViewmodel
    }

    companion object{
        fun prodieCateforyViewModelFactory(factory: Factory,category: Category) : ViewModelProvider.Factory{
            return object : ViewModelProvider.Factory{
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return factory.create(category) as T
                }
            }
        }
    }
}