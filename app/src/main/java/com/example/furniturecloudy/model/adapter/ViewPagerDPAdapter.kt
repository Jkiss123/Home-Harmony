package com.example.furniturecloudy.model.adapter

import android.media.Image
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.furniturecloudy.databinding.ViewpagerImageItemBinding

class ViewPagerDPAdapter : RecyclerView.Adapter<ViewPagerDPAdapter.ViewPagerDPViewHolder>() {

    inner class ViewPagerDPViewHolder( val binding:ViewpagerImageItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(imagePath:String){
            Glide.with(itemView).load(imagePath).into(binding.imgProductDetail)
        }
    }

    private val differCallback = object :DiffUtil.ItemCallback<String>(){
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
           return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerDPViewHolder {
        return ViewPagerDPViewHolder(ViewpagerImageItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ViewPagerDPViewHolder, position: Int) {
        val image = differ.currentList[position]
        holder.bind(image)
    }

    override fun getItemCount(): Int {
       return differ.currentList.size
    }
}