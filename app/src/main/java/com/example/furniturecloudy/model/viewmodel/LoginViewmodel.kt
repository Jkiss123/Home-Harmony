package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.LoginAttempt
import com.example.furniturecloudy.data.User
import com.example.furniturecloudy.util.Resource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    /**
     * Check if account is locked before allowing login
     * @return LoginAttempt if account is locked, null otherwise
     */
    private suspend fun checkAccountLock(email: String): LoginAttempt? {
        return try {
            val doc = firestore.collection(LOGIN_ATTEMPTS_COLLECTION)
                .document(email)
                .get()
                .await()

            if (doc.exists()) {
                val attempt = doc.toObject(LoginAttempt::class.java)
                if (attempt != null && attempt.isCurrentlyLocked()) {
                    return attempt
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Track failed login attempt and lock account if necessary
     */
    private suspend fun trackFailedLogin(email: String) {
        try {
            val docRef = firestore.collection(LOGIN_ATTEMPTS_COLLECTION).document(email)
            val doc = docRef.get().await()

            val currentAttempt = if (doc.exists()) {
                doc.toObject(LoginAttempt::class.java) ?: LoginAttempt(email = email)
            } else {
                LoginAttempt(email = email)
            }

            // Check if previous lock has expired
            val isStillLocked = currentAttempt.isCurrentlyLocked()

            val newFailedCount = if (isStillLocked) {
                currentAttempt.failedCount
            } else {
                currentAttempt.failedCount + 1
            }

            // Lock account if max attempts reached
            val shouldLock = newFailedCount >= LoginAttempt.MAX_ATTEMPTS

            val updatedAttempt = LoginAttempt(
                email = email,
                failedCount = newFailedCount,
                lastFailedAt = Timestamp.now(),
                lockedUntil = if (shouldLock) {
                    // Lock for 15 minutes
                    Timestamp(
                        Timestamp.now().seconds + (LoginAttempt.LOCKOUT_DURATION_MINUTES * 60),
                        0
                    )
                } else {
                    currentAttempt.lockedUntil
                },
                isLocked = shouldLock || isStillLocked
            )

            docRef.set(updatedAttempt).await()
        } catch (e: Exception) {
            // Log error but don't block login flow
            android.util.Log.e("LoginViewmodel", "Failed to track login attempt", e)
        }
    }

    /**
     * Reset failed login attempts after successful login
     */
    private suspend fun resetLoginAttempts(email: String) {
        try {
            firestore.collection(LOGIN_ATTEMPTS_COLLECTION)
                .document(email)
                .delete()
                .await()
        } catch (e: Exception) {
            // Log error but don't block login flow
            android.util.Log.e("LoginViewmodel", "Failed to reset login attempts", e)
        }
    }

    /**
     * Enhanced login with account lockout protection
     */
    fun loginAccountWithProtection(email: String, password: String) {
        viewModelScope.launch {
            _login.emit(Resource.Loading())

            // Check if account is locked
            val lockedAttempt = checkAccountLock(email)
            if (lockedAttempt != null) {
                val remainingSeconds = lockedAttempt.getRemainingLockoutSeconds()
                val remainingMinutes = (remainingSeconds / 60).toInt()
                _login.emit(
                    Resource.Error(
                        "Tài khoản đã bị khóa do đăng nhập sai quá nhiều lần. " +
                        "Vui lòng thử lại sau $remainingMinutes phút."
                    )
                )
                return@launch
            }

            // Proceed with login
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    viewModelScope.launch {
                        it.user?.let { user ->
                            // Reset login attempts on successful login
                            resetLoginAttempts(email)
                            _login.emit(Resource.Success(user))
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    viewModelScope.launch {
                        // Track failed attempt
                        trackFailedLogin(email)

                        // Get updated attempt info to show remaining attempts
                        val updatedAttempt = checkAccountLock(email)
                        val message = if (updatedAttempt != null && updatedAttempt.isLocked) {
                            val remainingSeconds = updatedAttempt.getRemainingLockoutSeconds()
                            val remainingMinutes = (remainingSeconds / 60).toInt()
                            "Tài khoản đã bị khóa do đăng nhập sai quá nhiều lần. " +
                            "Vui lòng thử lại sau $remainingMinutes phút."
                        } else {
                            // Show remaining attempts
                            val doc = firestore.collection(LOGIN_ATTEMPTS_COLLECTION)
                                .document(email)
                                .get()
                                .await()
                            val attempt = doc.toObject(LoginAttempt::class.java)
                            val remaining = LoginAttempt.MAX_ATTEMPTS - (attempt?.failedCount ?: 0)

                            if (remaining > 0) {
                                "${exception.message}. Còn $remaining lần thử."
                            } else {
                                exception.message.toString()
                            }
                        }

                        _login.emit(Resource.Error(message))
                    }
                }
        }
    }

    companion object {
        private const val LOGIN_ATTEMPTS_COLLECTION = "login_attempts"
    }
}