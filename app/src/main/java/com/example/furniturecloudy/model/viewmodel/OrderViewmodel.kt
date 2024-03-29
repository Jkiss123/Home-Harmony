package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.Order
import com.example.furniturecloudy.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewmodel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : ViewModel(){
    private val _order = MutableStateFlow<Resource<Order>>(Resource.UnSpecified())
    val order = _order.asStateFlow()

    fun placeOrder(order: Order){
        viewModelScope.launch {
            _order.emit(Resource.Loading())
        }
        firestore.runBatch {batch ->
            // Thêm order vào user collection
            firestore.collection("user").document(firebaseAuth.uid!!).collection("orders").document().set(order)
            // thêm order vào order cllection
            firestore.collection("orders").document().set(order)
            // Xóa hết sản phẩm trong cart

            firestore.collection("user").document(firebaseAuth.uid!!).collection("cart").get()
                .addOnSuccessListener {
                    it.documents.forEach {
                        it.reference.delete()
                    }
                }

        }.addOnSuccessListener {
            viewModelScope.launch {
                _order.emit(Resource.Success(order))
            }
        }.addOnFailureListener {
            viewModelScope.launch {
                _order.emit(Resource.Error(it.message.toString()))
            }
        }
    }
}