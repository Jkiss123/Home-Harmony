package com.example.furniturecloudy.data

data class PagingInfo(
    var page : Long = 1,
    var oldProduct: List<Product> = emptyList(),
    var isPagingEnd:Boolean = false
) {
}