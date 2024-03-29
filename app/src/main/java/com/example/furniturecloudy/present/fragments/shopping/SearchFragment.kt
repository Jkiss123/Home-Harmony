package com.example.furniturecloudy.present.fragments.shopping

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.databinding.FragmentSearchBinding
import com.example.furniturecloudy.model.adapter.BestDealsAdapter
import com.example.furniturecloudy.model.viewmodel.SearchViewmodel
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.showBottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private lateinit var binding : FragmentSearchBinding
    private val bestDealsAdapter by lazy { BestDealsAdapter() }
    private val viewmodel : SearchViewmodel by viewModels()
    private  lateinit var listProduct : List<Product>
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.products.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            binding.progressbarSearch.visibility = View.GONE
                            Toast.makeText(requireContext(),it.message, Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.progressbarSearch.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressbarSearch.visibility = View.GONE
                            bestDealsAdapter.differ.submitList(it.data)
                            listProduct = it.data!!
                        }
                        else -> Unit
                    }
                }
            }
        }

        bestDealsAdapter.onClick = {
            val bundle = Bundle().apply { putParcelable("product",it) }
            findNavController().navigate(R.id.action_searchFragment_to_productDetailFragment,bundle)
        }

        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener{ v, _, scrollY, _, _ ->
            if (v.getChildAt(0).bottom <= v.height + scrollY)
                viewmodel.getProducts()
        })


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