package com.example.furniturecloudy.present.fragments.shopping

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.furniturecloudy.model.adapter.PaymentMethodAdapter
import com.example.furniturecloudy.model.viewmodel.AddressViewmodel
import com.example.furniturecloudy.model.viewmodel.BillingViewmodel
import com.example.furniturecloudy.model.viewmodel.OrderViewmodel
import com.example.furniturecloudy.util.HorizontalcalItemDecoration
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.formatPrice
import com.example.furniturecloudy.utils.payment.MoMoConfig
import com.example.furniturecloudy.utils.payment.MoMoPaymentHelper
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BillingFragment : Fragment() {
    private lateinit var binding:FragmentBillingBinding
    private val addresAdapter  by lazy { AddressAdapter() }
    private val billingAdapter by lazy { BillingProductAdapter() }
    private val paymentAdapter by lazy { PaymentMethodAdapter() }
    private val viewmodel  by viewModels<BillingViewmodel>()
    private val args by navArgs<BillingFragmentArgs>()
    private var listcarts = emptyList<CartProducts>()
    private var totalPrice : Float = 0f
    private var selectedAddress : Address? = null
    private var selectedPaymentMethod : String = "COD"
    private val orderViewModel  by viewModels<OrderViewmodel>()
    private val addressViewmodel by viewModels<AddressViewmodel>()
    private lateinit var moMoPaymentHelper: MoMoPaymentHelper
    private var pendingOrder: Order? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listcarts = args.cartProduct.toList()
        totalPrice= args.totalPrice

        // Initialize MoMo SDK and Payment Helper
        MoMoPaymentHelper.initialize(isDevelopment = MoMoConfig.IS_DEVELOPMENT)
        moMoPaymentHelper = MoMoConfig.createPaymentHelper()
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
        setupPaymentAdapter()
        // observeDeleteAddress() // Comment out để tránh conflict với AddressFragment
        //
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewmodel.address.collectLatest {
                    when(it){
                        is Resource.Error -> {
                            binding.progressbarAddress.visibility = View.GONE
                            Toast.makeText(requireContext(),"Không thể tải danh sách địa chỉ. Vui lòng thử lại",Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.progressbarAddress.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressbarAddress.visibility = View.GONE
                            addresAdapter.differ.submitList(it.data)
                            addresAdapter.resetSelection()
                            // Reset selectedAddress if it's not in current list
                            if (selectedAddress != null && it.data?.none { addr -> addr.id == selectedAddress?.id } == true) {
                                selectedAddress = null
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
        //
        binding.imageAddAddress.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable("address", null)
            }
            findNavController().navigate(R.id.action_billingFragment_to_addressFragment, bundle)
        }
        binding.tvTotalPrice.text = "$ ${totalPrice.formatPrice()}"
        binding.imageCloseBilling.setOnClickListener {
            findNavController().navigateUp()
        }
        addresAdapter.onClick = {
            selectedAddress = it
        }

        addresAdapter.onEditClick = { address ->
            val bundle = Bundle().apply {
                putParcelable("address", address)
            }
            findNavController().navigate(R.id.action_billingFragment_to_addressFragment, bundle)
        }

        addresAdapter.onDeleteClick = { address ->
            showDeleteAddressDialog(address)
        }

        binding.buttonPlaceOrder.setOnClickListener {
            if (selectedAddress == null){
                Toast.makeText(requireContext(),"Vui lòng chọn địa chỉ giao hàng",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showOrderConfirmDialog(selectedPaymentMethod)
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


    private fun showOrderConfirmDialog(paymentMethod: String) {
        val paymentMethodDisplay = when(paymentMethod) {
            "COD" -> "Tiền mặt khi nhận hàng"
            "MoMo" -> "MoMo"
            "VNPay" -> "VNPay"
            "ZaloPay" -> "ZaloPay"
            else -> "Tiền mặt khi nhận hàng"
        }

        val alertDialog = AlertDialog.Builder(requireContext()).apply {
            setTitle("Đặt Hàng")
                .setMessage("Xác nhận đặt hàng với phương thức thanh toán: $paymentMethodDisplay?")
                .setNegativeButton("Không"){dialog,_ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Có"){dialog,_ ->
                    dialog.dismiss()

                    if (paymentMethod == "MoMo") {
                        // Handle MoMo payment
                        handleMoMoPayment()
                    } else {
                        // Handle other payment methods (COD, VNPay, ZaloPay)
                        val paymentStatus = if (paymentMethod == "COD") "PENDING" else "PENDING"
                        val order = Order(
                            orderStatus = OrderStatus.Ordered.status,
                            totalPrice = totalPrice,
                            products = listcarts,
                            address = selectedAddress!!,
                            paymentMethod = paymentMethod,
                            paymentStatus = paymentStatus
                        )
                        orderViewModel.placeOrder(order)
                    }
                }
        }
        alertDialog.create().show()
    }

    private fun handleMoMoPayment() {
        // Check if MoMo app is installed
        if (!MoMoPaymentHelper.isMoMoAppInstalled(requireActivity())) {
            AlertDialog.Builder(requireContext()).apply {
                setTitle("Thông báo")
                setMessage("Bạn cần cài đặt ứng dụng MoMo để sử dụng phương thức thanh toán này. Bạn có muốn tiếp tục với phương thức COD không?")
                setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }
                setPositiveButton("Chuyển sang COD") { dialog, _ ->
                    selectedPaymentMethod = "COD"
                    val order = Order(
                        orderStatus = OrderStatus.Ordered.status,
                        totalPrice = totalPrice,
                        products = listcarts,
                        address = selectedAddress!!,
                        paymentMethod = "COD",
                        paymentStatus = "PENDING"
                    )
                    orderViewModel.placeOrder(order)
                    dialog.dismiss()
                }
            }.create().show()
            return
        }

        try {
            // Create pending order
            pendingOrder = Order(
                orderStatus = OrderStatus.Ordered.status,
                totalPrice = totalPrice,
                products = listcarts,
                address = selectedAddress!!,
                paymentMethod = "MoMo",
                paymentStatus = "PENDING"
            )

            // Convert price to VND (assuming current price is in USD, 1 USD = 25000 VND)
            val amountInVND = (totalPrice * 25000).toLong()

            // Request MoMo payment
            moMoPaymentHelper.requestPayment(
                activity = requireActivity(),
                amount = amountInVND,
                orderId = pendingOrder!!.orderId,
                description = "Thanh toán đơn hàng ${pendingOrder!!.orderId}"
            )

        } catch (e: Exception) {
            Log.e("BillingFragment", "Error requesting MoMo payment: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "Không thể kết nối với MoMo. Vui lòng thử lại.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle MoMo payment result
        val moMoResult = moMoPaymentHelper.handlePaymentResult(requestCode, resultCode, data)
        moMoResult?.let { result ->
            if (result.isSuccessful()) {
                // Payment successful - place order with transaction info
                pendingOrder?.let { order ->
                    val updatedOrder = order.copy(
                        paymentStatus = "PAID",
                        paymentTransactionId = result.token
                    )
                    orderViewModel.placeOrder(updatedOrder)
                    pendingOrder = null

                    Toast.makeText(
                        requireContext(),
                        "Thanh toán MoMo thành công!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Payment failed or cancelled
                Toast.makeText(
                    requireContext(),
                    "Thanh toán thất bại: ${result.getErrorMessage()}",
                    Toast.LENGTH_LONG
                ).show()

                // Ask user if they want to retry or switch to COD
                showPaymentFailedDialog()
            }
        }
    }

    private fun showPaymentFailedDialog() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Thanh toán thất bại")
            setMessage("Bạn có muốn thử lại hoặc chuyển sang thanh toán khi nhận hàng (COD)?")
            setNegativeButton("Hủy") { dialog, _ ->
                pendingOrder = null
                dialog.dismiss()
            }
            setPositiveButton("Thử lại MoMo") { dialog, _ ->
                dialog.dismiss()
                handleMoMoPayment()
            }
            setNeutralButton("Chuyển sang COD") { dialog, _ ->
                pendingOrder?.let { order ->
                    val updatedOrder = order.copy(
                        paymentMethod = "COD",
                        paymentStatus = "PENDING",
                        paymentTransactionId = null
                    )
                    orderViewModel.placeOrder(updatedOrder)
                    pendingOrder = null
                }
                dialog.dismiss()
            }
        }.create().show()
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

    private fun setupPaymentAdapter() {
        binding.rvPaymentMethods.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = paymentAdapter
            addItemDecoration(HorizontalcalItemDecoration())
        }

        paymentAdapter.onPaymentMethodSelected = { method ->
            selectedPaymentMethod = method
        }
    }
}