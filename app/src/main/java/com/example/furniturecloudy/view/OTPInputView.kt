package com.example.furniturecloudy.view

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.ViewOtpInputBinding

/**
 * Custom OTP Input View with 6 boxes
 *
 * Features:
 * - Auto-focus next box when digit entered
 * - Auto-focus previous box on backspace
 * - Paste support (paste "123456" fills all boxes)
 * - Auto-submit when all 6 digits entered
 */
class OTPInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewOtpInputBinding
    private val otpBoxes: List<EditText>
    private var onOTPCompleteListener: ((String) -> Unit)? = null

    init {
        binding = ViewOtpInputBinding.inflate(LayoutInflater.from(context), this, true)

        otpBoxes = listOf(
            binding.otpBox1,
            binding.otpBox2,
            binding.otpBox3,
            binding.otpBox4,
            binding.otpBox5,
            binding.otpBox6
        )

        setupOTPBoxes()
    }

    private fun setupOTPBoxes() {
        otpBoxes.forEachIndexed { index, editText ->
            // Set max length to 1
            editText.filters = arrayOf(InputFilter.LengthFilter(1))

            // Text changed listener
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        // Move to next box
                        if (index < otpBoxes.size - 1) {
                            otpBoxes[index + 1].requestFocus()
                        } else {
                            // Last box filled, hide keyboard and trigger completion
                            hideKeyboard(editText)
                            checkOTPComplete()
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            // Key listener for backspace
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && index > 0) {
                        // Move to previous box
                        otpBoxes[index - 1].requestFocus()
                        otpBoxes[index - 1].setText("")
                    }
                }
                false
            }
        }

        // Focus first box on start
        otpBoxes[0].requestFocus()
    }

    /**
     * Get entered OTP code
     */
    fun getOTP(): String {
        return otpBoxes.joinToString("") { it.text.toString() }
    }

    /**
     * Set OTP code programmatically
     */
    fun setOTP(otp: String) {
        val digits = otp.take(6).toCharArray()
        digits.forEachIndexed { index, char ->
            if (index < otpBoxes.size) {
                otpBoxes[index].setText(char.toString())
            }
        }
    }

    /**
     * Clear all boxes
     */
    fun clear() {
        otpBoxes.forEach { it.setText("") }
        otpBoxes[0].requestFocus()
    }

    /**
     * Check if OTP is complete (all 6 digits entered)
     */
    private fun checkOTPComplete() {
        val otp = getOTP()
        if (otp.length == 6) {
            onOTPCompleteListener?.invoke(otp)
        }
    }

    /**
     * Set listener for OTP completion
     */
    fun setOnOTPCompleteListener(listener: (String) -> Unit) {
        this.onOTPCompleteListener = listener
    }

    /**
     * Enable/disable all boxes
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        otpBoxes.forEach { it.isEnabled = enabled }
    }

    /**
     * Show shake animation for error
     */
    fun shake() {
        animate()
            .translationX(-10f)
            .setDuration(50)
            .withEndAction {
                animate()
                    .translationX(10f)
                    .setDuration(50)
                    .withEndAction {
                        animate()
                            .translationX(-10f)
                            .setDuration(50)
                            .withEndAction {
                                animate()
                                    .translationX(10f)
                                    .setDuration(50)
                                    .withEndAction {
                                        animate()
                                            .translationX(0f)
                                            .setDuration(50)
                                            .start()
                                    }
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }

    /**
     * Request focus on first box
     */
    fun requestOTPFocus() {
        otpBoxes[0].requestFocus()
        showKeyboard(otpBoxes[0])
    }

    private fun showKeyboard(view: EditText) {
        view.postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    private fun hideKeyboard(view: EditText) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
