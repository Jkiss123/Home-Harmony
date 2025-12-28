package com.example.furniturecloudy.util

import android.app.ProgressDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.furniturecloudy.data.Order
import com.example.furniturecloudy.workers.ExportOrdersWorker
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportOrdersHelper {

    private const val TAG = "E2_ExportHelper"

    private const val USE_WORKMANAGER_E2 = false  // Change to true for AFTER demo
    fun exportOrders(context: Context) {
        if (USE_WORKMANAGER_E2) {
            exportOrders_AFTER(context)
        } else {
            exportOrders_BEFORE(context)
        }
    }

    private fun exportOrders_BEFORE(context: Context) {
        val startTime = System.currentTimeMillis()

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "❌ E2 BEFORE: Blocking ProgressDialog")
        Log.d(TAG, "⚠️ User MUST WAIT - Cannot leave app!")

        // Create blocking ProgressDialog
        val progressDialog = ProgressDialog(context).apply {
            setTitle("Xuất báo cáo đơn hàng")
            setMessage("Đang khởi động...")
            setCancelable(false)  // ❌ User cannot cancel!
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = 100
            show()
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Step 1: Fetch orders
                progressDialog.setMessage("Đang tải dữ liệu từ server...")
                progressDialog.progress = 10

                delay(2000) // Delay để dễ quan sát tiến trình

                val orders = withContext(Dispatchers.IO) {
                    fetchOrders()
                }

                Log.d(TAG, "Fetched ${orders.size} orders")
                progressDialog.setMessage("Đã tải ${orders.size} đơn hàng")
                progressDialog.progress = 30

                // Step 2: Process data
                progressDialog.setMessage("Đang xử lý ${orders.size} đơn hàng...")
                progressDialog.progress = 50

                delay(3000) // Delay để dễ quan sát tiến trình

                // Step 3: Generate CSV
                progressDialog.setMessage("Đang lưu file...")
                progressDialog.progress = 80

                val csvFile = withContext(Dispatchers.IO) {
                    generateCSV(context, orders)
                }

                delay(2000)

                progressDialog.progress = 100

                val totalTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "❌ Export completed in ${totalTime}ms")
                Log.d(TAG, "❌ User was BLOCKED for ${totalTime}ms!")
                Log.d(TAG, "❌ Could NOT press Home or use other apps!")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                progressDialog.dismiss()

                // Show success
                Toast.makeText(
                    context,
                    "✅ Xuất thành công!\nFile: ${csvFile.name}",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Export failed: ${e.message}", e)
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                progressDialog.dismiss()

                Toast.makeText(
                    context,
                    "❌ Xuất thất bại: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun exportOrders_AFTER(context: Context) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ E2 AFTER: WorkManager Background Export")
        Log.d(TAG, "✅ User can LEAVE APP immediately!")
        Log.d(TAG, "✅ WorkManager will continue in background")

        // Create work request with constraints
        val exportWork = OneTimeWorkRequestBuilder<ExportOrdersWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        // Enqueue work
        WorkManager.getInstance(context).enqueue(exportWork)

        Log.d(TAG, "✅ Work enqueued! User can now leave app.")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Show toast and user can leave immediately!
        Toast.makeText(
            context,
            "📊 Đang xuất báo cáo ở background...\n✅ Bạn có thể về Home hoặc dùng app khác!",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Fetch orders from Firestore
     */
    private suspend fun fetchOrders(): List<Order> {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("orders")
                .limit(100)
                .get()
                .await()

            snapshot.toObjects(Order::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch orders, using demo data: ${e.message}")
            // Return demo data if Firestore fails
            createDemoOrders()
        }
    }

    /**
     * Create demo orders for testing
     */
    private fun createDemoOrders(): List<Order> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        return (1..50).map { i ->
            val timestamp = System.currentTimeMillis() - (i * 86400000L)
            Order(
                orderId = "ORD-${10000 + i}",
                orderStatus = if (i % 3 == 0) "Delivered" else if (i % 3 == 1) "Shipping" else "Pending",
                totalPrice = (100000 + i * 5000).toFloat(),
                date = dateFormat.format(Date(timestamp))
            )
        }
    }

    /**
     * Generate CSV file from orders
     */
    private fun generateCSV(context: Context, orders: List<Order>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "orders_export_$timestamp.csv"

        val file = File(context.getExternalFilesDir(null), fileName)

        FileWriter(file).use { writer ->
            writer.append("Order ID,Status,Total Price,Date\n")

            orders.forEach { order ->
                writer.append("${order.orderId},")
                writer.append("${order.orderStatus},")
                writer.append("${order.totalPrice},")
                writer.append("${order.date}\n")
            }
        }

        return file
    }
}
