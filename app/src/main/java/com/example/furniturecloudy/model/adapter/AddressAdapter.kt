package com.example.furniturecloudy.model.adapter

import android.util.Log
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
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
        fun bind(address: Address, selected: Boolean) {
            binding.apply {
                // Hiển thị thông tin địa chỉ hợp lý nhất có thể
                val displayParts = mutableListOf<String>()
                if (address.wards.trim().isNotEmpty()) {
                    displayParts.add(address.wards.trim())
                }
                if (address.district.trim().isNotEmpty()) {
                    displayParts.add(address.district.trim())
                }
                if (address.city.trim().isNotEmpty()) {
                    displayParts.add(address.city.trim())
                }

                val displayText = when {
                    displayParts.isNotEmpty() -> displayParts.joinToString(", ")
                    address.addressFull.trim().isNotEmpty() -> address.addressFull.trim()
                    address.fullName.trim().isNotEmpty() -> address.fullName.trim()
                    else -> "Địa chỉ mới"
                }

                buttonAddress.text = displayText

                // Force button dimensions để fix single item issue
                val layoutParams = buttonAddress.layoutParams
                if (layoutParams != null) {
                    // Set minimum width to ensure text is visible
                    if (layoutParams.width == 0 || layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                        // Convert 200dp to pixels
                        val scale = itemView.context.resources.displayMetrics.density
                        val px = (200 * scale + 0.5f).toInt()
                        layoutParams.width = px
                        buttonAddress.layoutParams = layoutParams
                    }
                }

                if (selected) {
                    buttonAddress.background = ColorDrawable(itemView.context.resources.getColor(R.color.g_blue))
                    buttonAddress.setTextColor(itemView.context.resources.getColor(R.color.white))
                } else {
                    buttonAddress.background = ColorDrawable(itemView.context.resources.getColor(R.color.g_white))
                    buttonAddress.setTextColor(itemView.context.resources.getColor(R.color.g_blue_gray200))
                }
            }
        }
    }

    private val differUtil = object : DiffUtil.ItemCallback<Address>(){
        override fun areItemsTheSame(oldItem: Address, newItem: Address): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Address, newItem: Address): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,differUtil)


    fun resetSelection() {
        val oldSelected = selectedAdress
        selectedAdress = -1
        if (oldSelected >= 0) {
            notifyItemChanged(oldSelected)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        return AddressViewHolder(AddressRvItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val address = differ.currentList[position]
        holder.bind(address, selectedAdress == position)
        holder.binding.buttonAddress.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val lastSelected = selectedAdress
                if (lastSelected != currentPosition) {
                    selectedAdress = currentPosition
                    if (lastSelected >= 0) notifyItemChanged(lastSelected)
                    notifyItemChanged(selectedAdress)
                    onClick?.invoke(address)
                }
            }
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