package com.example.furniturecloudy.model.adapter

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.Address
import com.example.furniturecloudy.databinding.AddressRvItemBinding

class AddressAdapter : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    private var selectedAdress = -1

    inner class AddressViewHolder( val binding : AddressRvItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(address: Address,selected : Boolean){
            binding.apply {
                buttonAddress.text = address.wards
                if (selected){
                    buttonAddress.background = ColorDrawable(itemView.context.resources.getColor(R.color.g_blue))
                }else{
                    buttonAddress.background = ColorDrawable(itemView.context.resources.getColor(R.color.g_white))
                }
            }
        }
    }

    private val differUtil = object : DiffUtil.ItemCallback<Address>(){
        override fun areItemsTheSame(oldItem: Address, newItem: Address): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Address, newItem: Address): Boolean {
            return  oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,differUtil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        return AddressViewHolder(AddressRvItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val address = differ.currentList[position]
        holder.bind(address,selectedAdress == position)
        holder.binding.buttonAddress.setOnClickListener {
            if (selectedAdress >= 0)
                notifyItemChanged(selectedAdress)
            selectedAdress = holder.adapterPosition
            notifyItemChanged(selectedAdress)
            onClick?.invoke(address)
        }

        holder.binding.imageEditAddress.setOnClickListener {
            onEditClick?.invoke(address)
        }

        holder.binding.imageDeleteAddress.setOnClickListener {
            onDeleteClick?.invoke(address)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    var onClick: ((Address) -> Unit)? = null
    var onEditClick: ((Address) -> Unit)? = null
    var onDeleteClick: ((Address) -> Unit)? = null
}