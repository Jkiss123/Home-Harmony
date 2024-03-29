package com.example.furniturecloudy.model.firebase

import com.example.furniturecloudy.data.CartProducts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject

class FirebaseCommon(private val firestore: FirebaseFirestore,firebaseAuth: FirebaseAuth) {
    private val cartCollecttion = firestore.collection("user").document(firebaseAuth.uid!!).collection("cart")

    fun addProductToCart(cartProducts: CartProducts,onResult: (CartProducts?,Exception?) ->Unit){
        cartCollecttion.document().set(cartProducts)
            .addOnSuccessListener {
                onResult(cartProducts,null)
            }.addOnFailureListener {
                onResult(null,it)
            }
    }

    fun increaseQuantity(documentId :String,onResult: (String?, Exception?) -> Unit){
        firestore.runTransaction{transition ->
            val documentRef = cartCollecttion.document(documentId)
            val document = transition.get(documentRef)
            val productObject = document.toObject(CartProducts::class.java)
            productObject?.let {
                val newQuantity = it.quantity + 1
                val newObjectProduct = it.copy(quantity = newQuantity)
                transition.set(documentRef,newObjectProduct)
            }
        }.addOnSuccessListener {
            onResult(documentId,null)
        }.addOnFailureListener {
            onResult(null,it)
        }
    }

    fun decreaseQuantity(documentId :String,onResult: (String?, Exception?) -> Unit){
        firestore.runTransaction{transition ->
            val documentRef = cartCollecttion.document(documentId)
            val document = transition.get(documentRef)
            val productObject = document.toObject(CartProducts::class.java)
            productObject?.let {
                val newQuantity = it.quantity - 1
                val newObjectProduct = it.copy(quantity = newQuantity)
                transition.set(documentRef,newObjectProduct)
            }
        }.addOnSuccessListener {
            onResult(documentId,null)
        }.addOnFailureListener {
            onResult(null,it)
        }
    }

enum class QuantityStatus{
    INCREASE,DECREASE
}

}