package com.example.furniturecloudy.present.fragments.shopping

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentHomeBinding
import com.example.furniturecloudy.model.adapter.HomeViewpagerAdapter
import com.example.furniturecloudy.present.fragments.categories.AccessoryFragment
import com.example.furniturecloudy.present.fragments.categories.ChairFragment
import com.example.furniturecloudy.present.fragments.categories.CupBoardFragment
import com.example.furniturecloudy.present.fragments.categories.FurnitureFragment
import com.example.furniturecloudy.present.fragments.categories.MainCategoryFragment
import com.example.furniturecloudy.present.fragments.categories.TableFragment
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private lateinit var binding : FragmentHomeBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoriesFragments = arrayListOf<Fragment>(
            MainCategoryFragment(),
            ChairFragment(),
            CupBoardFragment(),
            TableFragment(),
            AccessoryFragment(),
            FurnitureFragment()
        )
        binding.viewpagerHomeFragment.isUserInputEnabled = false

        val viewpagerAdapter = HomeViewpagerAdapter(categoriesFragments,childFragmentManager,lifecycle)
        binding.viewpagerHomeFragment.adapter = viewpagerAdapter
        TabLayoutMediator(binding.homeFragmentTablayout,binding.viewpagerHomeFragment){tab,position ->
            when(position){
                0-> tab.text = "Trang chủ"
                1-> tab.text = "Ghế"
                2-> tab.text = "Tủ"
                3-> tab.text = "Bàn"
                4-> tab.text = "Phụ kiện"
                5-> tab.text = "Nội thất"
            }
        }.attach()
    }


}