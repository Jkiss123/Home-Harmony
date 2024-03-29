package com.example.furniturecloudy.present.fragments.loginRegister

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.furniturecloudy.R
import com.example.furniturecloudy.databinding.FragmentLoginBinding
import com.example.furniturecloudy.model.viewmodel.LoginViewmodel
import com.example.furniturecloudy.present.ShoppingActivity
import com.example.furniturecloudy.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
@AndroidEntryPoint
class LoginFragment : Fragment() {
    private lateinit var binding:FragmentLoginBinding
    private val viewmodel:LoginViewmodel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            btnLoginDangnhap.setOnClickListener {
                    val email = edtLoginTaiKhoan.text.toString().trim()
                    val password =edtLoginMatKhau.text.toString()
                viewmodel.loginAccount(email,password)
            }
            txtvSlogan2Login.setOnClickListener {
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewmodel.login.collect{
                    when(it){
                        is Resource.Error -> {
                            binding.btnLoginDangnhap.revertAnimation()
                            Toast.makeText(requireContext(),"Đăng Nhập không thành công",Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {
                            binding.btnLoginDangnhap.startAnimation()
                        }
                        is Resource.Success -> {
                            binding.btnLoginDangnhap.revertAnimation()
                            Intent(requireActivity(),ShoppingActivity::class.java).also { intent ->
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                            }
                        }
                        else -> Unit
                    }
                }
            }
            }
    }


}