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

        // Set sort option using ChipGroup check method for single selection
        val chipIdToCheck = when (currentFilter.sortBy) {
            SortOption.NONE -> R.id.chipSortDefault
            SortOption.PRICE_LOW_TO_HIGH -> R.id.chipSortPriceLow
            SortOption.PRICE_HIGH_TO_LOW -> R.id.chipSortPriceHigh
            SortOption.RATING_HIGH_TO_LOW -> R.id.chipSortRating
            SortOption.NAME_A_TO_Z -> R.id.chipSortName
            else -> R.id.chipSortDefault
        }
        binding.chipGroupSort.check(chipIdToCheck)
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
        binding.chipGroupSort.check(R.id.chipSortDefault)
    }

    private fun applyFilter() {
        val minPrice = binding.etMinPrice.text.toString().toFloatOrNull() ?: 0f
        val maxPrice = binding.etMaxPrice.text.toString().toFloatOrNull() ?: Float.MAX_VALUE

        val sortBy = when (binding.chipGroupSort.checkedChipId) {
            R.id.chipSortPriceLow -> SortOption.PRICE_LOW_TO_HIGH
            R.id.chipSortPriceHigh -> SortOption.PRICE_HIGH_TO_LOW
            R.id.chipSortRating -> SortOption.RATING_HIGH_TO_LOW
            R.id.chipSortName -> SortOption.NAME_A_TO_Z
            R.id.chipSortDefault -> SortOption.NONE
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
