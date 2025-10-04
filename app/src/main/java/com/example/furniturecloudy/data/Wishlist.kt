package com.example.furniturecloudy.data

import com.google.firebase.Timestamp

data class Wishlist(
    val product: Product = Product(),
    val addedAt: Timestamp = Timestamp.now()
) {
    constructor() : this(Product(), Timestamp.now())
}
