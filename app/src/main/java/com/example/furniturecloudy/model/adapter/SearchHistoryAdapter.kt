package com.example.furniturecloudy.model.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.furniturecloudy.databinding.SearchHistoryItemBinding
import com.example.furniturecloudy.database.entity.SearchHistory

class SearchHistoryAdapter : RecyclerView.Adapter<SearchHistoryAdapter.SearchHistoryViewHolder>() {

    var onSearchClick: ((String) -> Unit)? = null
    var onDeleteClick: ((SearchHistory) -> Unit)? = null

    private val diffCallback = object : DiffUtil.ItemCallback<SearchHistory>() {
        override fun areItemsTheSame(oldItem: SearchHistory, newItem: SearchHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SearchHistory, newItem: SearchHistory): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    inner class SearchHistoryViewHolder(private val binding: SearchHistoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(searchHistory: SearchHistory) {
            binding.chipSearchHistory.apply {
                text = searchHistory.query

                setOnClickListener {
                    onSearchClick?.invoke(searchHistory.query)
                }

                setOnCloseIconClickListener {
                    onDeleteClick?.invoke(searchHistory)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHistoryViewHolder {
        val binding = SearchHistoryItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchHistoryViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount() = differ.currentList.size
}
