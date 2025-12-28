package com.example.furniturecloudy.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.example.furniturecloudy.data.Order
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * E2 OPTION C: PDF Generator for Single Order Export
 *
 * Generates a professional PDF invoice for a single order with:
 * - Order information (ID, date, status, payment)
 * - Shipping address
 * - Product list with prices
 * - Total amount
 */
object OrderPdfGenerator {

    private const val TAG = "OrderPdfGenerator"

    // PDF page dimensions (A4 size in points: 595 x 842)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    // Margins
    private const val MARGIN_LEFT = 50f
    private const val MARGIN_RIGHT = 545f
    private const val MARGIN_TOP = 50f

    // Font sizes
    private const val FONT_SIZE_TITLE = 24f
    private const val FONT_SIZE_HEADER = 18f
    private const val FONT_SIZE_NORMAL = 12f
    private const val FONT_SIZE_SMALL = 10f

    /**
     * Generate PDF invoice for an order
     *
     * @param context Android context
     * @param order Order to export
     * @return PDF file
     */
    fun generateOrderPdf(context: Context, order: Order): File {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Generating PDF for order: ${order.orderId}")

        // Create PDF document
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Current Y position for drawing
        var yPosition = MARGIN_TOP

        // Title
        yPosition = drawTitle(canvas, order.orderId, yPosition)

        // Horizontal line
        yPosition = drawLine(canvas, yPosition + 10f)

        // Order Info Section
        yPosition = drawOrderInfo(canvas, order, yPosition + 20f)

        // Horizontal line
        yPosition = drawLine(canvas, yPosition + 10f)

        // Shipping Address Section
        yPosition = drawShippingAddress(canvas, order, yPosition + 20f)

        // Horizontal line
        yPosition = drawLine(canvas, yPosition + 10f)

        // Products Section
        yPosition = drawProducts(canvas, order, yPosition + 20f)

        // Total Section
        yPosition = drawTotal(canvas, order, yPosition + 20f)

        // Finish page
        pdfDocument.finishPage(page)

        // Save to file
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "order_${order.orderId}_$timestamp.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }

        pdfDocument.close()

        val totalTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "PDF generated successfully in ${totalTime}ms: ${file.absolutePath}")

