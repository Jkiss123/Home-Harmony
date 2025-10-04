package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.data.Wishlist
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
class WishlistViewmodel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _wishlist = MutableStateFlow<Resource<List<Wishlist>>>(Resource.UnSpecified())
    val wishlist = _wishlist.asStateFlow()

    private val _addToWishlist = MutableSharedFlow<Resource<Product>>()
    val addToWishlist = _addToWishlist.asSharedFlow()

    private val _removeFromWishlist = MutableSharedFlow<Resource<Product>>()
    val removeFromWishlist = _removeFromWishlist.asSharedFlow()

    init {
        getWishlist()
    }

    fun getWishlist() {
        viewModelScope.launch {
            _wishlist.emit(Resource.Loading())
        }

        firestore.collection("user")
            .document(auth.uid!!)
            .collection("wishlist")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    viewModelScope.launch {
                        _wishlist.emit(Resource.Error(error.message.toString()))
                    }
                    return@addSnapshotListener
                }

                val wishlistItems = value?.toObjects(Wishlist::class.java)
                viewModelScope.launch {
                    _wishlist.emit(Resource.Success(wishlistItems ?: emptyList()))
                }
            }
    }

    fun addProductToWishlist(product: Product) {
        viewModelScope.launch {
            _addToWishlist.emit(Resource.Loading())
        }

        val wishlistItem = Wishlist(product)

        firestore.collection("user")
            .document(auth.uid!!)
            .collection("wishlist")
            .document(product.id)
            .set(wishlistItem)
            .addOnSuccessListener {
                viewModelScope.launch {
                    _addToWishlist.emit(Resource.Success(product))
                }
            }
            .addOnFailureListener {
                viewModelScope.launch {
                    _addToWishlist.emit(Resource.Error(it.message.toString()))
                }
            }
    }

    fun removeProductFromWishlist(productId: String) {
        viewModelScope.launch {
            _removeFromWishlist.emit(Resource.Loading())
        }

        firestore.collection("user")
            .document(auth.uid!!)
            .collection("wishlist")
            .document(productId)
            .delete()
            .addOnSuccessListener {
                viewModelScope.launch {
                    _removeFromWishlist.emit(Resource.Success(Product()))
                }
            }
            .addOnFailureListener {
                viewModelScope.launch {
                    _removeFromWishlist.emit(Resource.Error(it.message.toString()))
                }
            }
    }

    fun isProductInWishlist(productId: String): Boolean {
        return when (val state = _wishlist.value) {
            is Resource.Success -> {
                state.data?.any { it.product.id == productId } ?: false
            }
            else -> false
        }
    }
}
