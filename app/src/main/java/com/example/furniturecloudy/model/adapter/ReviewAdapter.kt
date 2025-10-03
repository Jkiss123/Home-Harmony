package com.example.furniturecloudy.model.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.Review
import com.example.furniturecloudy.databinding.ReviewItemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReviewAdapter : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(private val binding: ReviewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.apply {
                // Load user image
                if (review.userImage.isNotEmpty()) {
                    Glide.with(itemView)
                        .load(review.userImage)
                        .placeholder(R.drawable.ic_profile)
                        .into(imgReviewUserPhoto)
                } else {
                    imgReviewUserPhoto.setImageResource(R.drawable.ic_profile)
                }

                // Set user name
                tvReviewUserName.text = review.userName

                // Set rating
                ratingBarReview.rating = review.rating

                // Set comment
                tvReviewComment.text = review.comment

                // Show verified badge if user purchased product
                imgVerifiedPurchase.visibility = if (review.verified) View.VISIBLE else View.GONE

                // Format date - show relative time
                tvReviewDate.text = getRelativeTimeString(review.timestamp.toDate())
            }
        }

        private fun getRelativeTimeString(date: Date): String {
            val now = Date()
            val diff = now.time - date.time

            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            return when {
                seconds < 60 -> "Vừa xong"
                minutes < 60 -> "$minutes phút trước"
                hours < 24 -> "$hours giờ trước"
                days < 7 -> "$days ngày trước"
                days < 30 -> "${days / 7} tuần trước"
                days < 365 -> "${days / 30} tháng trước"
                else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
            }
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        return ReviewViewHolder(
            ReviewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = differ.currentList[position]
        holder.bind(review)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}
