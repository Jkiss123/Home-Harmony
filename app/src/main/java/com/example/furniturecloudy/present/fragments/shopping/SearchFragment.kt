package com.example.furniturecloudy.present.fragments.shopping

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.furniturecloudy.R
import android.widget.ArrayAdapter
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.database.repository.SearchHistoryRepository
import com.example.furniturecloudy.databinding.FragmentSearchBinding
import com.example.furniturecloudy.model.adapter.BestDealsAdapter
import com.example.furniturecloudy.model.viewmodel.SearchViewmodel
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.showBottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private lateinit var binding : FragmentSearchBinding
    private val bestDealsAdapter by lazy { BestDealsAdapter() }
    private val viewmodel : SearchViewmodel by viewModels()
    private var isSearching = false

    @Inject
    lateinit var searchHistoryRepository: SearchHistoryRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSearchBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRv()
        setupSearchView()
        observeProducts()
        observeSearchResults()

        bestDealsAdapter.onClick = {
            val bundle = Bundle().apply { putParcelable("product",it) }
            findNavController().navigate(R.id.action_searchFragment_to_productDetailFragment,bundle)
        }

        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener{ v, _, scrollY, _, _ ->
            // Only allow pagination when not actively searching
            if (!isSearching && v.getChildAt(0).bottom <= v.height + scrollY) {
                viewmodel.getProducts()
            }
        })
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.isNotEmpty()) {
                        isSearching = true
                        viewmodel.searchProducts(it)
                        // Save to search history
                        viewLifecycleOwner.lifecycleScope.launch {
                            searchHistoryRepository.addSearch(it)
                        }
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    isSearching = it.isNotEmpty()
                    if (it.isEmpty()) {
                        viewmodel.resetSearch()
                        // Show all products when search is cleared
                        observeProducts()
                    } else {
                        viewmodel.searchProducts(it)
                    }
                }
                return true
            }
        })
    }

    private fun observeProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.products.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            binding.progressbarSearch.visibility = View.GONE
                            Toast.makeText(requireContext(),"Không thể tải sản phẩm. Vui lòng thử lại", Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.progressbarSearch.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressbarSearch.visibility = View.GONE
                            if (!isSearching) {
                                bestDealsAdapter.differ.submitList(it.data)
                                updateEmptyState(it.data?.isEmpty() == true)
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeSearchResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.searchResults.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            binding.progressbarSearch.visibility = View.GONE
                            Toast.makeText(requireContext(),"Tìm kiếm thất bại. Vui lòng thử lại", Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.progressbarSearch.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressbarSearch.visibility = View.GONE
                            bestDealsAdapter.differ.submitList(it.data)
                            updateEmptyState(it.data?.isEmpty() == true)
                        }
                        is Resource.UnSpecified -> {
                            // When search is reset, this will be triggered
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.tvEmptySearch.visibility = View.VISIBLE
            binding.recvSearch.visibility = View.GONE
        } else {
            binding.tvEmptySearch.visibility = View.GONE
            binding.recvSearch.visibility = View.VISIBLE
        }
    }

    private fun setupRv(){
        binding.recvSearch.apply {
            layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false)
            adapter = bestDealsAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        showBottomNavigationView()
    }
}