package com.example.furniturecloudy.present.fragments.shopping

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.furniturecloudy.data.OrderStatus
import com.example.furniturecloudy.data.getOrderStatus
import com.example.furniturecloudy.databinding.FragmentOrderDetailBinding
import com.example.furniturecloudy.model.adapter.BillingProductAdapter
import com.example.furniturecloudy.util.OrderPdfGenerator
import com.example.furniturecloudy.util.VerticalItemDecoration
import com.example.furniturecloudy.util.formatPrice
import com.example.furniturecloudy.workers.ExportSingleOrderWorker
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderDetailFragment : Fragment() {

    companion object {
        private const val TAG = "E2_OrderDetail"
        private const val USE_WORKMANAGER_PDF = false
    }

    private lateinit var binding:FragmentOrderDetailBinding
    private val billingAdapter by lazy { BillingProductAdapter() }
    private val args by navArgs<OrderDetailFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val order = args.order
        setupBillingAdapter()
        billingAdapter.differ.submitList(order.products)
        binding.apply {
            tvOrderId.text = "Order #${order.orderId}"

            stepView.setSteps(
                mutableListOf(
                    OrderStatus.Ordered.status,
                    OrderStatus.Confirmed.status,
                    OrderStatus.Shipped.status,
                    OrderStatus.Delivered.status,
                )
            )

            val currentOrderState = when (getOrderStatus(order.orderStatus)) {
                is OrderStatus.Ordered -> 0
                is OrderStatus.Confirmed -> 1
                is OrderStatus.Shipped -> 2
                is OrderStatus.Delivered -> 3
                else -> 0
            }
            stepView.go(currentOrderState, false)
            if (currentOrderState == 3) {
                stepView.done(true)
            }

            tvFullName.text = order.address.fullName
            tvAddress.text = "${order.address.wards} ${order.address.city}"
            tvPhoneNumber.text = order.address.phone
            tvTotalPrice.text = "$ ${order.totalPrice.formatPrice()}"
        }

        binding.imageCloseOrder.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.imageExportOrderPdf.setOnClickListener {
            exportOrderToPdf(order)
        }
    }

    private fun exportOrderToPdf(order: com.example.furniturecloudy.data.Order) {
        if (USE_WORKMANAGER_PDF) {
            exportOrderToPdfAFTER(order)
        } else {
            exportOrderToPdfBEFORE(order)
        }
    }

    @Suppress("DEPRECATION")
    private fun exportOrderToPdfBEFORE(order: com.example.furniturecloudy.data.Order) {
        val startTime = System.currentTimeMillis()

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "❌ E2 OPTION C BEFORE: Blocking PDF Export")
        Log.d(TAG, "❌ User MUST wait, cannot leave app!")

        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Đang xuất đơn hàng ${order.orderId} thành PDF...")
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = 100
            show()
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Step 1: Preparation
                progressDialog.setMessage("Đang chuẩn bị...")
                progressDialog.progress = 10

                delay(500) // Delay để dễ quan sát tiến trình

                // Step 2: Generate PDF
                progressDialog.setMessage("Đang tạo file PDF...")
                progressDialog.progress = 30

                delay(1000) // Delay để dễ quan sát tiến trình

                val pdfFile = withContext(Dispatchers.IO) {
                    OrderPdfGenerator.generateOrderPdf(requireContext(), order)
                }

                progressDialog.setMessage("Đang hoàn tất...")
                progressDialog.progress = 80

                delay(500) // Delay để dễ quan sát tiến trình

                progressDialog.progress = 100

                val totalTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "❌ Export completed in ${totalTime}ms")
                Log.d(TAG, "❌ User was BLOCKED for ${totalTime}ms!")
                Log.d(TAG, "❌ Could NOT press Home or use other apps!")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                progressDialog.dismiss()

                Toast.makeText(
                    requireContext(),
                    "✅ Xuất đơn hàng ${order.orderId} thành công!\n📄 File: ${pdfFile.name}",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Export failed: ${e.message}", e)
                progressDialog.dismiss()

                Toast.makeText(
                    requireContext(),
                    "❌ Xuất thất bại: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun exportOrderToPdfAFTER(order: com.example.furniturecloudy.data.Order) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ E2 OPTION C AFTER: WorkManager PDF Export")
        Log.d(TAG, "✅ User can leave app immediately!")

        val orderJson = Gson().toJson(order)

        val inputData = Data.Builder()
            .putString(ExportSingleOrderWorker.KEY_ORDER_JSON, orderJson)
            .build()

        val exportWork = OneTimeWorkRequestBuilder<ExportSingleOrderWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(requireContext()).enqueue(exportWork)

        Toast.makeText(
            requireContext(),
            "📄 Đang xuất đơn hàng ${order.orderId} thành PDF...\n✅ Bạn có thể về Home!",
            Toast.LENGTH_LONG
        ).show()

        Log.d(TAG, "✅ WorkManager enqueued - User is FREE to leave!")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun setupBillingAdapter() {
        binding.rvProducts.apply {
            adapter =billingAdapter
            layoutManager = LinearLayoutManager(requireContext(),RecyclerView.VERTICAL,false)
            addItemDecoration(VerticalItemDecoration())
        }
    }
}