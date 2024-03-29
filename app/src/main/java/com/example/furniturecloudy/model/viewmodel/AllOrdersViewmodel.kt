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
class AllOrdersViewmodel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
):ViewModel() {
    private val _allOrder = MutableStateFlow<Resource<List<Order>>>(Resource.UnSpecified())
    val allOrder = _allOrder.asStateFlow()

    init {
        getAllOrder()
    }

    fun getAllOrder(){
        viewModelScope.launch {
            _allOrder.emit(Resource.Loading())
        }

        firestore.collection("user").document(firebaseAuth.uid!!).collection("orders").get()
            .addOnSuccessListener {
                val orders = it.toObjects(Order::class.java)
                viewModelScope.launch {
                    _allOrder.emit(Resource.Success(orders))
                }
            }.addOnFailureListener {
                viewModelScope.launch {
                    _allOrder.emit(Resource.Error(it.message.toString()))
                }
            }
    }
}