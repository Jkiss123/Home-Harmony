package com.example.furniturecloudy.present.fragments.shopping

import android.content.Intent
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
import com.example.furniturecloudy.data.ProductFilter
import com.example.furniturecloudy.databinding.FragmentSearchBinding
import com.example.furniturecloudy.model.adapter.BestDealsAdapter
import com.example.furniturecloudy.model.adapter.SearchHistoryAdapter
import com.example.furniturecloudy.model.viewmodel.SearchViewmodel
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.VoiceSearchManager
import com.example.furniturecloudy.util.showBottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private lateinit var binding : FragmentSearchBinding
    private val bestDealsAdapter by lazy { BestDealsAdapter() }
    private val searchHistoryAdapter by lazy { SearchHistoryAdapter() }
    private val viewmodel: SearchViewmodel by viewModels()

    private var isSearching = false
    private var isFromRecentSearch = false

    @Inject
    lateinit var searchHistoryRepository: SearchHistoryRepository

    private lateinit var voiceSearchManager: VoiceSearchManager
    private val VOICE_SEARCH_REQUEST_CODE = 1001

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
        setupSearchHistoryRv()
        setupSearchView()
        setupVoiceSearch()
        setupFilterButton()
        observeProducts()
        observeSearchResults()
        observeSearchHistory()

        bestDealsAdapter.onClick = {
            val bundle = Bundle().apply { putParcelable("product",it) }
            findNavController().navigate(R.id.action_searchFragment_to_productDetailFragment,bundle)
        }

        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener{ v, _, scrollY, _, _ ->
            // Only allow pagination when not actively searching or filtering
            if (!isSearching && !viewmodel.isFiltering() && v.getChildAt(0).bottom <= v.height + scrollY) {
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
                        // Save to search history if not from recent search
                        if (!isFromRecentSearch) {
                            viewLifecycleOwner.lifecycleScope.launch {
                                searchHistoryRepository.addSearch(it)
                            }
                        }
                        // Reset flag after submit
                        isFromRecentSearch = false
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

    private fun setupVoiceSearch() {
        voiceSearchManager = VoiceSearchManager(this)
        binding.btnVoiceSearch.setOnClickListener {
            // Add visual feedback
            it.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()

            voiceSearchManager.startVoiceRecognition(VOICE_SEARCH_REQUEST_CODE)
            Toast.makeText(
                requireContext(),
                "Đang nghe... Hãy nói tên sản phẩm",
                Toast.LENGTH_SHORT
            ).show()
        }
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

    private fun setupSearchHistoryRv() {
        binding.rvRecentSearches.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = searchHistoryAdapter
        }

        searchHistoryAdapter.onSearchClick = { query ->
            isFromRecentSearch = true
            binding.searchView.setQuery(
                query,
                true
            ) // true = submit, will trigger onQueryTextSubmit
        }

        searchHistoryAdapter.onDeleteClick = { searchHistory ->
            viewLifecycleOwner.lifecycleScope.launch {
                searchHistoryRepository.deleteSearch(searchHistory)
            }
        }
    }

    private fun setupFilterButton() {
        binding.fabFilter.setOnClickListener {
            val filterBottomSheet = FilterBottomSheetFragment.newInstance(viewmodel.getCurrentFilter())
            filterBottomSheet.onFilterApplied = { filter ->
                viewmodel.applyFilter(filter)
                updateFilterChip(filter)
            }
            filterBottomSheet.show(childFragmentManager, "FilterBottomSheet")
        }

        binding.chipActiveFilter.setOnCloseIconClickListener {
            viewmodel.applyFilter(ProductFilter())
            binding.layoutFilterChips.visibility = View.GONE
        }
    }

    private fun updateFilterChip(filter: ProductFilter) {
        if (filter.isActive()) {
            val count = filter.getActiveFilterCount()
            binding.chipActiveFilter.text = "Đang lọc ($count)"
            binding.layoutFilterChips.visibility = View.VISIBLE
        } else {
            binding.layoutFilterChips.visibility = View.GONE
        }
    }

    private fun observeSearchHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchHistoryRepository.getRecentSearches(limit = 10).collectLatest { searches ->
                    if (searches.isNotEmpty()) {
                        searchHistoryAdapter.differ.submitList(searches)
                        binding.tvRecentSearches.visibility = View.VISIBLE
                        binding.rvRecentSearches.visibility = View.VISIBLE
                    } else {
                        binding.tvRecentSearches.visibility = View.GONE
                        binding.rvRecentSearches.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showBottomNavigationView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_SEARCH_REQUEST_CODE) {
            val recognizedText = voiceSearchManager.handleVoiceResult(resultCode, data)
            if (recognizedText != null && recognizedText.isNotEmpty()) {
                isFromRecentSearch = false // Voice search should save to history
                binding.searchView.setQuery(recognizedText, true)
                Toast.makeText(requireContext(), "Đã tìm kiếm: $recognizedText", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Không nhận diện được giọng nói. Vui lòng thử lại",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}