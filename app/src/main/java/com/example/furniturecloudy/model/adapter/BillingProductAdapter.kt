package com.example.furniturecloudy.model.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.furniturecloudy.data.Address
import com.example.furniturecloudy.data.CartProducts
import com.example.furniturecloudy.databinding.BillingProductsRvItemBinding

class BillingProductAdapter : RecyclerView.Adapter<BillingProductAdapter.BillingViewHolder>() {
    inner class BillingViewHolder(private val binding:BillingProductsRvItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(cartProducts: CartProducts){
            binding.apply {
                Glide.with(itemView).load(cartProducts.product.images[0]).into(imageCartProduct)
                tvProductCartName.text = cartProducts.product.name
                tvBillingProductQuantity.text = cartProducts.quantity.toString()
                // setup gia
                if (cartProducts.product.offerPercentage == null) {
                    tvProductCartPrice.text = cartProducts.product.price.toString()
                } else {
                    cartProducts.product.offerPercentage?.let {
                        val remainPercentage = 1f - it
                        val priceAfterOffer = remainPercentage * cartProducts.product.price
                        binding.tvProductCartPrice.text = "$ ${String.format("%.2f", priceAfterOffer)}"
                    }
                }
                imageCartProductColor.setImageDrawable(
                    ColorDrawable(
                        cartProducts.color ?: Color.TRANSPARENT
                    )
                )
                tvCartProductSize.text = cartProducts.size
                    ?: "".also { imageCartProductSize.setImageDrawable(ColorDrawable(Color.TRANSPARENT)) }
            }

        }
    }



    private val differUtil = object : DiffUtil.ItemCallback<CartProducts>(){
        override fun areItemsTheSame(oldItem: CartProducts, newItem: CartProducts): Boolean {
            return oldItem.product == newItem.product
        }

        override fun areContentsTheSame(oldItem: CartProducts, newItem: CartProducts): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,differUtil)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillingViewHolder {
        return BillingViewHolder(BillingProductsRvItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: BillingViewHolder, position: Int) {
        val cartProducts = differ.currentList[position]
        holder.bind(cartProducts)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}