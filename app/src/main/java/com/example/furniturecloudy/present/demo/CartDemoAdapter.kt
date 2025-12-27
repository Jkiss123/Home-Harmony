package com.example.furniturecloudy.present.demo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.furniturecloudy.data.CartProducts
import com.example.furniturecloudy.databinding.ItemCartDemoBinding

/**
 * Adapter for Performance Demo Cart
 *
 * Measures and displays real-time performance for each quantity change operation
 */
class CartDemoAdapter(
    private val useBefore: Boolean, // true = List O(n), false = Map O(1)
    private val onOperationCompleted: (timeMs: Double) -> Unit
) : RecyclerView.Adapter<CartDemoAdapter.CartDemoViewHolder>() {

    // Data storage
    private val products = mutableListOf<CartProducts>()

    // ❌ BEFORE: List-based storage (O(n) lookup)
    private val productIdListBefore = mutableListOf<String>()

    // ✅ AFTER: Map-based storage (O(1) lookup)
    private val productIdMapAfter = mutableMapOf<String, String>()

    inner class CartDemoViewHolder(val binding: ItemCartDemoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cartProduct: CartProducts, position: Int) {
            binding.apply {
                // Display product info
                tvProductName.text = cartProduct.product.name
                tvProductId.text = "ID: ${cartProduct.product.id}"
                tvProductPrice.text = "$${cartProduct.product.price.toInt()}"
                tvQuantity.text = cartProduct.quantity.toString()

                // Reset timer
                tvOperationTime.text = "--"

                // Increase button
                btnIncrease.setOnClickListener {
                    performOperation(isIncrease = true)
                }

                // Decrease button
                btnDecrease.setOnClickListener {
                    performOperation(isIncrease = false)
                }
            }
        }

        private fun performOperation(isIncrease: Boolean) {
            // Get fresh data from list by position to avoid stale reference
            val position = adapterPosition
            if (position == RecyclerView.NO_POSITION) return

            val currentProduct = products.getOrNull(position) ?: return

            // Check minimum quantity for decrease
            if (!isIncrease && currentProduct.quantity <= 1) return

            val startTime = System.nanoTime()

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // PERFORMANCE TEST: BEFORE (List) vs AFTER (Map)
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            // Performance test: lookup product ID
            if (useBefore) {
                // ❌ BEFORE: O(n) indexOf() lookup
                val index = products.indexOfFirst { it.product.id == currentProduct.product.id }
                if (index != -1) productIdListBefore.getOrNull(index)
            } else {
                // ✅ AFTER: O(1) Map lookup
                productIdMapAfter[currentProduct.product.id]
            }

            // Calculate new quantity from CURRENT data
            val newQuantity = if (isIncrease) {
                currentProduct.quantity + 1
            } else {
                (currentProduct.quantity - 1).coerceAtLeast(1)
            }

            // Measure time (BEFORE updating data to measure pure lookup performance)
            val endTime = System.nanoTime()
            val timeMs = (endTime - startTime) / 1_000_000.0

            // ✅ Update actual data in list (create copy with new quantity)
            products[position] = currentProduct.copy(quantity = newQuantity)

            // Update UI
            binding.tvQuantity.text = newQuantity.toString()
            binding.tvOperationTime.text = "%.3fms".format(timeMs)

            // Callback to update stats
            onOperationCompleted(timeMs)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartDemoViewHolder {
        val binding = ItemCartDemoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartDemoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartDemoViewHolder, position: Int) {
        holder.bind(products[position], position)
    }

    override fun getItemCount(): Int = products.size

    /**
     * Load products and build data structures based on mode
     */
    fun loadProducts(newProducts: List<CartProducts>) {
        products.clear()
        products.addAll(newProducts)

        if (useBefore) {
            // ❌ BEFORE: Store in List
            productIdListBefore.clear()
            newProducts.forEach { cartProduct ->
                productIdListBefore.add(cartProduct.product.id)
            }
        } else {
            // ✅ AFTER: Build Map for O(1) lookup
            productIdMapAfter.clear()
            newProducts.forEach { cartProduct ->
                productIdMapAfter[cartProduct.product.id] = cartProduct.product.id
            }
        }

        notifyDataSetChanged()
    }

    fun clear() {
        products.clear()
        productIdListBefore.clear()
        productIdMapAfter.clear()
        notifyDataSetChanged()
    }
}