package com.example.furniturecloudy.model.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.furniturecloudy.data.User
import com.example.furniturecloudy.util.Constants.USER_COLLECTION
import com.example.furniturecloudy.util.RegisterFieldsState
import com.example.furniturecloudy.util.RegisterValidation
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.validateEmail
import com.example.furniturecloudy.util.validatePassword
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class RegisterViewmodel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
): ViewModel(){

    private val _register = MutableStateFlow<Resource<User>>(Resource.UnSpecified())
    val register : Flow<Resource<User>> = _register
    private val _validation = Channel<RegisterFieldsState>()
    val validation = _validation.receiveAsFlow()
    fun createAccountWithEmailAndPassword(user: User,password:String){
        if (checkValidation(user.email,password)) {
            runBlocking {
                _register.emit(Resource.Loading())
            }
            firebaseAuth.createUserWithEmailAndPassword(user.email, password)
                .addOnSuccessListener {
                    it.user?.let {
                        saveUserInformation(it.uid,user)
                       // _register.value = Resource.Success(it)
                    }
                }
                .addOnFailureListener {
                    _register.value = Resource.Error("Khong tao duoc account")
                }

        }else{
            val registerFieldsState = RegisterFieldsState(validateEmail(user.email),validatePassword(password))
            runBlocking {
                _validation.send(registerFieldsState)
            }
        }
    }

    private fun saveUserInformation(userID:String,user: User) {
        firestore.collection(USER_COLLECTION)
            .document(userID)
            .set(user)
            .addOnSuccessListener {
                _register.value = Resource.Success(user)
            }.addOnFailureListener {
                _register.value = Resource.Error("Lưu user vào firestore thất bại")
            }
    }

    private fun checkValidation(email:String,password:String):Boolean{
        val emailValidation = validateEmail(email)
        val passwordValidation = validatePassword(password)
        val shouldRegister = emailValidation is RegisterValidation.Success && passwordValidation is RegisterValidation.Success
        return shouldRegister
    }
}