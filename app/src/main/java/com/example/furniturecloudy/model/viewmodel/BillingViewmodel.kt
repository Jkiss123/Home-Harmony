package com.example.furniturecloudy.model.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.Address
import com.example.furniturecloudy.util.AddressEncryptionHelper
import com.example.furniturecloudy.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewmodel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    application: Application
) :ViewModel(){

    // Encryption helper for decrypting sensitive address data
    private val encryptionHelper = AddressEncryptionHelper(application)
    private val _address = MutableStateFlow<Resource<List<Address>>>(Resource.UnSpecified())
    val address = _address.asStateFlow()

    init {
        getUserAddresses()
    }

    fun getUserAddresses() {
      viewModelScope.launch {
          _address.emit(Resource.Loading())
      }
        firestore.collection("user").document(firebaseAuth.uid!!).collection("address")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    viewModelScope.launch { _address.emit(Resource.Error(error.message.toString())) }
                    return@addSnapshotListener
                }
                val encryptedAddresses = value?.toObjects(Address::class.java) ?: emptyList()

                // Decrypt sensitive fields after reading from Firestore
                val decryptedAddresses = encryptionHelper.decryptAddresses(encryptedAddresses)

                viewModelScope.launch {
                    _address.emit(Resource.Success(decryptedAddresses))
                }
            }
    }
}