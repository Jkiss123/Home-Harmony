package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.User
import com.example.furniturecloudy.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewmodel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) :ViewModel() {
    private val _user = MutableStateFlow<Resource<User>>(Resource.UnSpecified())
    val user = _user.asStateFlow()

    init {
        getUser()
    }

    fun getUser(){
        viewModelScope.launch {
            _user.emit(Resource.Loading())
        }

        firestore.collection("user").document(firebaseAuth.uid!!).addSnapshotListener { value, error ->
            if (error != null) {
                viewModelScope.launch {
                    _user.emit(Resource.Error(error.message.toString()))
                }
            }else{
                val user = value?.toObject(User::class.java)
                user?.let {
                    viewModelScope.launch {
                        _user.emit(Resource.Success(user))
                    }
                }
            }
        }
    }

    fun logOut(){
        firebaseAuth.signOut()
    }


}