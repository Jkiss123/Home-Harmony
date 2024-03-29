package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.Address
import com.example.furniturecloudy.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddressViewmodel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
):ViewModel(){
    private val _addNewAddress = MutableStateFlow<Resource<Address>>(Resource.UnSpecified())
    val addNewAddress = _addNewAddress.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    fun addAddress(address: Address){

        if (validateInputs(address)){
            viewModelScope.launch {
                _addNewAddress.emit(Resource.Loading())
            }
            firestore.collection("user").document(firebaseAuth.uid!!).collection("address").document()
                .set(address).addOnSuccessListener {
                    viewModelScope.launch {
                        _addNewAddress.emit(Resource.Success(address))
                    }
                }.addOnFailureListener {
                    viewModelScope.launch {
                        _addNewAddress.emit(Resource.Error(it.message.toString()))
                    }
                }
        }else{
            viewModelScope.launch {
                _error.emit("Điền đầy đủ")
            }
        }
    }





    private fun validateInputs(address: Address): Boolean {
        return address.addressFull.trim().isNotEmpty() &&
                address.city.trim().isNotEmpty() &&
                address.phone.trim().isNotEmpty() &&
                address.wards.trim().isNotEmpty() &&
                address.fullName.trim().isNotEmpty() &&
                address.district.trim().isNotEmpty()
    }
}