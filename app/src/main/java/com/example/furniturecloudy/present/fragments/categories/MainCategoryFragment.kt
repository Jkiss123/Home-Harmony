package com.example.furniturecloudy.present.fragments.categories

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentMainCategoryBinding
import com.example.furniturecloudy.database.repository.RecentlyViewedRepository
import com.example.furniturecloudy.model.adapter.BestDealsAdapter
import com.example.furniturecloudy.model.adapter.BestProductsAdapter
import com.example.furniturecloudy.model.adapter.SpecialDealsAdapter
import com.example.furniturecloudy.model.viewmodel.MainCategoryViewmodel
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.showBottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
private val TAG = "MainCategoryFragment"
@AndroidEntryPoint
class MainCategoryFragment : Fragment() {
    private lateinit var binding: FragmentMainCategoryBinding
    private lateinit var adapterSpecialDealsAdapter: SpecialDealsAdapter
    private lateinit var adapterBestProductsAdapter: BestProductsAdapter
    private lateinit var adapterBestDealsAdapter: BestDealsAdapter
    private lateinit var adapterRecentlyViewedAdapter: BestProductsAdapter
    private val viewmodel:MainCategoryViewmodel by viewModels()

    @Inject
    lateinit var recentlyViewedRepository: RecentlyViewedRepository
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMainCategoryBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpecialDealsAdapter()
        setupBestDealsAdapter()
        setupBestProducsAdapter()
        setupRecentlyViewedAdapter()

        adapterSpecialDealsAdapter.onClick = {
            val bundle = Bundle().apply { putParcelable("product",it) }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment,bundle)
        }
        adapterBestProductsAdapter.onClick = {
            val bundle = Bundle().apply { putParcelable("product",it) }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment,bundle)
        }
        adapterBestDealsAdapter.onClick = {
            val bundle = Bundle().apply { putParcelable("product",it) }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment,bundle)
        }
        adapterRecentlyViewedAdapter.onClick = {
            val bundle = Bundle().apply { putParcelable("product",it) }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment,bundle)
        }

        // Observe Recently Viewed products
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                recentlyViewedRepository.getRecentlyViewedProducts().collectLatest { products ->
                    if (products.isNotEmpty()) {
                        binding.tvRecentlyViewed.visibility = View.VISIBLE
                        binding.recvRecentlyViewed.visibility = View.VISIBLE
                        adapterRecentlyViewedAdapter.differ.submitList(products)
                    } else {
                        binding.tvRecentlyViewed.visibility = View.GONE
                        binding.recvRecentlyViewed.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.specialProducts.collect{
                    when(it){
                        is Resource.Error -> {
                            hideLoading()
                            Log.e(TAG,it.message.toString())
                            Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> showLoading()
                        is Resource.Success -> {
                            adapterSpecialDealsAdapter.differ.submitList(it.data)
                            hideLoading()
                        }else -> Unit

                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.bestDeals.collect{
                    when(it){
                        is Resource.Error -> {
                            hideLoading()
                            Log.e(TAG,it.message.toString())
                            Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> showLoading()
                        is Resource.Success -> {
                            adapterBestDealsAdapter.differ.submitList(it.data)
                            hideLoading()
                        }else -> Unit

                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.bestProducts.collect{
                    when(it){
                        is Resource.Error -> {
                            binding.progressbarMainCategory2.visibility = View.GONE
                            Log.e(TAG,it.message.toString())
                            Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> binding.progressbarMainCategory2.visibility = View.VISIBLE
                        is Resource.Success -> {
                            adapterBestProductsAdapter.differ.submitList(it.data)
                            binding.progressbarMainCategory2.visibility = View.GONE
                        }else -> Unit

                    }
                }
            }
        }

        binding.NestedScollMainCategory.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener{v,_,scrollY,_,_ ->
            if (v.getChildAt(0).bottom <= v.height + scrollY)
                viewmodel.fetchBestProducts()
        })


    }

    private fun setupBestDealsAdapter() {
        adapterBestDealsAdapter = BestDealsAdapter()
        binding.recvBestDeals.apply {
            layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
            adapter = adapterBestDealsAdapter
        }
    }

    private fun setupBestProducsAdapter() {
        adapterBestProductsAdapter = BestProductsAdapter()
        binding.recvBestProducts.apply {
            layoutManager = GridLayoutManager(requireContext(),2,GridLayoutManager.VERTICAL,false)
            adapter = adapterBestProductsAdapter
        }
    }

    private fun showLoading() {
        binding.progressbarMainCategory.visibility = View.VISIBLE
    }

    private fun hideLoading(){
        binding.progressbarMainCategory.visibility = View.GONE
    }

    private fun setupSpecialDealsAdapter() {
        adapterSpecialDealsAdapter = SpecialDealsAdapter()
        binding.recvSpecialProducts.apply {
             layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
            adapter = adapterSpecialDealsAdapter
        }
    }

    private fun setupRecentlyViewedAdapter() {
        adapterRecentlyViewedAdapter = BestProductsAdapter()
        binding.recvRecentlyViewed.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = adapterRecentlyViewedAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        showBottomNavigationView()
    }

}