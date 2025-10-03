package com.example.furniturecloudy.data

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<Timestamp, TimestampParceler>()
data class Review(
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImage: String = "",
    val rating: Float = 0f, // 1.0 to 5.0
    val comment: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val verified: Boolean = false // Has user purchased this product?
) : Parcelable {
    constructor() : this("", "", "", "", "", 0f, "", Timestamp.now(), false)
}

object TimestampParceler : Parceler<Timestamp> {
    override fun create(parcel: Parcel): Timestamp {
        val seconds = parcel.readLong()
        val nanoseconds = parcel.readInt()
        return Timestamp(seconds, nanoseconds)
    }

    override fun Timestamp.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(this.seconds)
        parcel.writeInt(this.nanoseconds)
    }
}
