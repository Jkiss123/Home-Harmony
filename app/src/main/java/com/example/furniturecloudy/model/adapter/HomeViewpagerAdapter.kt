package com.example.furniturecloudy.model.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class HomeViewpagerAdapter(
    private val fragmes: List<Fragment>,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager,lifecycle){
    override fun getItemCount(): Int {
        return fragmes.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragmes[position]
    }
}