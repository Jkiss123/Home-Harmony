package com.example.furniturecloudy.present.fragments.shopping

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.CartProducts
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.databinding.FragmentProductDetailBinding
import com.example.furniturecloudy.databinding.SizeRvItemBinding
import com.example.furniturecloudy.model.adapter.ColorsAdapter
import com.example.furniturecloudy.model.adapter.SizesAdapter
import com.example.furniturecloudy.model.adapter.ViewPagerDPAdapter
import com.example.furniturecloudy.model.viewmodel.DetailViewmodel
import com.example.furniturecloudy.present.ShoppingActivity
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.hideBottomNavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductDetailFragment : Fragment() {
    private val args by navArgs<ProductDetailFragmentArgs>()
    private lateinit var binding: FragmentProductDetailBinding
    private val viewPagerAdapter by lazy { ViewPagerDPAdapter() }
    private val sizeAdapter by lazy { SizesAdapter() }
    private val colorAdapter by lazy { ColorsAdapter() }
    private val viewModel : DetailViewmodel by viewModels()
    //AddtoCart
    private var selectedColor:Int? =null
    private var selectedSize:String? =null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        hideBottomNavigationView()
        binding = FragmentProductDetailBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val product = args.product

        setupSizeRV()
        setupColorRV()
        setupImagePager()

        binding.apply {
            tvProductNameProductDetail.text = product.name
            tvProductPriceProductDetail.text = "$ ${product.price}"
            tvProductDescriptionProductDetail.text = product.description
            if (product.colors.isNullOrEmpty())
                tvProductColor.visibility = View.GONE
            if (product.sizes.isNullOrEmpty())
                tvProductSize.visibility = View.GONE
            imgvCloseDetailProduct.setOnClickListener {
                findNavController().navigateUp()
            }
        }

        viewPagerAdapter.differ.submitList(product.images)
        product.sizes?.let { sizeAdapter.differ.submitList(it) }
        product.colors?.let { colorAdapter.differ.submitList(it) }

        sizeAdapter.onItemClicked = {
            selectedSize = it
        }

        colorAdapter.onItemClicked= {
            selectedColor = it
        }

        binding.btnAddToCartDetailProdcut.setOnClickListener {
             CartProducts(product,1,selectedColor,selectedSize)
            viewModel.addUpdateProduct(CartProducts(product,1,selectedColor,selectedSize))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.addToCart.collect(){
                    when(it){
                        is Resource.Error -> {
                            binding.btnAddToCartDetailProdcut.stopAnimation()
                            Toast.makeText(requireContext(),it.message.toString(),Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> binding.btnAddToCartDetailProdcut.startAnimation()
                        is Resource.Success -> {
                            binding.btnAddToCartDetailProdcut.revertAnimation()
                            Toast.makeText(requireContext(),"Thêm Thành Công",Toast.LENGTH_SHORT).show()
                        }
                        else -> Unit
                    }
                }
            }
        }


    }

    private fun setupImagePager() {
        binding.viewpagerDetailProduct.apply {
            adapter = viewPagerAdapter
        }
    }

    private fun setupColorRV() {
        binding.rcvColorsDetailProduct.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = colorAdapter
        }
    }

    private fun setupSizeRV() {
        binding.rcvSizesDetailProduct.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = sizeAdapter
        }
    }


}