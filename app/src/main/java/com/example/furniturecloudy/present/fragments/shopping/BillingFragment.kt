package com.example.furniturecloudy.present.fragments.shopping

import android.app.AlertDialog
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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.Address
import com.example.furniturecloudy.data.CartProducts
import com.example.furniturecloudy.data.Order
import com.example.furniturecloudy.data.OrderStatus
import com.example.furniturecloudy.databinding.FragmentAddressBinding
import com.example.furniturecloudy.databinding.FragmentBillingBinding
import com.example.furniturecloudy.model.adapter.AddressAdapter
import com.example.furniturecloudy.model.adapter.BillingProductAdapter
import com.example.furniturecloudy.model.viewmodel.AddressViewmodel
import com.example.furniturecloudy.model.viewmodel.BillingViewmodel
import com.example.furniturecloudy.model.viewmodel.OrderViewmodel
import com.example.furniturecloudy.util.HorizontalcalItemDecoration
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.formatPrice
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BillingFragment : Fragment() {
    private lateinit var binding:FragmentBillingBinding
    private val addresAdapter  by lazy { AddressAdapter() }
    private val billingAdapter by lazy { BillingProductAdapter() }
    private val viewmodel  by viewModels<BillingViewmodel>()
    private val args by navArgs<BillingFragmentArgs>()
    private var listcarts = emptyList<CartProducts>()
    private var totalPrice : Float = 0f
    private var selectedAddress : Address? = null
    private val orderViewModel  by viewModels<OrderViewmodel>()
    private val addressViewmodel by viewModels<AddressViewmodel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listcarts = args.cartProduct.toList()
        totalPrice= args.totalPrice
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBillingBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBillingAdapter()
        setupAddressAdapter()
        observeDeleteAddress()
        //
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.address.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            binding.progressbarAddress.visibility = View.GONE
                            Toast.makeText(requireContext(),"Không thể tải danh sách địa chỉ. Vui lòng thử lại",Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {binding.progressbarAddress.visibility = View.VISIBLE}
                        is Resource.Success -> {
                            binding.progressbarAddress.visibility = View.GONE
                            addresAdapter.differ.submitList(it.data)
                        }
                        else -> Unit
                    }
                }
            }
        }
        //
        binding.imageAddAddress.setOnClickListener {
            findNavController().navigate(R.id.action_billingFragment_to_addressFragment)
        }
        binding.tvTotalPrice.text = "$ ${totalPrice.formatPrice()}}"
        binding.imageCloseBilling.setOnClickListener {
            findNavController().navigateUp()
        }
        addresAdapter.onClick = {
            selectedAddress = it
        }

        addresAdapter.onEditClick = { address ->
            val action = BillingFragmentDirections.actionBillingFragmentToAddressFragment()
            findNavController().navigate(action)
        }

        addresAdapter.onDeleteClick = { address ->
            showDeleteAddressDialog(address)
        }

        binding.buttonPlaceOrder.setOnClickListener {
            if (selectedAddress == null){
                Toast.makeText(requireContext(),"Vui lòng chọn địa chỉ giao hàng",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showOrderConfirmDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                orderViewModel.order.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            Toast.makeText(requireContext(),"Đặt hàng thất bại. Vui lòng thử lại",Toast.LENGTH_SHORT).show()
                            binding.buttonPlaceOrder.stopAnimation()
                        }
                        is Resource.Loading -> {binding.buttonPlaceOrder.startAnimation()}
                        is Resource.Success -> {
                            binding.buttonPlaceOrder.stopAnimation()
                            findNavController().navigateUp()
                            Snackbar.make(requireView(),"Đặt Hàng Thành Công",Snackbar.LENGTH_SHORT).show()
                        }
                        else -> Unit
                    }
                }
            }
        }

    }

    private fun showOrderConfirmDialog() {
        val alertDialog = AlertDialog.Builder(requireContext()).apply {
            setTitle("Đạt Hàng")
                .setMessage("Bạn confirm muốn đặt hàng chứ")
                .setNegativeButton("Không"){dialog,_ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Có"){dialog,_ ->
                    val order = Order(OrderStatus.Ordered.status,totalPrice,listcarts,selectedAddress!!)
                    orderViewModel.placeOrder(order)
                    dialog.dismiss()
                }
        }
        alertDialog.create().show()
    }

    private fun setupBillingAdapter() {
        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext(),RecyclerView.HORIZONTAL,false)
            adapter = billingAdapter
            billingAdapter.differ.submitList(listcarts)
            addItemDecoration(HorizontalcalItemDecoration())
        }
    }

    private fun setupAddressAdapter() {
        binding.rvAddress.apply {
            layoutManager = LinearLayoutManager(requireContext(),RecyclerView.HORIZONTAL,false)
            adapter = addresAdapter
            addItemDecoration(HorizontalcalItemDecoration())
        }
    }

    private fun showDeleteAddressDialog(address: Address) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Xóa địa chỉ")
            setMessage("Bạn có chắc chắn muốn xóa địa chỉ này?")
            setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton("Xóa") { dialog, _ ->
                addressViewmodel.deleteAddress(address.id)
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun observeDeleteAddress() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                addressViewmodel.deleteAddress.collectLatest {
                    when (it) {
                        is Resource.Success -> {
                            Toast.makeText(requireContext(), "Đã xóa địa chỉ", Toast.LENGTH_SHORT).show()
                            // No need to manually refresh - addSnapshotListener auto-updates
                        }
                        is Resource.Error -> {
                            Toast.makeText(requireContext(), "Xóa địa chỉ thất bại. Vui lòng thử lại", Toast.LENGTH_SHORT).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}