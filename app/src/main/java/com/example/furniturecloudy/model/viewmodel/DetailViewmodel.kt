package com.example.furniturecloudy.model.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.CartProducts
import com.example.furniturecloudy.model.firebase.FirebaseCommon
import com.example.furniturecloudy.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class DetailViewmodel @Inject constructor(
//    private val firestore: FirebaseFirestore,
//    private val firebaseAuth: FirebaseAuth,
//    private val firebaseCommon: FirebaseCommon
//):ViewModel() {
//    private val _addToCart = MutableStateFlow<Resource<CartProducts>>(Resource.UnSpecified())
//    val addToCart : StateFlow<Resource<CartProducts>> = _addToCart
//
//
//    fun addUpdateProduct(cartProducts: CartProducts){
//        viewModelScope.launch { _addToCart.emit(Resource.Loading()) }
//        firestore.collection("user").document(firebaseAuth.uid!!)
//            .collection("cart").whereEqualTo("product.id",cartProducts.product.id).get()
//            .addOnSuccessListener {
//               it.documents.let {
//                   if (it.isEmpty()){ // Thêm sản phẩm mới
//                            firebaseCommon.addProductToCart(cartProducts){addedProduct,e ->
//                                viewModelScope.launch {
//                                   if (e== null){
//                                        _addToCart.emit(Resource.Success(addedProduct!!))
//                                   }else{
//                                       _addToCart.emit(Resource.Error(e.message.toString()))
//                                   }
//                                }
//                            }
//                   }else{   // Đã có sẵn tăng số lượng
//                            val products = it.first().toObject(cartProducts::class.java)
//                       if (products!!.product == cartProducts.product && products.color == cartProducts.color &&
//                           products.size == cartProducts.size   ){ // Tăng số lượng
//                           val documentId = it.first().id
//                                firebaseCommon.increaseQuantity(documentId){ addedProduct,e ->
//                                    viewModelScope.launch {
//                                        if (e == null){
//                                            _addToCart.emit(Resource.Success(cartProducts!!))
//                                        }else{
//                                            _addToCart.emit(Resource.Error(e.message.toString()))
//                                        }
//                                    }
//                                }
//                       }else{ // Thêm mới (Màu sắc hoặc size)
//                           firebaseCommon.addProductToCart(cartProducts){addedProduct,e ->
//                               viewModelScope.launch {
//                                   if (e== null){
//                                       _addToCart.emit(Resource.Success(addedProduct!!))
//                                   }else{
//                                       _addToCart.emit(Resource.Error(e.message.toString()))
//                                   }
//                               }
//                           }
//                       }
//                   }
//               }
//            }.addOnFailureListener {
//                viewModelScope.launch {
//                    _addToCart.emit(Resource.Error(it.message.toString()))
//                }
//            }
//    }
//
//    private fun addNewProduct(cartProduct: CartProducts) {
//        firebaseCommon.addProductToCart(cartProduct) { addedProduct, e ->
//            viewModelScope.launch {
//                if (e == null)
//                    _addToCart.emit(Resource.Success(addedProduct!!))
//                else
//                    _addToCart.emit(Resource.Error(e.message.toString()))
//            }
//        }
//    }
//
//    private fun increaseQuantity(documentId: String, cartProduct: CartProducts) {
//        firebaseCommon.increaseQuantity(documentId) { _, e ->
//            viewModelScope.launch {
//                if (e == null)
//                    _addToCart.emit(Resource.Success(cartProduct))
//                else
//                    _addToCart.emit(Resource.Error(e.message.toString()))
//            }
//        }
//    }
//
//}
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val firebaseCommon: FirebaseCommon
) : ViewModel() {

    private val _addToCart = MutableStateFlow<Resource<CartProducts>>(Resource.UnSpecified())
    val addToCart = _addToCart.asStateFlow()

    fun addUpdateProduct(cartProduct: CartProducts) {
        // Check stock availability
        if (cartProduct.product.stock <= 0) {
            viewModelScope.launch {
                _addToCart.emit(Resource.Error("Sản phẩm đã hết hàng"))
            }
            return
        }

        viewModelScope.launch { _addToCart.emit(Resource.Loading()) }
        firestore.collection("user").document(auth.uid!!).collection("cart")
            .whereEqualTo("product.id", cartProduct.product.id).get()
            .addOnSuccessListener {
                it.documents.let {
                    if (it.isEmpty()) { //Add new product
                        addNewProduct(cartProduct)
                    } else {
                        val product = it.first().toObject(CartProducts::class.java)

                        if(product!!.product == cartProduct.product && product.color == cartProduct.color && product.size== cartProduct.size){ //Increase the quantity
                            Log.d("Loidebug","Thêm vào sẵn ")
                            val documentId = it.first().id

                            // Check if increasing quantity exceeds stock
                            val newQuantity = product.quantity + 1
                            if (newQuantity > cartProduct.product.stock) {
                                viewModelScope.launch {
                                    _addToCart.emit(Resource.Error("Không đủ hàng trong kho. Chỉ còn ${cartProduct.product.stock} sản phẩm"))
                                }
                            } else {
                                increaseQuantity(documentId, cartProduct)
                            }
                        } else { //Add new product
                            Log.d("Loidebug","Thêm vào sẵn lỗi ")
                            addNewProduct(cartProduct)
                        }
                    }
                }
            }.addOnFailureListener {
                viewModelScope.launch { _addToCart.emit(Resource.Error(it.message.toString())) }
            }
    }

    private fun addNewProduct(cartProduct: CartProducts) {
        // Check if quantity to add exceeds stock
        if (cartProduct.quantity > cartProduct.product.stock) {
            viewModelScope.launch {
                _addToCart.emit(Resource.Error("Không đủ hàng trong kho. Chỉ còn ${cartProduct.product.stock} sản phẩm"))
            }
            return
        }

        firebaseCommon.addProductToCart(cartProduct) { addedProduct, e ->
            viewModelScope.launch {
                if (e == null)
                    _addToCart.emit(Resource.Success(addedProduct!!))
                else
                    _addToCart.emit(Resource.Error(e.message.toString()))
            }
        }
    }

    private fun increaseQuantity(documentId: String, cartProduct: CartProducts) {
        firebaseCommon.increaseQuantity(documentId) { _, e ->
            viewModelScope.launch {
                if (e == null)
                    _addToCart.emit(Resource.Success(cartProduct))
                else
                    _addToCart.emit(Resource.Error(e.message.toString()))
            }
        }
    }
}