        return file
    }

    /**
     * Draw title section
     */
    private fun drawTitle(canvas: Canvas, orderId: String, yPosition: Float): Float {
        val paint = Paint().apply {
            textSize = FONT_SIZE_TITLE
            color = Color.BLACK
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("ĐơN HÀNG", PAGE_WIDTH / 2f, yPosition, paint)
        canvas.drawText(orderId, PAGE_WIDTH / 2f, yPosition + 30f, paint)

        return yPosition + 60f
    }

    /**
     * Draw horizontal line
     */
    private fun drawLine(canvas: Canvas, yPosition: Float): Float {
        val paint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }

        canvas.drawLine(MARGIN_LEFT, yPosition, MARGIN_RIGHT, yPosition, paint)

        return yPosition
    }

    /**
     * Draw order information section
     */
    private fun drawOrderInfo(canvas: Canvas, order: Order, yPosition: Float): Float {
        val headerPaint = Paint().apply {
            textSize = FONT_SIZE_HEADER
            color = Color.BLACK
            isFakeBoldText = true
        }

        val textPaint = Paint().apply {
            textSize = FONT_SIZE_NORMAL
            color = Color.DKGRAY
        }

        var currentY = yPosition

        // Section header
        canvas.drawText("Thông tin đơn hàng", MARGIN_LEFT, currentY, headerPaint)
        currentY += 25f

        // Order date
        canvas.drawText("Ngày đặt: ${order.date}", MARGIN_LEFT, currentY, textPaint)
        currentY += 20f

        // Order status
        val statusColor = when (order.orderStatus.lowercase()) {
            "delivered" -> Color.rgb(0, 150, 0)
            "shipping" -> Color.rgb(255, 140, 0)
            "canceled" -> Color.RED
            else -> Color.rgb(128, 128, 128)
        }

        textPaint.color = statusColor
        canvas.drawText("Trạng thái: ${order.orderStatus}", MARGIN_LEFT, currentY, textPaint)
        textPaint.color = Color.DKGRAY
        currentY += 20f

        // Payment method
        canvas.drawText("Phương thức thanh toán: ${order.paymentMethod}", MARGIN_LEFT, currentY, textPaint)
        currentY += 20f

        // Payment status
        val paymentStatusColor = if (order.paymentStatus == "PAID") Color.rgb(0, 150, 0) else Color.rgb(255, 140, 0)
        textPaint.color = paymentStatusColor
        canvas.drawText("Trạng thái thanh toán: ${order.paymentStatus}", MARGIN_LEFT, currentY, textPaint)
        textPaint.color = Color.DKGRAY
        currentY += 20f

        // Transaction ID if available
        order.paymentTransactionId?.let { txId ->
            canvas.drawText("Mã giao dịch: $txId", MARGIN_LEFT, currentY, textPaint)
            currentY += 20f
        }

        return currentY + 10f
    }

    /**
     * Draw shipping address section
     */
    private fun drawShippingAddress(canvas: Canvas, order: Order, yPosition: Float): Float {
        val headerPaint = Paint().apply {
            textSize = FONT_SIZE_HEADER
            color = Color.BLACK
            isFakeBoldText = true
        }

        val textPaint = Paint().apply {
            textSize = FONT_SIZE_NORMAL
            color = Color.DKGRAY
        }

        var currentY = yPosition

        // Section header
        canvas.drawText("Địa chỉ giao hàng", MARGIN_LEFT, currentY, headerPaint)
        currentY += 25f

        // Full name
        canvas.drawText(order.address.fullName, MARGIN_LEFT, currentY, textPaint)
        currentY += 20f

        // Address
        val address = order.address.addressFull.ifEmpty {
            "${order.address.wards}, ${order.address.district}, ${order.address.city}"
        }

        // Word wrap for long addresses
        val maxWidth = MARGIN_RIGHT - MARGIN_LEFT
        val addressLines = wrapText(address, textPaint, maxWidth)
        addressLines.forEach { line ->
            canvas.drawText(line, MARGIN_LEFT, currentY, textPaint)
            currentY += 20f
        }

        // Phone number
        canvas.drawText("SĐT: ${order.address.phone}", MARGIN_LEFT, currentY, textPaint)
        currentY += 20f

        return currentY + 10f
    }

    /**
     * Draw products section
     */
    private fun drawProducts(canvas: Canvas, order: Order, yPosition: Float): Float {
        val headerPaint = Paint().apply {
            textSize = FONT_SIZE_HEADER
            color = Color.BLACK
            isFakeBoldText = true
        }

        val textPaint = Paint().apply {
            textSize = FONT_SIZE_NORMAL
            color = Color.DKGRAY
        }

        val smallPaint = Paint().apply {
            textSize = FONT_SIZE_SMALL
            color = Color.GRAY
        }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        var currentY = yPosition

        // Section header
        canvas.drawText("Sản phẩm", MARGIN_LEFT, currentY, headerPaint)
        currentY += 25f

        // Product list
        order.products.forEachIndexed { index, cartProduct ->
            // Product name and quantity
            val productInfo = "${index + 1}. ${cartProduct.product.name} (x${cartProduct.quantity})"
            canvas.drawText(productInfo, MARGIN_LEFT, currentY, textPaint)
            currentY += 18f

            // Product price and subtotal
            val priceInfo = "   ${currencyFormat.format(cartProduct.product.price)} x ${cartProduct.quantity}"
            canvas.drawText(priceInfo, MARGIN_LEFT + 20f, currentY, smallPaint)

            val subtotal = cartProduct.product.price * cartProduct.quantity
            smallPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(currencyFormat.format(subtotal), MARGIN_RIGHT, currentY, smallPaint)
            smallPaint.textAlign = Paint.Align.LEFT
            currentY += 25f
        }

        return currentY + 10f
    }

    /**
     * Draw total section
     */
    private fun drawTotal(canvas: Canvas, order: Order, yPosition: Float): Float {
        val paint = Paint().apply {
            strokeWidth = 2f
            color = Color.LTGRAY
        }

        // Top line
        canvas.drawLine(MARGIN_LEFT, yPosition, MARGIN_RIGHT, yPosition, paint)

        val totalPaint = Paint().apply {
            textSize = FONT_SIZE_HEADER
            color = Color.BLACK
            isFakeBoldText = true
        }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        val formattedTotal = currencyFormat.format(order.totalPrice)

        val currentY = yPosition + 30f

        // Total label
        canvas.drawText("TỔNG CỘNG:", MARGIN_LEFT, currentY, totalPaint)

        // Total amount (right aligned)
        totalPaint.textAlign = Paint.Align.RIGHT
        totalPaint.color = Color.rgb(255, 87, 34) // Orange color
        canvas.drawText(formattedTotal, MARGIN_RIGHT, currentY, totalPaint)

        // Bottom line
        canvas.drawLine(MARGIN_LEFT, currentY + 10f, MARGIN_RIGHT, currentY + 10f, paint)

        return currentY + 40f
    }

    /**
     * Wrap text to fit within max width
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(testLine)

            if (textWidth > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}
