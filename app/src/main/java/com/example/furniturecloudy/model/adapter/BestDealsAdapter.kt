package com.example.furniturecloudy.model.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.databinding.BestDealsRvItemBinding

class BestDealsAdapter() : RecyclerView.Adapter<BestDealsAdapter.BestDealsViewHolder>() {

    inner class BestDealsViewHolder(private val binding: BestDealsRvItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(product: Product){
            binding.apply {
                Glide.with(itemView).load(product.images[0]).into(imgBestDeal)
                tvBestdealName.setText(product.name)
                if (product.offerPercentage == null) {
                    tvNewPrice.text = "$ ${String.format("%.2f", product.price)}"
                    tvOldPrice.visibility = View.GONE
                } else {
                    product.offerPercentage.let {
                        val remainPercentage = 1f - it
                        val priceAfterOffer = remainPercentage * product.price
                        binding.tvNewPrice.text = "$ ${String.format("%.2f", priceAfterOffer)}"
                        tvOldPrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                    }
                    tvOldPrice.text = product.price.toString()
                }
            }
        }
    }

    private val differCallback = object :DiffUtil.ItemCallback<Product>(){
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BestDealsViewHolder {
        return BestDealsViewHolder(BestDealsRvItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: BestDealsViewHolder, position: Int) {
        val product = differ.currentList[position]
        holder.bind(product)
        holder.itemView.setOnClickListener {
            onClick?.invoke(product)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }



    var onClick : ((Product) ->Unit)? = null
}