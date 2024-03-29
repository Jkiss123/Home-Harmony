package com.example.furniturecloudy.model.adapter

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.furniturecloudy.data.CartProducts
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.databinding.CartProductItemBinding

class CartAdapter : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {
    inner class CartViewHolder( val binding:CartProductItemBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(cartProducts: CartProducts) {
            binding.apply {
                Glide.with(itemView).load(cartProducts.product.images[0]).into(imgCartProduct)
                tvCartProductname.text = cartProducts.product.name
                if (cartProducts.product.offerPercentage == null) {
                    tvCartProudctprice.text = cartProducts.product.price.toString()
                } else {
                    cartProducts.product.offerPercentage?.let {
                        val remainPercentage = 1f - it
                        val priceAfterOffer = remainPercentage * cartProducts.product.price
                        binding.tvCartProudctprice.text = "$ ${String.format("%.2f", priceAfterOffer)}"
                            //"$ ${String.format("%.2f",priceAfterOffer)}"

                    }
                }
                imgvCartColor.setImageDrawable(
                    ColorDrawable(
                        cartProducts.color ?: Color.TRANSPARENT
                    )
                )
                tvCartProductSize.text = cartProducts.size
                    ?: "".also { imgvCartSize.setImageDrawable(ColorDrawable(Color.TRANSPARENT)) }
                tvCartQuantity.text = cartProducts.quantity.toString()
            }
        }
    }

    private val differCallBack = object :DiffUtil.ItemCallback<CartProducts>(){
        override fun areItemsTheSame(oldItem: CartProducts, newItem: CartProducts): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(oldItem: CartProducts, newItem: CartProducts): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,differCallBack)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        return CartViewHolder(CartProductItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartProducts = differ.currentList[position]
        holder.bind(cartProducts)
        holder.itemView.setOnClickListener {
            onProductClick?.invoke(cartProducts)
        }
        holder.binding.btnplusCart.setOnClickListener {
            onPlusClick?.invoke(cartProducts)
        }

        holder.binding.btnminusCart.setOnClickListener {
            onMinusClick?.invoke(cartProducts)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    var onProductClick:((CartProducts) -> Unit)? = null
    var onPlusClick:((CartProducts) -> Unit)? = null
    var onMinusClick:((CartProducts) -> Unit)? = null
}