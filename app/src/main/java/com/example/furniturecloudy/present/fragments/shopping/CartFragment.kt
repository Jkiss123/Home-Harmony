package com.example.furniturecloudy.present.fragments.shopping

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentCartBinding
import com.example.furniturecloudy.model.adapter.CartAdapter
import com.example.furniturecloudy.model.firebase.FirebaseCommon
import com.example.furniturecloudy.model.viewmodel.CartViewmodel
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.VerticalItemDecoration
import com.example.furniturecloudy.util.formatPrice
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CartFragment : Fragment() {
    private lateinit var binding : FragmentCartBinding
    private val cartAdapter by lazy { CartAdapter() }
    private val viewModel by activityViewModels<CartViewmodel>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCartBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCartRecycleView()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.cartProduct.collect{
                    when(it){
                        is Resource.Error -> {
                            binding.progressbarCart.visibility = View.GONE
                            Toast.makeText(requireContext(),"Không thể tải giỏ hàng. Vui lòng thử lại",Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {binding.progressbarCart.visibility = View.VISIBLE}
                        is Resource.Success -> {
                            binding.progressbarCart.visibility = View.GONE
                            if (it.data!!.isEmpty()){
                                showEmtyCart()
                                hideOtherView()
                            }else {
                                hideEmtyCart()
                                showOtherView()
                                cartAdapter.differ.submitList(it.data)
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }

        //setUp cong tru chi tiet
        cartAdapter.onProductClick = {
            val bundle = Bundle().apply { putParcelable("product",it.product) }
            findNavController().navigate(R.id.action_cartFragment_to_productDetailFragment,bundle)
        }

        cartAdapter.onPlusClick = {
            viewModel.ChangeQuantity(it,FirebaseCommon.QuantityStatus.INCREASE)
        }
        cartAdapter.onMinusClick = {
            viewModel.ChangeQuantity(it,FirebaseCommon.QuantityStatus.DECREASE)
        }


        // setup tong tien
        var totalPrice = 0f
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.productPrice.collectLatest {price ->

                    price?.let {
                        totalPrice = price.toFloat()
                        binding.tvTotalPrice.text = "$ ${totalPrice.formatPrice()}"
                    }

                }
            }
        }

        // Setup xóa item khi còn 1
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.deleteDialog.collectLatest{
                        val alertDialog =AlertDialog.Builder(requireContext()).apply {
                            setTitle("xóa item")
                                .setMessage("Bạn có thực sự muốn xóa item không")
                                .setNegativeButton("Không"){dialog,_ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton("Có"){dialog,_ ->
                                    viewModel.deleteCartProduct(it)
                                    dialog.dismiss()
                                }
                        }
                    alertDialog.create().show()
                }}}
        binding.buttonCheckout.setOnClickListener {
            val aciton = CartFragmentDirections.actionCartFragmentToBillingFragment(totalPrice,cartAdapter.differ.currentList.toTypedArray())
            findNavController().navigate(aciton)
        }

        binding.imageCloseCart.setOnClickListener {
            findNavController().navigateUp()
        }

    }




    private fun setupCartRecycleView() {
        binding.rvCart.apply {
            layoutManager = LinearLayoutManager(requireContext(),RecyclerView.VERTICAL,false)
            adapter = cartAdapter
            addItemDecoration(VerticalItemDecoration())
        }
    }





    private fun hideEmtyCart() {
        binding.apply {
            layoutCartEmpty.visibility = View.GONE
        }
    }

    private fun showEmtyCart() {
        binding.apply {
            layoutCartEmpty.visibility = View.VISIBLE
        }
    }

    private fun showOtherView() {
        binding.apply {
            rvCart.visibility = View.VISIBLE
            totalBoxContainer.visibility= View.VISIBLE
            buttonCheckout.visibility = View.VISIBLE
        }
    }

    private fun hideOtherView() {
        binding.apply {
            rvCart.visibility = View.GONE
            totalBoxContainer.visibility= View.GONE
            buttonCheckout.visibility = View.GONE
        }
    }
}
