package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.util.Resource
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewmodel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _resetPassword = MutableStateFlow<Resource<String>>(Resource.UnSpecified())
    val resetPassword = _resetPassword.asStateFlow()

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _resetPassword.emit(Resource.Loading())
        }

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                viewModelScope.launch {
                    _resetPassword.emit(Resource.Success(email))
                }
            }
            .addOnFailureListener { exception ->
                viewModelScope.launch {
                    _resetPassword.emit(Resource.Error(exception.message.toString()))
                }
            }
    }
}
