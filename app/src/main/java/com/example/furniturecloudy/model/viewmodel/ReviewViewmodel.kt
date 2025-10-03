package com.example.furniturecloudy.model.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furniturecloudy.data.Review
import com.example.furniturecloudy.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewmodel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _reviews = MutableStateFlow<Resource<List<Review>>>(Resource.UnSpecified())
    val reviews: StateFlow<Resource<List<Review>>> = _reviews.asStateFlow()

    private val _addReview = MutableStateFlow<Resource<Review>>(Resource.UnSpecified())
    val addReview: StateFlow<Resource<Review>> = _addReview.asStateFlow()

    private val _averageRating = MutableStateFlow<Float>(0f)
    val averageRating: StateFlow<Float> = _averageRating.asStateFlow()

    private val _reviewCount = MutableStateFlow<Int>(0)
    val reviewCount: StateFlow<Int> = _reviewCount.asStateFlow()

    fun getReviewsForProduct(productId: String) {
        viewModelScope.launch { _reviews.emit(Resource.Loading()) }

        firestore.collection("Reviews")
            .whereEqualTo("productId", productId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    viewModelScope.launch {
                        _reviews.emit(Resource.Error(error.message.toString()))
                    }
                    return@addSnapshotListener
                }

                val reviewsList = value?.toObjects(Review::class.java) ?: emptyList()
                viewModelScope.launch {
                    _reviews.emit(Resource.Success(reviewsList))

                    // Calculate average rating
                    if (reviewsList.isNotEmpty()) {
                        val avgRating = reviewsList.map { it.rating }.average().toFloat()
                        _averageRating.emit(avgRating)
                        _reviewCount.emit(reviewsList.size)
                    } else {
                        _averageRating.emit(0f)
                        _reviewCount.emit(0)
                    }
                }
            }
    }

    fun addReview(productId: String, rating: Float, comment: String, userName: String, userImage: String) {
        viewModelScope.launch { _addReview.emit(Resource.Loading()) }

        // Check if user has already reviewed this product
        firestore.collection("Reviews")
            .whereEqualTo("productId", productId)
            .whereEqualTo("userId", auth.uid!!)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    viewModelScope.launch {
                        _addReview.emit(Resource.Error("Bạn đã đánh giá sản phẩm này rồi"))
                    }
                    return@addOnSuccessListener
                }

                // Check if user has purchased this product
                checkPurchaseAndAddReview(productId, rating, comment, userName, userImage)
            }
            .addOnFailureListener { exception ->
                viewModelScope.launch {
                    _addReview.emit(Resource.Error(exception.message.toString()))
                }
            }
    }

    private fun checkPurchaseAndAddReview(
        productId: String,
        rating: Float,
        comment: String,
        userName: String,
        userImage: String
    ) {
        // Check if user has ordered this product
        firestore.collection("user").document(auth.uid!!)
            .collection("order")
            .get()
            .addOnSuccessListener { orders ->
                var hasPurchased = false

                for (orderDoc in orders.documents) {
                    val products = orderDoc.get("products") as? List<*>
                    products?.forEach { productMap ->
                        if (productMap is Map<*, *>) {
                            val product = productMap["product"] as? Map<*, *>
                            if (product?.get("id") == productId) {
                                hasPurchased = true
                                return@forEach
                            }
                        }
                    }
                    if (hasPurchased) break
                }

                saveReview(productId, rating, comment, userName, userImage, hasPurchased)
            }
            .addOnFailureListener {
                // If check fails, still allow review but mark as unverified
                saveReview(productId, rating, comment, userName, userImage, false)
            }
    }

    private fun saveReview(
        productId: String,
        rating: Float,
        comment: String,
        userName: String,
        userImage: String,
        verified: Boolean
    ) {
        val reviewRef = firestore.collection("Reviews").document()
        val review = Review(
            id = reviewRef.id,
            productId = productId,
            userId = auth.uid!!,
            userName = userName,
            userImage = userImage,
            rating = rating,
            comment = comment,
            verified = verified
        )

        reviewRef.set(review)
            .addOnSuccessListener {
                viewModelScope.launch {
                    _addReview.emit(Resource.Success(review))
                }
            }
            .addOnFailureListener { exception ->
                viewModelScope.launch {
                    _addReview.emit(Resource.Error(exception.message.toString()))
                }
            }
    }

    fun resetAddReviewState() {
        viewModelScope.launch {
            _addReview.emit(Resource.UnSpecified())
        }
    }
}
