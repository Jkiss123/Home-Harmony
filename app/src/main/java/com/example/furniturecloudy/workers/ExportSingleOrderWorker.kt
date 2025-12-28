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
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.Order
import com.example.furniturecloudy.util.OrderPdfGenerator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

/**
 * E2 OPTION C: Export Single Order to PDF (Background Worker)
 *
 * This worker exports a single order to PDF in the background:
 * - Fetches full order details from Firestore
 * - Generates professional PDF invoice
 * - Shows progress notifications
 * - User can leave app while exporting
 * - Guaranteed execution even if app crashes
 */
class ExportSingleOrderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val firestore = FirebaseFirestore.getInstance()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val TAG = "E2_ExportSingleOrder"
        private const val CHANNEL_ID = "export_single_order_channel"
        private const val NOTIFICATION_ID = 2001
        const val KEY_ORDER_JSON = "order_json"
    }

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ E2 OPTION C: Export Single Order to PDF")
        Log.d(TAG, "✅ Running in background - User can use other apps!")

        return try {
            // Create notification channel
            createNotificationChannel()

            // Get order from input data
            val orderJson = inputData.getString(KEY_ORDER_JSON)
                ?: return Result.failure()

            val order = Gson().fromJson(orderJson, Order::class.java)

            Log.d(TAG, "Exporting order: ${order.orderId}")

            // Set as foreground service
            setForeground(createForegroundInfo(0, "Đang chuẩn bị..."))

            delay(500) // Delay để dễ quan sát tiến trình

            // Step 1: Generate PDF
            Log.d(TAG, "Step 1: Generating PDF...")
            setForeground(createForegroundInfo(30, "Đang tạo file PDF..."))

            delay(1000) // Delay để dễ quan sát tiến trình

            val pdfFile = OrderPdfGenerator.generateOrderPdf(context, order)

            Log.d(TAG, "PDF file created: ${pdfFile.absolutePath}")
            setForeground(createForegroundInfo(80, "Đang hoàn tất..."))

            delay(500) // Delay để dễ quan sát tiến trình

            setForeground(createForegroundInfo(100, "Hoàn tất!"))

            val totalTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ Export completed in ${totalTime}ms")
            Log.d(TAG, "✅ User was FREE to use other apps during this time!")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Show completion notification
            showCompletionNotification(pdfFile, order.orderId)

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Export failed: ${e.message}", e)
            Log.d(TAG, "⚠️ WorkManager will auto-retry this task!")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            showErrorNotification(e.message ?: "Unknown error")

            Result.retry()
        }
    }

    /**
     * Create notification channel (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Xuất đơn hàng PDF",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị tiến trình xuất đơn hàng thành PDF"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground info with progress
     */
    private fun createForegroundInfo(progress: Int, message: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Xuất đơn hàng PDF")
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
    private fun showCompletionNotification(file: java.io.File, orderId: String) {
        // Create intent to open PDF file
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent.createChooser(intent, "Mở file PDF"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("✅ Xuất đơn hàng $orderId thành công!")
            .setContentText("Tap để xem file PDF")
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
            .setContentTitle("❌ Xuất đơn hàng thất bại")
            .setContentText("Lỗi: $error. WorkManager sẽ tự động thử lại.")
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }
}
