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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.furniturecloudy.databinding.FragmentOrdersBinding
import com.example.furniturecloudy.model.adapter.OrdersAdapter
import com.example.furniturecloudy.model.viewmodel.AllOrdersViewmodel
import com.example.furniturecloudy.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
@AndroidEntryPoint
class AllOrdersFragment : Fragment() {
    private lateinit var binding: FragmentOrdersBinding
    val viewModel by viewModels<AllOrdersViewmodel>()
    val ordersAdapter by lazy { OrdersAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrdersBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupOrderAdapter()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.allOrder.collectLatest {
                    when(it){
                        is Resource.Loading -> {
                            binding.progressbarAllOrders.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressbarAllOrders.visibility = View.GONE
                            ordersAdapter.differ.submitList(it.data)
                            if (it.data.isNullOrEmpty()) {
                                binding.tvEmptyOrders.visibility = View.VISIBLE
                            }
                        }
                        is Resource.Error -> {
                            Toast.makeText(requireContext(), "Không thể tải danh sách đơn hàng. Vui lòng thử lại", Toast.LENGTH_SHORT).show()
                            binding.progressbarAllOrders.visibility = View.GONE
                        }
                        else -> Unit
                    }
                }
            }
        }

        ordersAdapter.onClick = {
            val action = AllOrdersFragmentDirections.actionOrdersFragmentToOrderDetailFragment(it)
            findNavController().navigate(action)
        }

        binding.imageCloseOrders.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupOrderAdapter() {
        binding.rvAllOrders.apply {
            layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false)
            adapter = ordersAdapter
        }
    }

}