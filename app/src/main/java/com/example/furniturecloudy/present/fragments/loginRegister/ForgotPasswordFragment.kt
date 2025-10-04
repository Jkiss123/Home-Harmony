package com.example.furniturecloudy.present.fragments.loginRegister

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.furniturecloudy.databinding.FragmentForgotPasswordBinding
import com.example.furniturecloudy.model.viewmodel.ForgotPasswordViewmodel
import com.example.furniturecloudy.util.RegisterValidation
import com.example.furniturecloudy.util.Resource
import com.example.furniturecloudy.util.validateEmail
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {
    private lateinit var binding: FragmentForgotPasswordBinding
    private val viewmodel: ForgotPasswordViewmodel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForgotPasswordBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imageCloseResetPassword.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSendResetPassword.setOnClickListener {
            val email = binding.edtResetPasswordEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val emailValidation = validateEmail(email)
            if (emailValidation is RegisterValidation.Failed) {
                Toast.makeText(requireContext(), emailValidation.message, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewmodel.sendPasswordResetEmail(email)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.resetPassword.collectLatest {
                    when (it) {
                        is Resource.Error -> {
                            binding.btnSendResetPassword.revertAnimation()
                            Toast.makeText(
                                requireContext(),
                                "Gửi email thất bại. Vui lòng kiểm tra địa chỉ email",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        is Resource.Loading -> {
                            binding.btnSendResetPassword.startAnimation()
                        }
                        is Resource.Success -> {
                            binding.btnSendResetPassword.revertAnimation()
                            Toast.makeText(
                                requireContext(),
                                "Email đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư.",
                                Toast.LENGTH_LONG
                            ).show()
                            findNavController().navigateUp()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
