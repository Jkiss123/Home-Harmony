package com.example.furniturecloudy

import com.example.furniturecloudy.data.CartProducts
import com.example.furniturecloudy.data.Product
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit Test Benchmark - Map vs List Performance
 *
 * 🎯 MỤC TIÊU:
 * So sánh performance giữa List.indexOf() và Map[key] lookup
 *
 * 📊 EXPECTED RESULTS:
 * - List indexOf: ~18-20ms (O(n))
 * - Map lookup: ~0.5-1ms (O(1))
 * - Improvement: ~20-23x faster
 *
 * ✅ CÁCH CHẠY:
 * 1. Right-click file này → Run 'CartBenchmarkTest'
 * 2. Xem kết quả trong Console
 * 3. Copy kết quả vào docs/optimization/B4_RESULTS.md
 */
class CartBenchmarkTest {

    @Test
    fun `benchmark Map vs List with 100 products`() {
        println("\n" + "=".repeat(60))
        println("BENCHMARK: Map vs List - 100 Products")
        println("=".repeat(60))

        val products = createFakeCartProducts(100)
        runBenchmark(products, iterations = 1000)
    }

    @Test
    fun `benchmark Map vs List with 1000 products`() {
        println("\n" + "=".repeat(60))
        println("BENCHMARK: Map vs List - 1000 Products")
        println("=".repeat(60))

        val products = createFakeCartProducts(1000)
        runBenchmark(products, iterations = 100)
    }

    @Test
    fun `benchmark Map vs List with 5000 products`() {
        println("\n" + "=".repeat(60))
        println("BENCHMARK: Map vs List - 5000 Products (Stress Test)")
        println("=".repeat(60))

        val products = createFakeCartProducts(5000)
        runBenchmark(products, iterations = 20)
    }

    /**
     * Run benchmark comparison
     */
    private fun runBenchmark(products: List<CartProducts>, iterations: Int) {
        val productCount = products.size
        val targetId = "product_${productCount - 1}" // Worst case: tìm product cuối cùng

        println("Cart size:        $productCount products")
        println("Test iterations:  $iterations")
        println("Target product:   $targetId (worst case - last item)")
        println("")

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // BEFORE: List with indexOf() - O(n)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        val startList = System.nanoTime()

        repeat(iterations) {
            // Simulate current CartViewModel logic
            val index = products.indexOfFirst { it.product.id == targetId }
            products.getOrNull(index)
        }

        val listTimeMs = (System.nanoTime() - startList) / 1_000_000.0

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // AFTER: Map with direct lookup - O(1)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        // Build map (one-time cost)
        val productsMap = products.associateBy { it.product.id }

        val startMap = System.nanoTime()

        repeat(iterations) {
            // Optimized lookup
            productsMap[targetId]
        }

        val mapTimeMs = (System.nanoTime() - startMap) / 1_000_000.0

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // RESULTS
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        val improvement = listTimeMs / mapTimeMs

        println("RESULTS:")
        println("--------")
        println("BEFORE (List.indexOf):  ${formatTime(listTimeMs)}")
        println("AFTER (Map[key]):       ${formatTime(mapTimeMs)}")
        println("")
        println("Improvement:            ${improvement.format(2)}x faster")
        println("Time saved:             ${formatTime(listTimeMs - mapTimeMs)}")
        println("")

        // Visual bar chart
        printBarChart(listTimeMs, mapTimeMs)

        println("=".repeat(60))
        println("")

        // Assertions
        assertTrue(
            "Map should be faster than List",
            mapTimeMs < listTimeMs
        )

        assertTrue(
            "Improvement should be at least 5x for $productCount products",
            improvement >= 5.0
        )
    }

    /**
     * Create fake cart products for testing
     */
    private fun createFakeCartProducts(count: Int): List<CartProducts> {
        return List(count) { index ->
            CartProducts(
                product = Product(
                    id = "product_$index",
                    name = "Test Product $index",
                    price = (50 + index % 500).toFloat(),
                    category = "Test Category",
                    stock = 10,
                    averageRating = 4.5f,
                    offerPercentage = if (index % 3 == 0) 0.1f else null,
                    images = listOf("https://example.com/image_$index.jpg")
                ),
                quantity = 1
            )
        }
    }

    /**
     * Format time with appropriate unit
     */
    private fun formatTime(ms: Double): String {
        return when {
            ms < 1.0 -> "%.3fms".format(ms)
            ms < 10.0 -> "%.2fms".format(ms)
            else -> "%.1fms".format(ms)
        }
    }

    /**
     * Print visual bar chart
     */
    private fun printBarChart(listTime: Double, mapTime: Double) {
        val maxTime = maxOf(listTime, mapTime)
        val barWidth = 40

        println("Visual Comparison:")
        println("------------------")

        // List bar
        val listBars = (listTime / maxTime * barWidth).toInt()
        print("List:  ")
        print("█".repeat(listBars))
        println(" ${formatTime(listTime)}")

        // Map bar
        val mapBars = (mapTime / maxTime * barWidth).toInt().coerceAtLeast(1)
        print("Map:   ")
        print("█".repeat(mapBars))
        println(" ${formatTime(mapTime)}")
        println("")
    }

    /**
     * Extension: Format Double
     */
    private fun Double.format(decimals: Int): String {
        return "%.${decimals}f".format(this)
    }
}