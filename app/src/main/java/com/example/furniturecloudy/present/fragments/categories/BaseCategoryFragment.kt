package com.example.furniturecloudy.present.fragments.categories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentBaseCategoryBinding
import com.example.furniturecloudy.model.adapter.BestDealsAdapter
import com.example.furniturecloudy.model.adapter.BestProductsAdapter
import com.example.furniturecloudy.util.showBottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class BaseCategoryFragment : Fragment() {

    private lateinit var binding:FragmentBaseCategoryBinding
    protected  val offerAdapter: BestDealsAdapter by lazy { BestDealsAdapter() }
    protected  val bestProductsAdapter: BestProductsAdapter by lazy { BestProductsAdapter() }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBaseCategoryBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //SetupClickek on item
        offerAdapter.onClick = {
            val bundle = Bundle().apply { putParcelable("product",it) }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment,bundle)
        }
        bestProductsAdapter.onClick = {
            val bundle = Bundle().apply { putParcelable("product",it) }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment,bundle)
        }

        //// setup
        binding.recvOfferBaseCategory.apply {
            layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
            adapter = offerAdapter
        }
        // setup
        binding.recProductsBaseCategory.apply {
            layoutManager = GridLayoutManager(requireContext(),2,GridLayoutManager.VERTICAL,false)
            adapter = bestProductsAdapter
        }

        binding.recvOfferBaseCategory.addOnScrollListener(object :RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if(!recyclerView.canScrollVertically(1) && dx != 0){
                    onOfferPagingRequest()
                }
            }
        })

        binding.NestedScollBaseCategory.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener{ v, _, scrollY, _, _ ->
            if (v.getChildAt(0).bottom <= v.height + scrollY)
                onProductsPagingRequest()
        })
    }

    open fun onOfferPagingRequest(){

    }
    open fun onProductsPagingRequest(){

    }

    fun showOfferLoading(){
        binding.progressbarBaseCategory1.visibility = View.VISIBLE
    }

    fun hideOfferLoading(){
        binding.progressbarBaseCategory1.visibility = View.GONE
    }

    fun showDealsLoading(){
        binding.progressbarBaseCategory2.visibility = View.VISIBLE
    }

    fun hideDealsLoading(){
        binding.progressbarBaseCategory2.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        showBottomNavigationView()
    }

}