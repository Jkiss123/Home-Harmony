package com.example.furniturecloudy.model.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.PaymentMethodItemBinding

data class PaymentMethodItem(
    val name: String,
    val displayName: String,
    val iconRes: Int
)

class PaymentMethodAdapter : RecyclerView.Adapter<PaymentMethodAdapter.PaymentMethodViewHolder>() {

    private val paymentMethods = listOf(
        PaymentMethodItem("COD", "Tiền mặt", R.drawable.ic_money),
        PaymentMethodItem("MoMo", "MoMo", R.drawable.ic_money),
        PaymentMethodItem("VNPay", "VNPay", R.drawable.ic_money),
        PaymentMethodItem("ZaloPay", "ZaloPay", R.drawable.ic_money)
    )

    private var selectedPosition = 0
    var onPaymentMethodSelected: ((String) -> Unit)? = null

    inner class PaymentMethodViewHolder(private val binding: PaymentMethodItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && position != selectedPosition) {
                    val previousPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)
                    onPaymentMethodSelected?.invoke(paymentMethods[position].name)
                }
            }

            binding.radioPaymentMethod.setOnClickListener {
                binding.root.performClick()
            }
        }

        fun bind(paymentMethod: PaymentMethodItem, isSelected: Boolean) {
            binding.apply {
                tvPaymentMethodName.text = paymentMethod.displayName
                imgPaymentIcon.setImageResource(paymentMethod.iconRes)
                radioPaymentMethod.isChecked = isSelected
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentMethodViewHolder {
        val binding = PaymentMethodItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PaymentMethodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentMethodViewHolder, position: Int) {
        holder.bind(paymentMethods[position], position == selectedPosition)
    }

    override fun getItemCount() = paymentMethods.size

    fun getSelectedPaymentMethod(): String = paymentMethods[selectedPosition].name
}
