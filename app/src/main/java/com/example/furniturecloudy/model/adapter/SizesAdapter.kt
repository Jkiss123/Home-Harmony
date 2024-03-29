package com.example.furniturecloudy.model.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.furniturecloudy.databinding.SizeRvItemBinding

class SizesAdapter : RecyclerView.Adapter<SizesAdapter.SizesAdapterViewHoler>() {

    private var  selectedPotition = -1
    inner class  SizesAdapterViewHoler(private val binding:SizeRvItemBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(size:String, position: Int){
            binding.tvSize.text = size
             if (position == selectedPotition){
                 binding.apply {
                     imgShadow.visibility = View.VISIBLE
                 }
             }else{
                 binding.imgShadow.visibility = View.GONE
             }
        }

    }

    private val differCallBack = object : DiffUtil.ItemCallback<String>(){
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return  oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return  oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,differCallBack)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SizesAdapterViewHoler {
        return SizesAdapterViewHoler(SizeRvItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: SizesAdapterViewHoler, position: Int) {
        var size = differ.currentList[position]
        holder.bind(size,position)
        holder.itemView.setOnClickListener {
            if(selectedPotition  >= 0 ){
                notifyItemChanged(selectedPotition)
            }
            selectedPotition = holder.adapterPosition
            notifyItemChanged(selectedPotition)
            onItemClicked?.invoke(size)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    var onItemClicked : ((String)->Unit)? = null
}