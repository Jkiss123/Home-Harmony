package com.example.furniturecloudy.model.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.databinding.ProductRvItemBinding

class BestProductsAdapter() : RecyclerView.Adapter<BestProductsAdapter.BestProductsViewHoler>() {
    inner class BestProductsViewHoler(private val binding:ProductRvItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(product: Product){
            binding.apply {
                Glide.with(itemView).load(product.images[0]).into(imgProduct)
                tvName.text = product.name
                tvPrice.text = "$ ${product.price.toString()}"
                if(product.offerPercentage == null){
                    tvNewPrice.visibility = View.GONE
                }else {
                    product.offerPercentage?.let {
                        val remainPercentage = 1f - it
                        val priceAfterOffer = remainPercentage * product.price
                        binding.tvNewPrice.text = "$ ${String.format("%.2f", priceAfterOffer)}"
                        tvPrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                    }
                }

            }
        }
    }

    val differCallback = object : DiffUtil.ItemCallback<Product>(){
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,differCallback)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BestProductsViewHoler {
        return BestProductsViewHoler(ProductRvItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: BestProductsViewHoler, position: Int) {
        val product = differ.currentList[position]
        holder.bind(product)
        holder.itemView.setOnClickListener {
            onClick?.invoke(product)
        }
    }

    var onClick : ((Product) ->Unit)? = null
}