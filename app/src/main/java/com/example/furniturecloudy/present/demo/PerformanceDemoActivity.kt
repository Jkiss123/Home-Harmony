package com.example.furniturecloudy.present.demo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.furniturecloudy.R
import com.example.furniturecloudy.data.CartProducts
import com.example.furniturecloudy.data.Product
import com.example.furniturecloudy.databinding.ActivityPerformanceDemoBinding
import kotlin.random.Random

class PerformanceDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerformanceDemoBinding
    private var adapter: CartDemoAdapter? = null

    // Current configuration
    private var useBefore = true // true = BEFORE (List), false = AFTER (Map)
    private var currentCartSize = 0

    // Performance tracking
    private val operationTimes = mutableListOf<Double>()
    private var totalOperations = 0
    private var lastOperationTime = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerformanceDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        updateStatusText()
        showEmptyState()
    }

    private fun setupRecyclerView() {
        binding.rvCartDemo.layoutManager = LinearLayoutManager(this)
    }

    private fun setupButtons() {
        // Mode toggle buttons
        binding.btnModeBefore.setOnClickListener {
            switchMode(useBefore = true)
        }

        binding.btnModeAfter.setOnClickListener {
            switchMode(useBefore = false)
        }

        // Cart size buttons
        binding.btnSize100.setOnClickListener {
            loadCartProducts(100)
        }

        binding.btnSize1000.setOnClickListener {
            loadCartProducts(1000)
        }

        binding.btnSize5000.setOnClickListener {
            loadCartProducts(5000)
        }

        // Reset stats button
        binding.btnResetStats.setOnClickListener {
            resetStats()
        }

        // Initial selection
        updateModeButtonStyles()
        updateSizeButtonStyles(0)
    }

    private fun switchMode(useBefore: Boolean) {
        this.useBefore = useBefore
        updateModeButtonStyles()
        updateStatusText()
        resetStats()

        // Reload cart with new mode
        if (currentCartSize > 0) {
            loadCartProducts(currentCartSize)
        }
    }

    private fun loadCartProducts(size: Int) {
        currentCartSize = size
        updateSizeButtonStyles(size)
        updateStatusText()
        resetStats()

        // Show loading state
        binding.emptyState.visibility = View.GONE

        // Create fake products
        val products = createFakeCartProducts(size)

        // Create adapter with current mode
        adapter = CartDemoAdapter(useBefore) { timeMs ->
            onOperationCompleted(timeMs)
        }

        binding.rvCartDemo.adapter = adapter
        adapter?.loadProducts(products)
    }

    private fun onOperationCompleted(timeMs: Double) {
        // Track operation
        operationTimes.add(timeMs)
        totalOperations++
        lastOperationTime = timeMs

        // Update stats UI
        updateStatsUI()
    }

    private fun updateStatsUI() {
        val avgTime = if (operationTimes.isNotEmpty()) {
            operationTimes.average()
        } else {
            0.0
        }

        binding.tvAverageTime.text = "%.3fms".format(avgTime)
        binding.tvTotalOperations.text = totalOperations.toString()
        binding.tvLastOperation.text = "%.3fms".format(lastOperationTime)
    }

    private fun resetStats() {
        operationTimes.clear()
        totalOperations = 0
        lastOperationTime = 0.0
        updateStatsUI()
    }

    private fun updateStatusText() {
        val mode = if (useBefore) "BEFORE (List O(n))" else "AFTER (Map O(1))"
        val cartText = if (currentCartSize > 0) {
            "${currentCartSize.formatWithCommas()} products"
        } else {
            "No products loaded"
        }
        binding.tvCurrentStatus.text = "Mode: $mode | Cart: $cartText"
    }

    private fun updateModeButtonStyles() {
        // BEFORE button
        if (useBefore) {
            binding.btnModeBefore.setBackgroundColor(getColor(R.color.g_red))
            binding.btnModeBefore.strokeWidth = 4
        } else {
            binding.btnModeBefore.setBackgroundColor(getColor(R.color.g_gray700))
            binding.btnModeBefore.strokeWidth = 0
        }

        // AFTER button
        if (!useBefore) {
            binding.btnModeAfter.setBackgroundColor(getColor(R.color.g_green))
            binding.btnModeAfter.strokeWidth = 4
        } else {
            binding.btnModeAfter.setBackgroundColor(getColor(R.color.g_gray700))
            binding.btnModeAfter.strokeWidth = 0
        }
    }

    private fun updateSizeButtonStyles(selectedSize: Int) {
        val buttons = listOf(
            binding.btnSize100 to 100,
            binding.btnSize1000 to 1000,
            binding.btnSize5000 to 5000
        )

        buttons.forEach { (button, size) ->
            if (size == selectedSize) {
                button.setBackgroundColor(getColor(R.color.g_blue))
                button.setTextColor(getColor(R.color.g_white))
            } else {
                button.setBackgroundColor(getColor(R.color.g_white))
                button.setTextColor(getColor(R.color.g_blue))
            }
        }
    }

    private fun showEmptyState() {
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun createFakeCartProducts(count: Int): List<CartProducts> {
        return List(count) { index ->
            CartProducts(
                product = Product(
                    id = "product_$index",
                    name = "Test Product #$index",
                    price = (50 + index % 500).toFloat(),
                    category = "Test Category",
                    stock = 10,
                    averageRating = 4.5f,
                    offerPercentage = if (index % 3 == 0) 0.1f else null,
                    images = listOf("https://example.com/image_$index.jpg")
                ),
                quantity = Random.nextInt(1, 10)
            )
        }
    }

    private fun Int.formatWithCommas(): String {
        return String.format("%,d", this)
    }
}