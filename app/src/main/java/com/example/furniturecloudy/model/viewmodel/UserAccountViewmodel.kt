package com.example.furniturecloudy.model.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.CloudyApplication
import com.example.furniturecloudy.data.User
import com.example.furniturecloudy.util.RegisterValidation
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.validateEmail
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class UserAccountViewmodel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val store : StorageReference,
    application: Application
) :AndroidViewModel(application)
 {
    private val _user = MutableStateFlow<Resource<User>>(Resource.UnSpecified())
     val user = _user.asStateFlow()

     private val _updateInfo = MutableStateFlow<Resource<User>>(Resource.UnSpecified())
     val updateInfor = _updateInfo.asStateFlow()

     init {
         getUser()
     }

     fun getUser(){
         viewModelScope.launch {
             _user.emit(Resource.Loading())
         }
         firestore.collection("user").document(firebaseAuth.uid!!).get()
             .addOnSuccessListener {
                 val user =it.toObject(User::class.java)
                 user?.let {
                     viewModelScope.launch {
                         _user.emit(Resource.Success(it))
                     }
                 }
             }.addOnFailureListener {
                viewModelScope.launch {
                    _user.emit(Resource.Error(it.message.toString()))
                }
             }
     }

     fun updateUser(user: User,imageUri:Uri?){
         val areInputsValid = validateEmail(user.email) is RegisterValidation.Success && user.firstName.trim().isNotEmpty() && user.lastName.isNotEmpty()
         if (!areInputsValid){
             viewModelScope.launch {
                 _updateInfo.emit(Resource.Error("Điền đầy đủ"))
             }
             return
         }
         viewModelScope.launch {
             _updateInfo.emit(Resource.Loading())
         }
         if (imageUri == null){
             saveUser(user,true)
         }else{
            saveUserWithImage(user,imageUri)
         }

     }

     private fun saveUserWithImage(user: User, imageUri: Uri) {
         viewModelScope.launch {
             try {
                val imageBitmap = MediaStore.Images.Media.getBitmap(getApplication<CloudyApplication>().contentResolver,imageUri)
                 val byteArrayOutputStream = ByteArrayOutputStream()
                 imageBitmap.compress(Bitmap.CompressFormat.JPEG,96,byteArrayOutputStream)
                 val imageByteArray = byteArrayOutputStream.toByteArray()
                 val imageDirectory = store.child("profileImages/${firebaseAuth.uid}/${UUID.randomUUID()}")
                 val result = imageDirectory.putBytes(imageByteArray).await()
                 val imageURL = result.storage.downloadUrl.await().toString()
                 saveUser(user.copy(imagePath = imageURL),false)
             }catch (e : Exception){
                 viewModelScope.launch {
                     _updateInfo.emit(Resource.Error(e.message.toString()))
                 }
             }
         }
     }

     private fun saveUser(user: User,check :Boolean){
         firestore.runTransaction { transition->
             val documentRef = firestore.collection("user").document(firebaseAuth.uid!!)
             if (check){
                 val currentUser = transition.get(documentRef).toObject(User::class.java)
                 val newUser = user.copy(imagePath = currentUser?.imagePath ?: "")
                 transition.set(documentRef,newUser)
             }else{
                 transition.set(documentRef,user)
             }
         }.addOnSuccessListener {
             viewModelScope.launch {
                 _updateInfo.emit(Resource.Success(user))
             }
         }.addOnFailureListener {
             viewModelScope.launch {
                 _updateInfo.emit(Resource.Error("Điền đầy đủ"))
             }
         }
     }
 }