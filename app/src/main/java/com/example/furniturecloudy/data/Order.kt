package com.example.furniturecloudy.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import kotlin.random.nextLong


@Parcelize
data class Order(
    val orderStatus: String = "",
    val totalPrice: Float = 0f,
    val products: List<CartProducts> = emptyList(),
    val address: Address = Address(),
    val date: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date()),
    val orderId: String = "ORD-${SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH).format(Date())}-${Random.nextInt(10000, 99999)}",
    val paymentMethod: String = "COD", // "COD", "MoMo", "VNPay", "ZaloPay"
    val paymentStatus: String = "PENDING", // "PENDING", "PAID", "FAILED", "REFUNDED"
    val paymentTransactionId: String? = null // Transaction ID from payment gateway
): Parcelable
