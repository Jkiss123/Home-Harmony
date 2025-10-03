package com.example.furniturecloudy.data

import com.google.firebase.firestore.DocumentSnapshot

data class PagingInfo(
    var page : Long = 1,
    var oldProduct: List<Product> = emptyList(),
    var isPagingEnd:Boolean = false,
    var lastVisibleDocument: DocumentSnapshot? = null
) {
}