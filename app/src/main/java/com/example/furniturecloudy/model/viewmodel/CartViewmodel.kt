package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.CartProducts
import com.example.furniturecloudy.model.firebase.FirebaseCommon
import com.example.furniturecloudy.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class CartViewmodel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val firebaseCommon: FirebaseCommon
):ViewModel() {
    private val _cartProducts = MutableStateFlow<Resource<List<CartProducts>>>(Resource.UnSpecified())
    val cartProduct = _cartProducts.asStateFlow()
    private var cartProductsDocument = emptyList<DocumentSnapshot>()

    private val _deleteDialog = MutableSharedFlow<CartProducts>()
    val deleteDialog = _deleteDialog.asSharedFlow()

    val productPrice = cartProduct.map {
        when(it){
            is Resource.Success -> {
                caculatePrice(it.data!!)
            }
            else -> null
        }
    }



    init {
        getCartProducts()
    }

    private fun getCartProducts(){
        viewModelScope.launch {
            _cartProducts.emit(Resource.Loading())
        }

        firestore.collection("user").document(firebaseAuth.uid!!).collection("cart").addSnapshotListener { value, error ->
            if (error != null || value == null){
                viewModelScope.launch {
                    _cartProducts.emit(Resource.Error(error?.message.toString()))
                }
            }else{
                cartProductsDocument =value.documents
                val cartProducts = value.toObjects(CartProducts::class.java)
                viewModelScope.launch {
                    _cartProducts.emit(Resource.Success(cartProducts))
                }
            }
        }
    }

    fun ChangeQuantity(cartProducts: CartProducts,status: FirebaseCommon.QuantityStatus){
       val index = _cartProducts.value.data?.indexOf(cartProducts)
           //index có thể bằng -1 nếu người dùng spam tăng và giảm sản phẩm liên tục
        if (index != null && index != -1) {
            val documentId = cartProductsDocument[index].id
            when(status){
                FirebaseCommon.QuantityStatus.INCREASE -> {
                    viewModelScope.launch {
                        _cartProducts.emit(Resource.Loading())
                    }
                    increase(documentId)
                }
                FirebaseCommon.QuantityStatus.DECREASE -> {
                    if(cartProducts.quantity == 1){
                        viewModelScope.launch {
                            _deleteDialog.emit(cartProducts)
                        }
                        return
                    }
                    viewModelScope.launch {
                        _cartProducts.emit(Resource.Loading())
                    }
                    decrease(documentId)
                }
            }
        }

    }

    fun deleteCartProduct(cartProducts: CartProducts){
        val index = cartProduct.value.data?.indexOf(cartProducts)
        if (index != null && index != -1){
            val documentId = cartProductsDocument[index].id
            firestore.collection("user").document(firebaseAuth.uid!!).collection("cart").document(documentId).delete()
        }

    }

    private fun decrease(documentId: String) {
        firebaseCommon.decreaseQuantity(documentId){result,exception ->
            if (exception!=null)
                viewModelScope.launch { _cartProducts.emit(Resource.Error(exception.message.toString())) }
        }
    }

    private fun increase(documentid: String) {
        firebaseCommon.increaseQuantity(documentid){result,exception ->
            if (exception!=null)
                viewModelScope.launch { _cartProducts.emit(Resource.Error(exception.message.toString())) }
        }

    }

    private fun caculatePrice(data: List<CartProducts>): Double {
        var total : Double = 0.0
        data.forEach{
            if (it.product.offerPercentage !=null){
                total = total +(it.product.price*(1-it.product.offerPercentage)*it.quantity).toDouble()
            }else{
                total = total + (it.product.price*it.quantity).toDouble()
            }
        }
        return total
    }



}