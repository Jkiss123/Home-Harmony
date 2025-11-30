package com.example.furniturecloudy.util

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.example.furniturecloudy.R

/**
 * Dialog for PIN code setup and verification
 */
class PinCodeDialog(
    context: Context,
    private val mode: Mode,
    private val pinCodeManager: PinCodeManager,
    private val onSuccess: () -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {

    enum class Mode {
        SETUP,      // First time setup - requires confirmation
        VERIFY      // Verify existing PIN
    }

    private var currentPin = ""
    private var confirmPin = ""
    private var isConfirmStep = false

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvError: TextView
    private lateinit var pinDots: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_pin_code)

        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        setCancelable(false)

        initViews()
        setupNumberPad()
        updateUI()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvPinTitle)
        tvSubtitle = findViewById(R.id.tvPinSubtitle)
        tvError = findViewById(R.id.tvPinError)

        pinDots = listOf(
            findViewById(R.id.pinDot1),
            findViewById(R.id.pinDot2),
            findViewById(R.id.pinDot3),
            findViewById(R.id.pinDot4)
        )

        findViewById<Button>(R.id.btnCancelPin).setOnClickListener {
            onCancel()
            dismiss()
        }

        findViewById<ImageButton>(R.id.btnBackspace).setOnClickListener {
            onBackspace()
        }
    }

    private fun setupNumberPad() {
        val numberButtons = listOf(
            R.id.btn0 to "0",
            R.id.btn1 to "1",
            R.id.btn2 to "2",
            R.id.btn3 to "3",
            R.id.btn4 to "4",
            R.id.btn5 to "5",
            R.id.btn6 to "6",
            R.id.btn7 to "7",
            R.id.btn8 to "8",
            R.id.btn9 to "9"
        )

        numberButtons.forEach { (id, number) ->
            findViewById<Button>(id).setOnClickListener {
                onNumberPressed(number)
            }
        }
    }

    private fun updateUI() {
        when (mode) {
            Mode.SETUP -> {
                if (isConfirmStep) {
                    tvTitle.text = "Xác nhận mã PIN"
                    tvSubtitle.text = "Nhập lại mã PIN để xác nhận"
                } else {
                    tvTitle.text = "Thiết lập mã PIN"
                    tvSubtitle.text = "Tạo mã PIN 4 chữ số để bảo vệ ứng dụng"
                }
            }
            Mode.VERIFY -> {
                tvTitle.text = "Nhập mã PIN"
                tvSubtitle.text = "Nhập mã PIN của bạn để tiếp tục"
            }
        }
    }

    private fun onNumberPressed(number: String) {
        if (currentPin.length >= PinCodeManager.PIN_LENGTH) return

        currentPin += number
        updatePinDots()
        hideError()

        if (currentPin.length == PinCodeManager.PIN_LENGTH) {
            handlePinComplete()
        }
    }

    private fun onBackspace() {
        if (currentPin.isNotEmpty()) {
            currentPin = currentPin.dropLast(1)
            updatePinDots()
            hideError()
        }
    }

    private fun updatePinDots() {
        pinDots.forEachIndexed { index, dot ->
            val drawableRes = if (index < currentPin.length) {
                R.drawable.pin_dot_filled
            } else {
                R.drawable.pin_dot_empty
            }
            dot.setBackgroundResource(drawableRes)
        }
    }

    private fun handlePinComplete() {
        when (mode) {
            Mode.SETUP -> handleSetupMode()
            Mode.VERIFY -> handleVerifyMode()
        }
    }

    private fun handleSetupMode() {
        if (!isConfirmStep) {
            // First entry - save and ask for confirmation
            confirmPin = currentPin
            currentPin = ""
            isConfirmStep = true
            updateUI()
            updatePinDots()
        } else {
            // Confirmation step
            if (currentPin == confirmPin) {
                // PINs match - save it
                if (pinCodeManager.setPin(currentPin)) {
                    Toast.makeText(context, "Đã thiết lập mã PIN thành công", Toast.LENGTH_SHORT).show()
                    onSuccess()
                    dismiss()
                } else {
                    showError("Không thể lưu mã PIN. Vui lòng thử lại.")
                    resetSetup()
                }
            } else {
                // PINs don't match
                showError("Mã PIN không khớp. Vui lòng thử lại.")
                resetSetup()
            }
        }
    }

    private fun handleVerifyMode() {
        when (val result = pinCodeManager.verifyPin(currentPin)) {
            is PinCodeManager.PinVerificationResult.Success -> {
                onSuccess()
                dismiss()
            }
            is PinCodeManager.PinVerificationResult.WrongPin -> {
                showError("Mã PIN không đúng. Còn ${result.remainingAttempts} lần thử.")
                currentPin = ""
                updatePinDots()
            }
            is PinCodeManager.PinVerificationResult.LockedOut -> {
                val seconds = result.remainingTimeMs / 1000
                showError("Đã vượt quá số lần thử. Vui lòng đợi ${seconds} giây.")
                currentPin = ""
                updatePinDots()
            }
            is PinCodeManager.PinVerificationResult.NoPinSet -> {
                showError("Chưa thiết lập mã PIN")
                onCancel()
                dismiss()
            }
            is PinCodeManager.PinVerificationResult.Error -> {
                showError("Lỗi: ${result.message}")
                currentPin = ""
                updatePinDots()
            }
        }
    }

    private fun resetSetup() {
        currentPin = ""
        confirmPin = ""
        isConfirmStep = false
        updateUI()
        updatePinDots()
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}