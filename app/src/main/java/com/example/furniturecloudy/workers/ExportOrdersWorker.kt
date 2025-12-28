package com.example.furniturecloudy.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ForegroundInfo
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportOrdersWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val firestore = FirebaseFirestore.getInstance()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val TAG = "E2_ExportWorker"
        private const val CHANNEL_ID = "export_orders_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ E2 AFTER: WorkManager Background Export")
        Log.d(TAG, "✅ Running in background - User can use other apps!")

        return try {
            // Create notification channel
            createNotificationChannel()

            // Set as foreground service to prevent killing
            setForeground(createForegroundInfo(0, "Đang khởi động..."))

            // Step 1: Fetch orders from Firestore
            Log.d(TAG, "Step 1: Fetching orders from Firestore...")
            setForeground(createForegroundInfo(10, "Đang tải dữ liệu từ server..."))

            val orders = fetchOrders()

            delay(2000) // Delay để dễ quan sát tiến trình

            Log.d(TAG, "Fetched ${orders.size} orders")
            setForeground(createForegroundInfo(30, "Đã tải ${orders.size} đơn hàng"))

            // Step 2: Process and generate CSV
            Log.d(TAG, "Step 2: Generating CSV file...")
            setForeground(createForegroundInfo(50, "Đang xử lý ${orders.size} đơn hàng..."))

            delay(3000) // Delay để dễ quan sát tiến trình

            val csvFile = generateCSV(orders)

            setForeground(createForegroundInfo(80, "Đang lưu file..."))

            delay(2000) // Delay để dễ quan sát tiến trình

            Log.d(TAG, "CSV file created: ${csvFile.absolutePath}")
            setForeground(createForegroundInfo(100, "Hoàn tất!"))

            val totalTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ Export completed in ${totalTime}ms")
            Log.d(TAG, "✅ User was FREE to use other apps during this time!")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Show completion notification
            showCompletionNotification(csvFile)

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Export failed: ${e.message}", e)
            Log.d(TAG, "⚠️ WorkManager will auto-retry this task!")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            showErrorNotification(e.message ?: "Unknown error")

            // Auto retry on failure
            Result.retry()
        }
    }

    /**
     * Fetch orders from Firestore
     */
    private suspend fun fetchOrders(): List<Order> {
        return try {
            val snapshot = firestore.collection("orders")
                .limit(100) // Limit for demo
                .get()
                .await()

            snapshot.toObjects(Order::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch orders: ${e.message}")
            // Return demo data if Firestore fails
            createDemoOrders()
        }
    }

    /**
     * Create demo orders for testing
     */
    private fun createDemoOrders(): List<Order> {
        Log.d(TAG, "Using demo data for export")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        return (1..50).map { i ->
            val timestamp = System.currentTimeMillis() - (i * 86400000L) // i days ago
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
    private fun generateCSV(orders: List<Order>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "orders_export_$timestamp.csv"

        // Save to app's external files directory
        val file = File(context.getExternalFilesDir(null), fileName)

        FileWriter(file).use { writer ->
            // CSV Header
            writer.append("Order ID,Status,Total Price,Date\n")

            // CSV Data
            orders.forEach { order ->
                writer.append("${order.orderId},")
                writer.append("${order.orderStatus},")
                writer.append("${order.totalPrice},")
                writer.append("${order.date}\n")
            }
        }

        return file
    }

    /**
     * Create notification channel (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Xuất dữ liệu đơn hàng",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị tiến trình xuất dữ liệu đơn hàng"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground info with progress
     */
    private fun createForegroundInfo(progress: Int, message: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Xuất báo cáo đơn hàng")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Show completion notification with file access
     */
    private fun showCompletionNotification(file: File) {
        // Create intent to open CSV file
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "text/csv")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent.createChooser(intent, "Mở file với"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("✅ Xuất báo cáo thành công!")
            .setContentText("Tap để xem file CSV")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * Show error notification
     */
    private fun showErrorNotification(error: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("❌ Xuất báo cáo thất bại")
            .setContentText("Lỗi: $error. WorkManager sẽ tự động thử lại.")
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }
}
