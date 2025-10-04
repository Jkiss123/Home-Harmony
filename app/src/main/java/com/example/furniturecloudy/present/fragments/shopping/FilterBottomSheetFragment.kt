package com.example.furniturecloudy.present.fragments.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.ProductFilter
import com.example.furniturecloudy.data.SortOption
import com.example.furniturecloudy.databinding.BottomSheetFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFilterBinding
    private var currentFilter: ProductFilter = ProductFilter()
    var onFilterApplied: ((ProductFilter) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get current filter from arguments
        arguments?.getParcelable<ProductFilter>("currentFilter")?.let {
            currentFilter = it
            applyCurrentFilter()
        }

        setupClickListeners()
    }

    private fun applyCurrentFilter() {
        // Set price range
        if (currentFilter.minPrice > 0f) {
            binding.etMinPrice.setText(currentFilter.minPrice.toString())
        }
        if (currentFilter.maxPrice < Float.MAX_VALUE) {
            binding.etMaxPrice.setText(currentFilter.maxPrice.toString())
        }

        // Set checkboxes
        binding.cbInStockOnly.isChecked = currentFilter.inStockOnly
        binding.cbOnSaleOnly.isChecked = currentFilter.onSaleOnly

        // Set sort option
        when (currentFilter.sortBy) {
            SortOption.NONE -> binding.chipSortDefault.isChecked = true
            SortOption.PRICE_LOW_TO_HIGH -> binding.chipSortPriceLow.isChecked = true
            SortOption.PRICE_HIGH_TO_LOW -> binding.chipSortPriceHigh.isChecked = true
            SortOption.RATING_HIGH_TO_LOW -> binding.chipSortRating.isChecked = true
            SortOption.NAME_A_TO_Z -> binding.chipSortName.isChecked = true
            else -> binding.chipSortDefault.isChecked = true
        }
    }

    private fun setupClickListeners() {
        binding.btnCloseFilter.setOnClickListener {
            dismiss()
        }

        binding.btnResetFilter.setOnClickListener {
            resetFilter()
        }

        binding.btnApplyFilter.setOnClickListener {
            applyFilter()
        }
    }

    private fun resetFilter() {
        binding.etMinPrice.text?.clear()
        binding.etMaxPrice.text?.clear()
        binding.cbInStockOnly.isChecked = false
        binding.cbOnSaleOnly.isChecked = false
        binding.chipSortDefault.isChecked = true
    }

    private fun applyFilter() {
        val minPrice = binding.etMinPrice.text.toString().toFloatOrNull() ?: 0f
        val maxPrice = binding.etMaxPrice.text.toString().toFloatOrNull() ?: Float.MAX_VALUE

        val sortBy = when {
            binding.chipSortPriceLow.isChecked -> SortOption.PRICE_LOW_TO_HIGH
            binding.chipSortPriceHigh.isChecked -> SortOption.PRICE_HIGH_TO_LOW
            binding.chipSortRating.isChecked -> SortOption.RATING_HIGH_TO_LOW
            binding.chipSortName.isChecked -> SortOption.NAME_A_TO_Z
            else -> SortOption.NONE
        }

        val filter = ProductFilter(
            minPrice = minPrice,
            maxPrice = maxPrice,
            inStockOnly = binding.cbInStockOnly.isChecked,
            onSaleOnly = binding.cbOnSaleOnly.isChecked,
            sortBy = sortBy
        )

        onFilterApplied?.invoke(filter)
        dismiss()
    }

    companion object {
        fun newInstance(currentFilter: ProductFilter): FilterBottomSheetFragment {
            return FilterBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("currentFilter", currentFilter)
                }
            }
        }
    }
}
