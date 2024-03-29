package com.example.furniturecloudy.present.fragments.categories

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.furniturecloudy.data.Category
import com.example.furniturecloudy.model.viewmodel.BaseCategoryViewmodel
import com.example.furniturecloudy.util.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChairFragment : BaseCategoryFragment() {
    @Inject lateinit var factory: BaseCategoryViewmodel.Factory
    private val viewmodel :BaseCategoryViewmodel by viewModels{
        BaseCategoryViewmodel.prodieCateforyViewModelFactory(factory,Category.Chair)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.offerProducts.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            Snackbar.make(requireView(),it.message.toString(),Snackbar.LENGTH_SHORT).show()
                            hideOfferLoading()
                        }
                        is Resource.Loading -> showOfferLoading()
                        is Resource.Success -> {
                            hideOfferLoading()
                            offerAdapter.differ.submitList(it.data)
                        }
                        else -> Unit
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.bestProducts.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            hideDealsLoading()
                            Snackbar.make(requireView(),it.message.toString(),Snackbar.LENGTH_SHORT).show()
                        }
                        is Resource.Loading ->{showDealsLoading()}
                        is Resource.Success -> {
                            hideDealsLoading()
                            bestProductsAdapter.differ.submitList(it.data)
                        }
                        else -> Unit
                    }
                }
            }
        }



    }

}