package com.example.furniturecloudy.model.adapter

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.Order
import com.example.furniturecloudy.data.OrderStatus
import com.example.furniturecloudy.data.getOrderStatus
import com.example.furniturecloudy.databinding.OrderItemBinding

class OrdersAdapter : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(private val binding:OrderItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(order: Order){
            binding.apply {
                tvOrderDate.text = order.date
                tvOrderId.text = order.orderId.toString()
                val resources = itemView.resources
                val colorDrawable = when (getOrderStatus(order.orderStatus)) {
                    is OrderStatus.Ordered -> {
                        ColorDrawable(resources.getColor(R.color.g_orange_yellow))
                    }
                    is OrderStatus.Confirmed -> {
                        ColorDrawable(resources.getColor(R.color.g_green))
                    }
                    is OrderStatus.Delivered -> {
                        ColorDrawable(resources.getColor(R.color.g_green))
                    }
                    is OrderStatus.Shipped -> {
                        ColorDrawable(resources.getColor(R.color.g_green))
                    }
                    is OrderStatus.Canceled -> {
                        ColorDrawable(resources.getColor(R.color.g_red))
                    }
                    is OrderStatus.Returned -> {
                        ColorDrawable(resources.getColor(R.color.g_red))
                    }
                }
                imageOrderState.setImageDrawable(colorDrawable)
            }
        }
    }

    private val differCallBack = object : DiffUtil.ItemCallback<Order>(){
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,differCallBack)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
       return OrderViewHolder(OrderItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = differ.currentList[position]
        holder.bind(order)
        holder.itemView.setOnClickListener {
            onClick?.invoke(order)
        }

    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    var onClick : ((Order) ->Unit)? = null
}