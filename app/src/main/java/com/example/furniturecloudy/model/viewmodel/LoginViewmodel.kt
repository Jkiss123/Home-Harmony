package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.User
import com.example.furniturecloudy.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class LoginViewmodel @Inject constructor(
    private  val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) :ViewModel(){
    private val _login = MutableSharedFlow<Resource<FirebaseUser>>()
    val login  = _login.asSharedFlow()

    fun loginAccount(email:String,password:String){
        viewModelScope.launch {
            _login.emit(Resource.Loading())
        }
        firebaseAuth.signInWithEmailAndPassword(email,password).addOnSuccessListener {
                viewModelScope.launch {
                    it.user?.let {
                        _login.emit(Resource.Success(it))
                    }
                }
        }.addOnFailureListener {
                viewModelScope.launch {
                    _login.emit(Resource.Error(it.message.toString()))
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _login.emit(Resource.Loading())
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                firebaseUser?.let { user ->
                    // Check if user document exists in Firestore
                    firestore.collection("user").document(user.uid).get()
                        .addOnSuccessListener { document ->
                            if (!document.exists()) {
                                // Create new user document for Google sign-in users
                                val newUser = User(
                                    firstName = user.displayName?.split(" ")?.firstOrNull() ?: "",
                                    lastName = user.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "",
                                    email = user.email ?: "",
                                    imagePath = user.photoUrl?.toString() ?: ""
                                )
                                firestore.collection("user").document(user.uid).set(newUser)
                                    .addOnSuccessListener {
                                        viewModelScope.launch {
                                            _login.emit(Resource.Success(user))
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        viewModelScope.launch {
                                            _login.emit(Resource.Error(e.message.toString()))
                                        }
                                    }
                            } else {
                                viewModelScope.launch {
                                    _login.emit(Resource.Success(user))
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            viewModelScope.launch {
                                _login.emit(Resource.Error(e.message.toString()))
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                viewModelScope.launch {
                    _login.emit(Resource.Error(e.message.toString()))
                }
            }
    }
}