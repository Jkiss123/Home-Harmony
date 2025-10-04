package com.example.furniturecloudy.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class PaymentMethod : Parcelable {
    @Parcelize
    object COD : PaymentMethod(), Parcelable {
        override fun toString() = "COD"
    }

    @Parcelize
    data class MoMo(val transactionId: String = "") : PaymentMethod(), Parcelable {
        override fun toString() = "MoMo"
    }

    @Parcelize
    data class VNPay(val transactionId: String = "") : PaymentMethod(), Parcelable {
        override fun toString() = "VNPay"
    }

    @Parcelize
    data class ZaloPay(val transactionId: String = "") : PaymentMethod(), Parcelable {
        override fun toString() = "ZaloPay"
    }

    companion object {
        fun fromString(method: String): PaymentMethod {
            return when (method) {
                "COD" -> COD
                "MoMo" -> MoMo()
                "VNPay" -> VNPay()
                "ZaloPay" -> ZaloPay()
                else -> COD
            }
        }
    }
}

@Parcelize
enum class PaymentStatus : Parcelable {
    PENDING,    // Chờ thanh toán
    PAID,       // Đã thanh toán
    FAILED,     // Thanh toán thất bại
    REFUNDED;   // Đã hoàn tiền

    fun getStatusText(): String {
        return when (this) {
            PENDING -> "Chờ thanh toán"
            PAID -> "Đã thanh toán"
            FAILED -> "Thanh toán thất bại"
            REFUNDED -> "Đã hoàn tiền"
        }
    }
}
