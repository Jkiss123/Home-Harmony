package com.example.furniturecloudy.model.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.R
import com.example.furniturecloudy.util.Constants.INTRODUCTION_KEY
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class IntroductionViewmodel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val firebaseAuth: FirebaseAuth
):ViewModel() {
    private val _navigate = MutableStateFlow(0)
    val navigate :StateFlow<Int> = _navigate
    companion object{
        const val SHOPPING_ACTIVITY = 29
        const val ACCOUNT_OPTION_FRAGMENT = 28
    }
    init {
        val isButtonClicked = sharedPreferences.getBoolean(INTRODUCTION_KEY,false)
        if (firebaseAuth.currentUser!=null){
                viewModelScope.launch {
                    _navigate.emit(SHOPPING_ACTIVITY)
                }
        }else if(isButtonClicked){
                viewModelScope.launch {
                    _navigate.emit(ACCOUNT_OPTION_FRAGMENT)
                }
        }else{
            Unit
        }
    }

    fun startButtonClicked(){
        sharedPreferences.edit().putBoolean(INTRODUCTION_KEY,true).apply()
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}