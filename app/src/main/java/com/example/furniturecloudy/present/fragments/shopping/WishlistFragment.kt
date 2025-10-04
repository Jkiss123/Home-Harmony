package com.example.furniturecloudy.present.fragments.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentWishlistBinding
import com.example.furniturecloudy.model.adapter.BestProductsAdapter
import com.example.furniturecloudy.model.viewmodel.WishlistViewmodel
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.VerticalItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WishlistFragment : Fragment() {

    private lateinit var binding: FragmentWishlistBinding
    private val viewmodel by viewModels<WishlistViewmodel>()
    private val wishlistAdapter by lazy { BestProductsAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeWishlist()

        binding.imageCloseWishlist.setOnClickListener {
            findNavController().navigateUp()
        }

        wishlistAdapter.onClick = { product ->
            val bundle = Bundle().apply { putParcelable("product", product) }
            findNavController().navigate(R.id.action_wishlistFragment_to_productDetailFragment, bundle)
        }
    }

    private fun setupRecyclerView() {
        binding.rvWishlist.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = wishlistAdapter
            addItemDecoration(VerticalItemDecoration())
        }
    }

    private fun observeWishlist() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.wishlist.collectLatest {
                    when (it) {
                        is Resource.Loading -> {
                            binding.progressbarWishlist.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressbarWishlist.visibility = View.GONE
                            val products = it.data?.map { wishlist -> wishlist.product }
                            if (products.isNullOrEmpty()) {
                                binding.tvEmptyWishlist.visibility = View.VISIBLE
                                binding.rvWishlist.visibility = View.GONE
                            } else {
                                binding.tvEmptyWishlist.visibility = View.GONE
                                binding.rvWishlist.visibility = View.VISIBLE
                                wishlistAdapter.differ.submitList(products)
                            }
                        }
                        is Resource.Error -> {
                            binding.progressbarWishlist.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Không thể tải danh sách yêu thích",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
