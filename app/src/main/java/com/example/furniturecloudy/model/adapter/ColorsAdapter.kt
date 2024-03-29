package com.example.furniturecloudy.model.adapter

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.furniturecloudy.databinding.ColorRvItemBinding
import com.example.furniturecloudy.databinding.ViewpagerImageItemBinding

class ColorsAdapter : RecyclerView.Adapter<ColorsAdapter.ColorsItemView>(){
    private var  selectedPotition = -1
    inner class ColorsItemView(private val binding: ColorRvItemBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(color:Int,position: Int){
            val imageDrawable = ColorDrawable(color)
            binding.imgeColor.setImageDrawable(imageDrawable)
            if (position == selectedPotition){
                binding.apply {
                    imgShadow.visibility = View.VISIBLE
                    imgpicked.visibility = View.VISIBLE
                }
            }else{
                binding.apply {
                    imgShadow.visibility = View.INVISIBLE
                    imgpicked.visibility = View.INVISIBLE
                }
            }
        }
    }

    private val differCallBack = object :DiffUtil.ItemCallback<Int>(){
        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
            return  oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return  oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,differCallBack)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorsItemView {
        return ColorsItemView(ColorRvItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ColorsItemView, position: Int) {
        var color = differ.currentList[position]
        holder.bind(color,position)
        holder.itemView.setOnClickListener {
            if(selectedPotition  >= 0 ){
                notifyItemChanged(selectedPotition)
            }
            selectedPotition = holder.adapterPosition
            notifyItemChanged(selectedPotition)
            onItemClicked?.invoke(color)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    var onItemClicked : ((Int)->Unit)? = null
}